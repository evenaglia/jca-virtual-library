package net.venaglia.realms.common.map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.ObjectMap;
import net.venaglia.realms.common.map.db.DB;
import net.venaglia.realms.common.map.db.DBException;
import net.venaglia.realms.common.map.db.DatabaseOptions;
import net.venaglia.realms.common.map.db.impl.DiskDB;
import net.venaglia.realms.common.map.db.impl.KryoSerializer;
import net.venaglia.realms.common.map.db.impl.ReadOnlyDiskSpatialIndex;
import net.venaglia.realms.common.map.elements.DetailAcre;
import net.venaglia.realms.common.map.elements.GraphAcre;
import net.venaglia.realms.common.map.elements.WorldElement;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.util.BasicSpatialMap;
import net.venaglia.common.util.Series;
import net.venaglia.common.util.Tuple2;
import net.venaglia.gloo.util.impl.OctreeMap;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: ed
 * Date: 1/28/13
 * Time: 8:43 AM
 */
public class WorldMap {

    private final Map<Class<?>,Database<?>> allDatabases = new HashMap<Class<?>,Database<?>>();

    public final SpatialDatabase<GraphAcre> graph = getDBImpl(GraphAcre.class);
    public final Database<DetailAcre> detail = getDB(DetailAcre.class);

    private final DB.Mode mode;

    public WorldMap() {
        mode = DB.Mode.FULLY_MUTABLE;
        for (Database<?> db : allDatabases.values()) {
            try {
                db.init();
            } catch (IOException e) {
                throw new DBException(e);
            }
        }
    }

    public <T extends WorldElement> Database<T> getDB(Class<T> type) {
        return getDBImpl(type);
    }

    @SuppressWarnings("unchecked")
    private <T extends WorldElement,DB extends Database<T>> DB getDBImpl(Class<T> type) {
        Database<T> db = (Database<T>)allDatabases.get(type);
        if (db != null) {
            return (DB)db;
        }
        DatabaseOptions options = type.getAnnotation(DatabaseOptions.class);
        db = options != null && options.spatial() ? new SpatialDatabase<T>(type) : new Database<T>(type);
        allDatabases.put(type, db);
        return (DB)db;
    }

    protected class WorldElementSerializer<T extends WorldElement> extends KryoSerializer<T> {

        public WorldElementSerializer(Class<T> type) {
            super(type);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected T deserializeImpl(Input input) {
            ObjectMap context = getKryo().getContext();
            context.put(WorldMap.class, WorldMap.this);
            try {
                return super.deserializeImpl(input);
            } finally {
                context.remove(WorldMap.class);
            }
        }

        @Override
        protected void configureKryo(Kryo kryo) {
            kryo.addDefaultSerializer(WorldMap.class, new com.esotericsoftware.kryo.Serializer<WorldMap>() {
                @Override
                public void write(Kryo kryo, Output output, WorldMap worldMap) {
                    // no-op
                }

                @SuppressWarnings("unchecked")
                @Override
                public WorldMap read(Kryo kryo, Input input, Class<WorldMap> type) {
                    return (WorldMap)kryo.getContext().get(WorldMap.class);
                }
            });
            kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        }
    }

    public class Database<V extends WorldElement> implements DB<V> {

        private final Class<V> type;

        private DiskDB<V> delegate;

        protected Database(Class<V> type) {
            this.type = type;
        }

        void init() throws IOException{
            delegate = initDB(type);
            delegate.open();
        }

        DiskDB<V> initDB(Class<V> type) {
            return new DiskDB<V>(type, mode, new WorldElementSerializer<V>(type));
        }

        public boolean isReadOnly() {
            return delegate.isReadOnly();
        }

        public boolean hasUncommittedChanges() {
            return delegate.hasUncommittedChanges();
        }

        public void commitChanges() {
            delegate.commitChanges();
        }

        public boolean contains(Integer key) {
            return delegate.contains(key);
        }

        public V get(Integer key) {
            return delegate.get(key);
        }

        public void put(Integer key, V value) {
            delegate.put(key, value);
        }

        public void clear() {
            delegate.clear();
        }

        public int size() {
            return delegate.size();
        }

        public Iterator<Tuple2<Integer,V>> iterator() {
            return delegate.iterator();
        }

        public File getFile() {
            return delegate.getFile();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    public class SpatialDatabase<V extends WorldElement> extends Database<V> implements BasicSpatialMap<V> {

        private ReadOnlyDiskSpatialIndex<V> delegate;

        public SpatialDatabase(Class<V> type) {
            super(type);
        }

        @Override
        void init() throws IOException {
            super.init();
            delegate.open();
        }

        @Override
        DiskDB<V> initDB(Class<V> type) {
            DiskDB<V> db = super.initDB(type);
            delegate = new ReadOnlyDiskSpatialIndex<V>(type, db);
            return db;
        }

        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        public boolean contains(double x, double y, double z) {
            return delegate.contains(x, y, z);
        }

        public V get(Point p) {
            return delegate.get(p);
        }

        public V get(double x, double y, double z) {
            return delegate.get(x, y, z);
        }

        public V get(Point p, double r) {
            return delegate.get(p, r);
        }

        public V get(double x, double y, double z, double r) {
            return delegate.get(x, y, z, r);
        }

        public int intersect(BoundingVolume<?> region, BasicConsumer<V> consumer) {
            return delegate.intersect(region, consumer);
        }

        public BoundingVolume<?> getBounds() {
            return delegate.getBounds();
        }

        public Series<V> asSeries() {
            return new Series<V>() {
                public int size() {
                    return SpatialDatabase.this.size();
                }

                public Iterator<V> iterator() {
                    final Iterator<Tuple2<Integer,V>> delegate = SpatialDatabase.this.iterator();
                    return new Iterator<V>() {
                        public boolean hasNext() {
                            return delegate.hasNext();
                        }

                        public V next() {
                            return delegate.next().getB();
                        }

                        public void remove() {
                            delegate.remove();
                        }
                    };
                }
            };
        }

        public void updateSpatialMap(OctreeMap<V> map) throws IOException {
            if (mode.isReadOnly()) {
                throw new UnsupportedOperationException();
            }
            delegate.close();
            delegate.buildWriter(WorldElement.ID_PROVIDER);
            delegate.open();
        }
    }

}
