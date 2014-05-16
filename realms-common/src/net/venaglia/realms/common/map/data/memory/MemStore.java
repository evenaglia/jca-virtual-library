package net.venaglia.realms.common.map.data.memory;

import net.venaglia.common.util.Pair;
import net.venaglia.common.util.Tuple2;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.realms.common.map.data.AbstractDataStore;
import net.venaglia.realms.common.map.data.BufferedUpdates;
import net.venaglia.realms.common.map.data.CubeImpl;
import net.venaglia.realms.common.map.data.Sequence;
import net.venaglia.realms.common.map.data.ThingRefImpl;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.data.binaries.BinaryType;
import net.venaglia.realms.common.map.data.binaries.BinaryTypeRegistry;
import net.venaglia.realms.common.map.things.AbstractThing;
import net.venaglia.realms.common.map.things.ThingFactory;
import net.venaglia.realms.common.map.things.ThingMetadata;
import net.venaglia.realms.common.map.things.ThingProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * User: ed
 * Date: 5/9/14
 * Time: 7:50 AM
 */
public class MemStore extends AbstractDataStore {

    private final UUID uuid = UUID.randomUUID();
    private final Map<Sequence,Long> sequences = new EnumMap<Sequence,Long>(Sequence.class);
    private final NavigableMap<Long,ThingRow> things = new TreeMap<Long,ThingRow>();
    private final NavigableSet<Pair<Long,Long>> cubeIndex = new TreeSet<Pair<Long,Long>>(new CubeIndexComparator());
    private final NavigableMap<Long,BinaryRow> binaries = new TreeMap<Long,BinaryRow>();
    private final Map<String,String> properties = Collections.synchronizedMap(new TreeMap<String,String>());
    private final Map<String,Long> index = new ConcurrentSkipListMap<String,Long>();
    private final Map<BufferedUpdates.UpdatedFields,Character> modes =
            new EnumMap<BufferedUpdates.UpdatedFields,Character>(BufferedUpdates.UpdatedFields.class);

    public MemStore() {
        for (BufferedUpdates.UpdatedFields updatedFields : BufferedUpdates.UpdatedFields.values()) {
            char mode;
            switch (updatedFields) {
                case InsertThing: mode = 'i'; break;
                case DeleteThing: mode = 'd'; break;
                default: mode = updatedFields.updateCubeId() ? 'U' : 'u';
            }
            modes.put(updatedFields, mode);
        }
    }

    @Override
    protected Tuple2<Long,Long> getNextAvailableIdRange(Sequence seq) {
        Long begin;
        Long end;
        synchronized (sequences) {
            begin = sequences.get(seq);
            end = begin + 64L;
            sequences.put(seq, end);
        }
        return new Pair<Long,Long>(begin, end);
    }

    @Override
    protected long lookupNextAvailableInRange(Sequence seq, long from, long to) {
        switch (seq) {
            case THING:
                return lookupNextAvailableInRange(things, from, to);
            case BINARY:
                return lookupNextAvailableInRange(binaries, from, to);
        }
        throw new IllegalArgumentException("Unsupported Sequence" + seq);
    }

    private long lookupNextAvailableInRange(NavigableMap<Long,?> map, long from, long to) {
        NavigableMap<Long,?> subMab = map.subMap(from, true, to, false);
        return subMab.isEmpty() ? from : subMab.lastKey();
    }

    @Override
    protected void populateCube(Long id, CubeImpl cube) {
        Pair<Long,Long> head = new Pair<Long,Long>(id, 0L);
        Pair<Long,Long> tail = new Pair<Long,Long>(id + 1L, 0L);
        NavigableSet<Pair<Long,Long>> subset = cubeIndex.subSet(head, true, tail, false);
        Collection<ThingRefImpl<?>> refs = new ArrayList<ThingRefImpl<?>>(subset.size());
        for (Pair<Long,Long> entry : subset) {
            ThingRow row = things.get(entry.getB());
            if (row != null) {
                ThingRefImpl<AbstractThing> ref = new ThingRefImpl<AbstractThing>();
                ThingMetadata<?> metadata = ThingFactory.getFor(row.type).getMetadata();
                ThingProperties properties = new ThingProperties(metadata, row.properties);
                ref.load(row.thing_id, row.x, row.y, row.z, metadata, cube, properties);
                refs.add(ref);
            }
        }
        thingCache.seed(refs);
    }

    @Override
    protected void write(BufferedUpdates bufferedUpdates) {
        for (BufferedUpdates.UpdatedFields updatedFields : BufferedUpdates.UpdatedFields.values()) {
            Collection<BufferedUpdates.Delta> subset = bufferedUpdates.subset(updatedFields);
            char mode = modes.get(updatedFields);
            for (BufferedUpdates.Delta delta : subset) {
                ThingRow row = things.get(delta.getId());
                Pair<Long,Long> entry;
                switch (mode) {
                    case 'd':
                        entry = new Pair<Long,Long>(row.cube_id, row.thing_id);
                        cubeIndex.remove(entry);
                        things.remove(delta.getId());
                        break;
                    case 'i':
                        if (row == null) {
                            entry = new Pair<Long,Long>(delta.getCubeId(), delta.getId());
                            cubeIndex.add(entry);
                            row = new ThingRow();
                            things.put(delta.getId(), populateRow(delta, updatedFields, row));
                            break;
                        }
                    case 'U':
                        entry = new Pair<Long,Long>(row.cube_id, row.thing_id);
                        cubeIndex.remove(entry);
                        entry = new Pair<Long,Long>(delta.getCubeId(), delta.getId());
                        cubeIndex.add(entry);
                    case 'u':
                        populateRow(delta, updatedFields, row);
                        break;
                }
            }
        }
    }

    private ThingRow populateRow(BufferedUpdates.Delta delta, BufferedUpdates.UpdatedFields updatedFields, ThingRow row) {
        Point position = delta.getPosition();
        if (updatedFields.updateCubeId()) {
            row.cube_id = delta.getCubeId();
        }
        if (updatedFields.updatePosition()) {
            row.x = position.x;
            row.y = position.y;
            row.z = position.z;
        }
        if (updatedFields.updateProperties()) {
            row.properties = delta.getProperties();
            row.properties_length = delta.getPropertiesLength();
        }
        return row;
    }

    @Override
    protected <T extends AbstractThing> void populateRef(Long id, ThingRefImpl<T> ref) {
        ThingRow row = things.get(id);
        if (row != null) {
            ThingMetadata<T> metadata = ThingFactory.<T>getFor(row.type).getMetadata();
            CubeImpl cube = cubeCache.get(row.cube_id);
            ThingProperties properties = new ThingProperties(metadata, row.properties);
            ref.load(row.thing_id, row.x, row.y, row.z, metadata, cube, properties);
        }
    }

    @Override
    protected void populateBinaryResource(Long id, BinaryResource resource) {
        synchronized (binaries) {
            BinaryRow row;
            synchronized (binaries) {
                row = binaries.get(id);
            }
            if (row == null) return;
            BinaryType type = BinaryTypeRegistry.get(row.mimetype);
            if (type == null) {
                throw new RuntimeException("Unable to find a BinaryType for " + row.mimetype);
            }
            resource.init(id, type, row.locator_id, type.decodeMetadata(row.metadata), row.sha1, row.data);
        }
    }

    @Override
    protected Long findBinaryResourceId(String mimetype, long locatorId) {
        return index.get(getLocatorKey(locatorId, mimetype));
    }

    @Override
    protected BinaryResource insertBinaryResource(BinaryResource resource) {
        synchronized (binaries) {
            return insertBinaryResourceImpl(resource);
        }
    }

    private BinaryResource insertBinaryResourceImpl(BinaryResource resource) {
        String sha1Hash = resource.getSha1Hash();
        int length = resource.getLength();
        BinaryType type = resource.getType();
        String key = getHashKey(sha1Hash, length, type.mimeType());
        Long existingId = index.get(key);
        if (existingId != null) {
            binaries.get(existingId).reference_count++;
            return commonDataSources.getBinaryCache().get(existingId);
        }
        Long id = commonDataSources.nextId(Sequence.BINARY);
        byte[] data = resource.getData();
        Map<String,Object> metadata = resource.getMetadata();

        BinaryRow row = new BinaryRow();
        row.thing_binary_id = id;
        row.reference_count = 1;
        row.mimetype = type.mimeType();
        row.locator_id = resource.getLocatorId();
        row.metadata = type.encodeMetadata(metadata);
        row.sha1 = sha1Hash;
        row.length = length;
        row.data = data == null ? null : data.clone();

        addToIndex(row);
        binaries.put(id, row);

        resource.recycle();
        resource.init(id, type, row.locator_id, metadata, sha1Hash, data);
        return resource;
    }

    @Override
    protected BinaryResource updateBinaryResource(BinaryResource resource) {
        synchronized (binaries) {
            return updateBinaryResourceImpl(resource);
        }
    }

    private BinaryResource updateBinaryResourceImpl(BinaryResource resource) {
        String sha1Hash = resource.getSha1Hash();
        int length = resource.getLength();
        BinaryType type = resource.getType();
        String key = getHashKey(sha1Hash, length, type.mimeType());
        Long existingId = index.get(key);
        if (existingId != null && !existingId.equals(resource.getId())) {
            binaries.get(existingId).reference_count++;
            deleteBinaryResourceImpl(resource);
            return commonDataSources.getBinaryCache().get(existingId);
        }
        BinaryRow row = binaries.get(resource.getId());
        if (row == null) {
            return insertBinaryResourceImpl(resource);
        }
        removeFromIndex(row);
        if (row.reference_count == 1) {
            row.metadata = type.encodeMetadata(resource.getMetadata());
            row.sha1 = resource.getSha1Hash();
            row.length = resource.getLength();
            row.data = resource.getData().clone();
        }
        addToIndex(row);
        deleteBinaryResourceImpl(resource);
        return insertBinaryResourceImpl(resource);
    }

    @Override
    protected void deleteBinaryResource(BinaryResource resource) {
        synchronized (binaries) {
            deleteBinaryResourceImpl(resource);
        }
    }

    @Override
    protected String getProperty(String name) {
        return properties.get(name);
    }

    @Override
    protected void setProperty(String name, String value) {
        properties.put(name, value);
    }

    @Override
    protected void removeProperty(String name) {
        properties.remove(name);
    }

    private void deleteBinaryResourceImpl(BinaryResource resource) {
        BinaryRow row = binaries.get(resource.getId());
        if (row != null) {
            row.reference_count--;
            if (row.reference_count <= 0) {
                removeFromIndex(resource);
                binaries.remove(resource.getId());
            }
        }
        resource.recycle();
    }

    private String getLocatorKey(long locatorId, String mimetype) {
        return String.format("%x:%s:loc:bin:ix", locatorId, mimetype);
    }

    private String getHashKey(String sha1Hash, int length, String mimetype) {
        return String.format("%s:%x:%s:hash:bin:ix", sha1Hash, length, mimetype);
    }

    private void addToIndex(BinaryResource resource) {
        addToIndexImpl(resource.getId(),
                       getLocatorKey(resource),
                       getHashKey(resource));
    }

    private void addToIndex(BinaryRow row) {
        addToIndexImpl(row.thing_binary_id,
                       getLocatorKey(row.locator_id, row.mimetype),
                       getHashKey(row.sha1, row.length, row.mimetype));
    }

    private void removeFromIndex(BinaryResource resource) {
        removeFromInexImpl(resource.getId(),
                           getLocatorKey(resource),
                           getHashKey(resource));
    }

    private void removeFromIndex(BinaryRow row) {
        removeFromInexImpl(row.thing_binary_id,
                           getLocatorKey(row.locator_id, row.mimetype),
                           getHashKey(row.sha1, row.length, row.mimetype));
    }

    private String getHashKey(BinaryResource resource) {
        return getHashKey(resource.getSha1Hash(), resource.getLength(), resource.getType().mimeType());
    }

    private String getLocatorKey(BinaryResource resource) {
        return getLocatorKey(resource.getLocatorId(), resource.getType().mimeType());
    }

    private void addToIndexImpl(Long id, String... keys) {
        for (String key : keys) {
            if (index.containsKey(key)) {
                throw new RuntimeException("Key already exists: " + key);
            }
        }
        for (String key : keys) {
            index.put(key, id);
        }
    }

    private void removeFromInexImpl(Long id, String... keys) {
        for (String key : keys) {
            if (!id.equals(index.get(key))) {
                throw new RuntimeException("Key either does not exist, or references a different object: " + key + " -> " + index.get(key));
            }
        }
        for (String key : keys) {
            index.remove(key);
        }
    }

    public synchronized void init() {
        if (sequences.isEmpty()) {
            System.out.println("Using memory data store");
            for (Sequence sequence : Sequence.values()) {
                sequences.put(sequence, 1000L);
            }
            bootstrapPersistentProperties();
        }
    }

    public UUID getInstanceUuid() {
        return uuid;
    }

    private static class CubeIndexComparator implements Comparator<Pair<Long,Long>> {

        private final Comparator<Pair<Long,?>> a = Pair.compareA();
        private final Comparator<Pair<?,Long>> b = Pair.compareB();

        public int compare(Pair<Long,Long> o1, Pair<Long,Long> o2) {
            int cmp = a.compare(o1, o2);
            return cmp == 0 ? b.compare(o1, o2) : cmp;
        }
    }

    private static class ThingRow {
        long thing_id;
        long cube_id;
        long container_id;
        String type;
        double x;
        double y;
        double z;
        int properties_length;
        byte[] properties;
    }

    private static class BinaryRow {
        long thing_binary_id;
        int reference_count;
        String mimetype;
        long locator_id;
        String metadata;
        String sha1;
        int length;
        byte[] data;
    }
}
