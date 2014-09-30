package net.venaglia.realms.common.map.data.rdbms;

import static net.venaglia.realms.common.map.data.BufferedUpdates.Delta;
import static net.venaglia.realms.common.map.data.BufferedUpdates.UpdatedFields;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.PooledDataSource;
import net.venaglia.common.util.Pair;
import net.venaglia.common.util.Ref;
import net.venaglia.common.util.Tuple2;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.data.BufferedUpdates;
import net.venaglia.realms.common.map.data.CubeImpl;
import net.venaglia.realms.common.map.data.AbstractDataStore;
import net.venaglia.realms.common.map.data.ReusableByteStream;
import net.venaglia.realms.common.map.data.Sequence;
import net.venaglia.realms.common.map.data.ThingRefImpl;
import net.venaglia.realms.common.map.things.ThingFactory;
import net.venaglia.realms.common.map.things.ThingMetadata;
import net.venaglia.realms.common.map.things.ThingProperties;
import net.venaglia.realms.common.map.things.AbstractThing;
import net.venaglia.common.util.ThreadSingletonSource;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.data.binaries.BinaryType;
import net.venaglia.realms.common.map.data.binaries.BinaryTypeRegistry;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ed
 * Date: 3/20/14
 * Time: 5:24 PM
 */
public class JDBCDataStore extends AbstractDataStore {

    private static final boolean RESET_DATABASE = "true".equals(System.getProperty("data.reset", "false"));
    private static final Integer INCREMENT = 1;
    private static final Integer DECREMENT = -1;

    private PooledDataSource cpds;
    private UUID instanceUuid;
    private boolean readonly = true;

    public void init() {
        readonly = Configuration.DATABASE_HARMLESS.getBoolean(false);
        int maxPoolSize = Configuration.JDBC_POOL_SIZE.getInteger();
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be positive: " + maxPoolSize);
        }
        cpds = buildDataSource(maxPoolSize);

        if (readonly) {
            System.out.println("Database is READ ONLY");
            loadUUID();
        } else {
            System.out.println("Database is READ/WRITE");
            if (RESET_DATABASE) {
                wipeDatabase();
            }
            loadUUID();
            runUpgrades();
        }
    }

    private PooledDataSource buildDataSource(int maxPoolSize) {
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            Class.forName(Configuration.JDBC_DRIVER.getString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        cpds.setJdbcUrl(Configuration.JDBC_URL.getString());
        cpds.setUser(Configuration.JDBC_USERNAME.getString());
        cpds.setPassword(Configuration.JDBC_PASSWORD.getString());
        cpds.setMinPoolSize(reduce(maxPoolSize, 3, 2));
        cpds.setAcquireIncrement(reduce(maxPoolSize, 3, 1));
        cpds.setMaxPoolSize(maxPoolSize);
        return cpds;
    }

    private int reduce(int base, int magnitude, int lowerLimit) {
        return Math.max(base >> magnitude, Math.min(base, lowerLimit));
    }

    private void loadUUID() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = cpds.getConnection();
            ps = con.prepareStatement("SELECT instance_id FROM thing_version");
            rs = ps.executeQuery();
            if (rs.next()) {
                instanceUuid = UUID.fromString(rs.getString("instance_id"));
            }
        } catch (SQLException e) {
            if (!e.getMessage().contains("does not exist")) { // might be a new DB
                e.printStackTrace();
            }
        } finally {
            if (instanceUuid == null) {
                instanceUuid = UUID.randomUUID();
            }
            close(ps, rs);
            close(con);
        }
    }

    private void wipeDatabase() {
        Connection con = null;
        Statement s = null;
        for (String table : "thing_binary_locator thing_binary thing thing_seq thing_props thing_version".split(" ")) {
            try {
                con = cpds.getConnection();
                s = con.createStatement();
                s.execute("DROP TABLE " + table);
            } catch (SQLException e) {
                //noinspection StatementWithEmptyBody
                if (e.getMessage().contains("does not exist")) {
                    // don't care
                } else {
                    throw new RuntimeException(e.getMessage(), e);
                }
            } finally {
                close(s, null);
                close(con);
            }
        }
    }

    private void runUpgrades() {
        Connection con = null;
        Statement s = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<String> upgradeSQL = loadUpgradeSQL();
        try {
            con = cpds.getConnection();
            int version = 0;
            try {
                ps = con.prepareStatement("SELECT version FROM thing_version");
                rs = ps.executeQuery();
                if (rs.next()) {
                    version = rs.getInt("version");
                } else {
                    version = 1; // version table exists, but has no data
                }
            } catch (SQLException e) {
                // no version, new DB
                System.out.println("Initializing new database... [uuid=" + instanceUuid + "]");
            } finally {
                close(ps, rs);
                ps = null;
                rs = null;
            }
            if (version < upgradeSQL.size()) {
                s = con.createStatement();
                for (int i = version, l = upgradeSQL.size(); i < l; i++) {
                    String sql = upgradeSQL.get(i);
                    int toVersion = i + 1;
                    if (sql != null) {
                        System.out.printf("Running upgrade migration: %d -> %d\n\t%s;\n", i, toVersion, sql.replace("\n", "\n\t"));
                        if (sql.contains("INSERT INTO thing_props DEFAULT VALUES")) {
                            System.out.println("--------------------------------------------------");
                            bootstrapPersistentProperties();
                            System.out.println("--------------------------------------------------");
                        } else {
                            s.execute(sql);
                        }
                    }
                    try {
                        ps = con.prepareStatement("UPDATE thing_version SET version = ?");
                        ps.setInt(1, toVersion);
                        ps.executeUpdate();
                    } finally {
                        close(ps, null);
                        ps = null;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(s, null);
            close(ps, rs);
            close(con);
        }
    }

    private List<String> loadUpgradeSQL() {
        InputStream stream = getClass().getResourceAsStream("upgrade.sql");
        Reader sqlIn = new InputStreamReader(stream, Charset.forName("UTF-8"));
        StringBuilder buffer = new StringBuilder(1024);
        try {
            char[] c = new char[256];
            for (int n = sqlIn.read(c); n > 0; n = sqlIn.read(c)) {
                buffer.append(c, 0, n);
            }
            sqlIn.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            close(stream);
        }

        regexpReplace(buffer, Pattern.compile("\\{instanceUUID}"), instanceUuid.toString());
        regexpReplace(buffer, Pattern.compile("^--.*$|\\s+$", Pattern.MULTILINE), "");
        regexpReplace(buffer, Pattern.compile("\n\n+"), "\n");

        List<String> sql = new ArrayList<String>(16);
        for (int i = 0, j = buffer.indexOf(";", i); j > i; i = j + 1, j = buffer.indexOf(";", i)) {
            sql.add(buffer.substring(i, j).trim());
        }
        return sql;
    }

    private void regexpReplace(StringBuilder buffer, Pattern pattern, String replacement) {
        StringBuffer temp = new StringBuffer(buffer.length());
        Matcher matcher = pattern.matcher(buffer);
        while (matcher.find()) {
            matcher.appendReplacement(temp, replacement);
        }
        matcher.appendTail(temp);
        buffer.setLength(0);
        buffer.append(temp);
    }

    public UUID getInstanceUuid() {
        return instanceUuid;
    }

    public boolean isReadonly() {
        return readonly;
    }

    protected long runSingleValueQuery(String sql, Long valueIfNoRows, Object... params) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = cpds.getConnection();
            ps = con.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            } else if (valueIfNoRows == null) {
                throw new NoSuchElementException();
            } else {
                return valueIfNoRows;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close(ps, rs);
            close(con);
        }
    }

    protected void runSelectQuery(RowVisitor rowVisitor, String sql, Object... params) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = cpds.getConnection();
            ps = con.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                rowVisitor.visit(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close(ps, rs);
            close(con);
        }
    }

    protected <T> List<T> runSelectQuery(RowMapper<T> rowMapper, String sql, Object... params) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = cpds.getConnection();
            ps = con.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            rs = ps.executeQuery();
            List<T> result = new ArrayList<T>();
            while (rs.next()) {
                result.add(rowMapper.map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close(ps, rs);
            close(con);
        }
    }

    protected long runUpdateQuery(String sql, Object... params) {
        if (readonly) {
            throw new IllegalStateException("Denying write access to readonly database");
        }
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = cpds.getConnection();
            ps = con.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof ReusableByteStream) {
                    ReusableByteStream stream = (ReusableByteStream)param;
                    ps.setBinaryStream(i + 1, stream, stream.size());
                } else {
                    ps.setObject(i + 1, param);
                }
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close(ps, null);
            close(con);
        }
    }

    @Override
    protected Tuple2<Long,Long> getNextAvailableIdRange(Sequence sequence) {
        long next;
        do {
            next = runSingleValueQuery("SELECT next FROM thing_seq WHERE name = ?", Long.MIN_VALUE, sequence.name());
            if (next == Long.MIN_VALUE) {
                runUpdateQuery("INSERT INTO thing_seq (name) VALUES (?)", sequence.name());
            }
        } while (runUpdateQuery("UPDATE thing_seq SET next = ? WHERE next = ?", next + 64, next) == 0);
        return new Pair<Long,Long>(next, next + 64);
    }

    @Override
    protected long lookupNextAvailableInRange(Sequence seq, long from, long to) {
        String sql;
        switch (seq) {
            case THING:
                sql = "SELECT max(thing_id) FROM thing WHERE thing_id >= ? AND thing_id < ?";
                break;
            case BINARY:
                sql = "SELECT max(thing_binary_id) FROM thing_binary WHERE thing_binary_id >= ? AND thing_binary_id < ?";
                break;
            default:
                throw new RuntimeException("Encountered a Sequence that is not supported: " + seq);
        }
        return runSingleValueQuery(sql, from, from, to) + 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void populateCube(Long id, final CubeImpl cube) {
        RowMapper<ThingRefImpl<AbstractThing>> rowMapper = thingRefRowMapper.get().init(cube);
        List<ThingRefImpl<AbstractThing>> list = runSelectQuery(rowMapper, "SELECT * FROM thing WHERE cube_id = ?", cube.getId());
        thingCache.seed(list);
    }

    @Override
    protected <T extends AbstractThing> void populateRef(final Long id, final ThingRefImpl<T> ref) {
        runSelectQuery(singleThingRefRowVisitor.get().init(ref, id), "SELECT * FROM thing WHERE thing_id = ?", id);
    }

    @Override
    protected void write(BufferedUpdates bufferedUpdates) {
        if (readonly) {
            throw new IllegalStateException("Denying write access to readonly database");
        }
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = cpds.getConnection();
            for (UpdatedFields updatedFields : UpdatedFields.values()) {
                ps = con.prepareStatement(getSql(updatedFields));
                for (Delta delta : bufferedUpdates.subset(updatedFields)) {
                    setQueryParams(updatedFields, delta, ps);
                    ps.addBatch();
                    ps.clearParameters();
                }
                ps.executeBatch();
                close(ps, null);
                ps = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(ps, null);
            close(con);
        }
    }

    @Override
    protected void populateBinaryResource(Long id, BinaryResource binaryResource) {
        BinaryResourceRowVisitor rowVisitor = binaryResourceRowVisitor.get().init(binaryResource);
        runSelectQuery(rowVisitor, "SELECT * FROM thing_binary WHERE thing_binary_id = ?", id);
    }

    @Override
    protected Long findBinaryResourceId(String mimetype, long locatorId) {
        long id = runSingleValueQuery("SELECT thing_binary_id FROM thing_binary_locator WHERE locator_id = ? AND mimetype = ?",
                                      Long.MIN_VALUE, locatorId, mimetype);
        return id == Long.MIN_VALUE ? null : id;
    }

    @Override
    protected BinaryResource insertBinaryResource(BinaryResource resource, long locatorId) {
        BinaryType type = resource.getType();
        String sha1Hash = resource.getSha1Hash();
        long existingId = runSingleValueQuery("SELECT thing_binary_id FROM thing_binary WHERE mimetype = ? AND sha1 = ? AND length = ?",
                                              Long.MIN_VALUE, type.mimeType(), sha1Hash, resource.getLength());
        if (existingId != Long.MIN_VALUE) {
            bumpBinaryResourceReferenceCount(existingId, INCREMENT);
            return commonDataSources.getBinaryCache().get(existingId);
        }
        Long id = commonDataSources.nextId(Sequence.BINARY);
        byte[] data = resource.getData();
        ReusableByteStream dataStream = ReusableByteStream.get().load(data);
        try {
            Map<String,Object> metadata = resource.getMetadata();
            runUpdateQuery("INSERT INTO thing_binary (thing_binary_id, reference_count, mimetype, metadata, sha1, length, data) VALUES (?, 1, ?, ?, ?, ?, ?)",
                           id,
                           type.mimeType(),
                           type.encodeMetadata(metadata),
                           sha1Hash,
                           resource.getLength(),
                           dataStream);
            runUpdateQuery("INSERT INTO thing_binary_locator (locator_id, mimetype, thing_binary_id) VALUES (?, ?, ?)", locatorId, type.mimeType(), id);
            resource.recycle();
            resource.init(id, type, metadata, sha1Hash, data);
            return resource;
        } finally {
            ReusableByteStream.recycle(dataStream);
        }
    }

    @Override
    protected BinaryResource updateBinaryResource(BinaryResource resource, long locatorId) {
        BinaryType type = resource.getType();
        String sha1Hash = resource.getSha1Hash();
        long existingId = runSingleValueQuery("SELECT thing_binary_id FROM thing_binary WHERE mimetype = ? AND sha1 = ? AND length = ?",
                                              Long.MIN_VALUE, type.mimeType(), sha1Hash, resource.getLength());
        if (existingId != Long.MIN_VALUE && existingId != resource.getId()) {
            bumpBinaryResourceReferenceCount(existingId, INCREMENT);
            runUpdateQuery("UPDATE thing_binary_locator SET thing_binary_id = ? WHERE locator_id = ? AND mimetype = ?",
                           existingId, locatorId, resource.getType().mimeType());
            deleteResourceReferenceCount(resource);
            return commonDataSources.getBinaryCache().get(existingId);
        }
        long count = runSingleValueQuery("SELECT reference_count FROM thing_binary WHERE thing_binary_id = ?",
                                         Long.MIN_VALUE,
                                         resource.getId());
        if (count == Long.MIN_VALUE) {
            return insertBinaryResource(resource, locatorId);
        }
        if (count == 1) {
            ReusableByteStream stream = ReusableByteStream.get();
            try {
                stream.load(resource.getData());
                runUpdateQuery("UPDATE thing_binary SET metadata = ?, sha1 = ?, length = ?, data = ? WHERE thing_binary_id = ?",
                               resource.getType().encodeMetadata(resource.getMetadata()),
                               resource.getSha1Hash(),
                               resource.getLength(),
                               stream,
                               resource.getId());
            } catch (Exception e) {
                ReusableByteStream.recycle(stream);
            }
            return resource;
        }
        deleteBinaryResource(resource, locatorId);
        return insertBinaryResource(resource, locatorId);
    }

    private void deleteResourceReferenceCount(BinaryResource resource) {
        bumpBinaryResourceReferenceCount(resource.getId(), DECREMENT);
        runUpdateQuery("DELETE FROM thing_binary WHERE thing_binary_id = ? AND reference_count <= 0", resource.getId());
    }

    @Override
    protected void deleteBinaryResource(BinaryResource resource, long locatorId) {
        runUpdateQuery("DELETE FROM thing_binary_locator WHERE locator_id = ? AND mimetype = ?", locatorId, resource.getType().mimeType());
        deleteResourceReferenceCount(resource);
        resource.recycle();
    }

    @Override
    protected String getProperty(String name) {
        SinglePropertyRowVisitor visitor = new SinglePropertyRowVisitor();
        runSelectQuery(visitor, "SELECT value FROM thing_props WHERE name = ?", name);
        return visitor.getValue();
    }

    @Override
    protected void setProperty(String name, String value) {
        if (runUpdateQuery("UPDATE thing_props SET value = ? WHERE name = ?", value, name) == 0) {
            runUpdateQuery("INSERT INTO thing_props (name, value) VALUES (?, ?)", name, value);
        }
    }

    @Override
    protected void removeProperty(String name) {
        runUpdateQuery("DELETE FROM thing_props WHERE name = ?", name);
    }

    private void bumpBinaryResourceReferenceCount(Long binaryResourceId, Integer by) {
        runUpdateQuery("UPDATE thing_binary SET reference_count = reference_count + ? WHERE thing_binary_id = ?",
                       by,
                       binaryResourceId);
    }

    private String getSql(UpdatedFields updatedFields) {
        switch (updatedFields) {
            case DeleteThing:
                return "DELETE FROM thing WHERE thing_id = ?";
            case InsertThing:
                return "INSERT INTO thing (thing_id, cube_id, type, x, y, z, properties_length, properties) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            case UpdatePosition:
                return "UPDATE thing SET x=?, y=?, z=? WHERE thing_id = ?";
            case UpdatePositionAndCube:
                return "UPDATE thing SET cube_id=?, x=?, y=?, z=? WHERE thing_id = ?";
            case UpdateProperties:
                return "UPDATE thing SET properties_length=?, properties=? WHERE thing_id = ?";
            case UpdatePropertiesAndPosition:
                return "UPDATE thing SET x=?, y=?, z=?, properties_length=?, properties=? WHERE thing_id = ?";
            case UpdatePropertiesPositionAndCube:
                return "UPDATE thing SET cube_id=?, x=?, y=?, z=?, properties_length=?, properties=? WHERE thing_id = ?";
            default:
                return null;
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setQueryParams(UpdatedFields updatedFields, Delta delta, PreparedStatement ps)
            throws IOException, SQLException {
        Point position = delta.getPosition();
        switch (updatedFields) {
            case DeleteThing:
                ps.setLong(1, delta.getId());
                break;
            case InsertThing:
                ps.setObject(1, delta.getId());
                ps.setObject(2, delta.getCubeId());
                ps.setDouble(3, position.x);
                ps.setDouble(4, position.y);
                ps.setDouble(5, position.z);
                ps.setInt(6, delta.getPropertiesLength());
                ps.setBlob(7, delta.getPropertiesStream());
                break;
            case UpdatePosition:
                ps.setDouble(1, position.x);
                ps.setDouble(2, position.y);
                ps.setDouble(3, position.z);
                ps.setObject(4, delta.getId());
                break;
            case UpdatePositionAndCube:
                ps.setObject(1, delta.getCubeId());
                ps.setDouble(2, position.x);
                ps.setDouble(3, position.y);
                ps.setDouble(4, position.z);
                ps.setObject(5, delta.getId());
                break;
            case UpdateProperties:
                ps.setInt(1, delta.getPropertiesLength());
                ps.setBlob(2, delta.getPropertiesStream());
                ps.setObject(3, delta.getId());
                break;
            case UpdatePropertiesAndPosition:
                ps.setDouble(1, position.x);
                ps.setDouble(2, position.y);
                ps.setDouble(3, position.z);
                ps.setInt(4, delta.getPropertiesLength());
                ps.setBlob(5, delta.getPropertiesStream());
                ps.setObject(6, delta.getId());
                break;
            case UpdatePropertiesPositionAndCube:
                ps.setObject(1, delta.getCubeId());
                ps.setDouble(2, position.x);
                ps.setDouble(3, position.y);
                ps.setDouble(4, position.z);
                ps.setInt(5, delta.getPropertiesLength());
                ps.setBlob(6, delta.getPropertiesStream());
                ps.setObject(7, delta.getId());
                break;
        }
    }

    private void close(Statement ps, ResultSet rs) {
        try {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (ps != null && !ps.isClosed()) {
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void close(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void close(Closeable con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Ref<SingleThingRefRowVisitor> singleThingRefRowVisitor =
            new ThreadSingletonSource<SingleThingRefRowVisitor>() {
                @Override
                protected SingleThingRefRowVisitor newInstance() {
                    return new SingleThingRefRowVisitor();
                }
            };

    private final Ref<ThingRefRowMapper> thingRefRowMapper =
            new ThreadSingletonSource<ThingRefRowMapper>() {
                @Override
                protected ThingRefRowMapper newInstance() {
                    return new ThingRefRowMapper();
                }
            };

    private final Ref<BinaryResourceRowVisitor> binaryResourceRowVisitor =
            new ThreadSingletonSource<BinaryResourceRowVisitor>() {
                @Override
                protected BinaryResourceRowVisitor newInstance() {
                    return new BinaryResourceRowVisitor();
                }
            };

    private static byte[] read (InputStream in, int length) {
        byte[] buffer = new byte[length];
        int i = 0;
        try {
            while (i < length) {
                int c = in.read(buffer, i, length - i);
                if (c == -1) {
                    // this is probably not good, we should tell someone
                    throw new EOFException("Expected " + length + " bytes, but only " + i + " bytes were returned");
                }
                i += c;
            }
            if (in.read() != -1) {
                throw new EOFException("Expected " + length + " bytes, but more bytes were returned");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    private class SingleThingRefRowVisitor implements RowVisitor {

        private Long id;
        private ThingRefImpl<?> ref;

        public SingleThingRefRowVisitor init(ThingRefImpl<? extends AbstractThing> ref, Long id) {
            this.ref = ref;
            this.id = id;
            return this;
        }

        public void visit(ResultSet rs) throws SQLException {
            visitImpl(rs);
        }

        private <T extends AbstractThing> void visitImpl(ResultSet rs) throws SQLException {
            String type = rs.getString("type");
            ThingMetadata<T> metadata = ThingFactory.<T>getFor(type).getMetadata();
            Long cubeId = rs.getLong("cube_id");
            int length = rs.getInt("properties_length");
            byte[] data = length == 0 ? null : read(rs.getBinaryStream("properties"), length);
            ref.load(id,
                     rs.getDouble("x"),
                     rs.getDouble("y"),
                     rs.getDouble("z"),
                     metadata,
                     cubeCache.get(cubeId),
                     new ThingProperties(metadata, data));
        }
    }

    protected class ThingRefRowMapper implements RowMapper<ThingRefImpl<AbstractThing>> {

        private CubeImpl cube;

        public RowMapper<ThingRefImpl<AbstractThing>> init(CubeImpl cube) {
            this.cube = cube;
            return this;
        }

        public ThingRefImpl<AbstractThing> map(ResultSet rs) throws SQLException {
            return mapImpl(rs);
        }

        public <T extends AbstractThing> ThingRefImpl<AbstractThing> mapImpl(ResultSet rs) throws SQLException {
            long id = rs.getLong("thing_id");
            ThingFactory<T> type = ThingFactory.getFor(rs.getString("type"));
            double x = rs.getDouble("x");
            double y = rs.getDouble("y");
            double z = rs.getDouble("z");
            int length = rs.getInt("properties_length");
            byte[] data = length == 0 ? null : read(rs.getBinaryStream("properties"), length);
            return build(id, x, y, z, type, data);
        }

        private ThingRefImpl<AbstractThing> build(Long id, double x, double y, double z, ThingFactory<? extends AbstractThing> factory, byte[] data) {
            ThingRefImpl<AbstractThing> ref = createEmptyRef();
            ThingMetadata<? extends AbstractThing> metadata = factory.getMetadata();
            ref.load(id, x, y, z, metadata, cube, new ThingProperties(metadata, data));
            return ref;
        }
    }

    protected class BinaryResourceRowVisitor implements RowVisitor {

        private BinaryResource resource;

        public BinaryResourceRowVisitor init(BinaryResource resource) {
            this.resource = resource;
            return this;
        }

        public void visit(ResultSet rs) throws SQLException {
            Long id = rs.getLong("thing_binary_id");
            String mimeType = rs.getString("mimetype");
            BinaryType type = BinaryTypeRegistry.get(mimeType);
            if (type == null) {
                throw new RuntimeException("Unable to find a BinaryType for " + mimeType);
            }
            String metadata = rs.getString("metadata");
            String sha1Hash = rs.getString("sha1");
            int length = rs.getInt("length");
            InputStream in = rs.getBinaryStream("data");
            byte[] data = new byte[length];
            try {
                for (int i = 0, r = length, c = 1; r > 0 && c > 0; r-= c, i += c) {
                    c = in.read(data, i, r);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            resource.init(id, type, type.decodeMetadata(metadata), sha1Hash, data);
        }
    }

    public class SinglePropertyRowVisitor implements RowVisitor {

        private String value;

        public String getValue() {
            return value;
        }

        public void visit(ResultSet value) throws SQLException {
            this.value = value.getString("value");
        }
    }

    public static void main(String[] args) {
        JDBCDataStore dataStore = new JDBCDataStore();
        dataStore.instanceUuid = UUID.randomUUID();
        List<String> sql = dataStore.loadUpgradeSQL();
        int i = 0;
        for (String s : sql) {
            if (i > 0) System.out.println();
            System.out.printf("--[ Step %d ]---------------------------------------\n", i++);
            System.out.println(s);
        }
    }
}
