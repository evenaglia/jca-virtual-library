package net.venaglia.realms.common.map.data;

import net.venaglia.common.util.Ref;
import net.venaglia.common.util.Tuple2;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.Cube;
import net.venaglia.realms.common.map.CubeUtils;
import net.venaglia.realms.common.map.DataStore;
import net.venaglia.realms.common.map.PropertyStore;
import net.venaglia.realms.common.map.Source;
import net.venaglia.realms.common.map.UniqueIdSource;
import net.venaglia.realms.common.map.things.ThingProperties;
import net.venaglia.realms.common.map.things.ThingRef;
import net.venaglia.realms.common.map.things.ThingWriter;
import net.venaglia.realms.common.map.things.AbstractThing;
import net.venaglia.realms.common.map.data.binaries.BinaryCache;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.data.binaries.BinarySource;
import net.venaglia.realms.common.util.Visitor;
import net.venaglia.realms.common.util.cache.Cache;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 3/20/14
 * Time: 11:22 PM
 */
public abstract class AbstractDataStore implements DataStore {

    protected final ThingCache thingCache;
    protected final CubeCache cubeCache;
    protected final BinaryCache binaryCache;
    protected final PropertyStore propertyStore;
    protected final DirtyThingQueue dirtyThingQueue;
    protected final CommonDataSources commonDataSources;

    protected final AtomicInteger dirtyThingCount = new AtomicInteger();
    protected final CheckpointRunnable dirtyThingCheckpoint = new CheckpointRunnable();
    protected final AtomicReference<Map<Long,ThingRef<?>>> dirtyThings =
            new AtomicReference<Map<Long,ThingRef<?>>>(new ConcurrentSkipListMap<Long,ThingRef<?>>());

    private final AtomicBoolean open = new AtomicBoolean(true);

    protected AbstractDataStore() {
        final Ref<UUID> instanceUUIDRef = new Ref<UUID>() {
            public UUID get() {
                return getInstanceUuid();
            }
        };
        new Thread(dirtyThingCheckpoint, "Flush Dirty Things");
        dirtyThings.set(new ConcurrentSkipListMap<Long,ThingRef<?>>());
        thingCache = new ThingCache(new Source<ThingRefImpl<? extends AbstractThing>>() {
            public ThingRefImpl<? extends AbstractThing> createEmpty() {
                return createEmptyRef();
            }

            public void populate(Long id, ThingRefImpl<? extends AbstractThing> thingRef) {
                populateRef(id, thingRef);
            }
        });
        cubeCache = new CubeCache(new Source<CubeImpl>() {
            public CubeImpl createEmpty() {
                return createEmptyCube();
            }

            public void populate(final Long id, final CubeImpl cube) {
                cube.load(id, new Ref<Cache<ThingRef<?>>>() {
                    @SuppressWarnings("unchecked")
                    public Cache<ThingRef<?>> get() {
                        return (Cache<ThingRef<?>>)(Cache)thingCache.forCube(cube);
                    }
                });
                populateCube(id, cube);
            }
        });
        binaryCache = new BinaryCache(new BinarySource() {
            public BinaryResource createEmpty() {
                return createEmptyBinaryResource();
            }

            public void populate(Long id, BinaryResource binaryResource) {
                populateBinaryResource(id, binaryResource);
            }

            public BinaryResource insert(BinaryResource resource, long locatorId) {
                return insertBinaryResource(resource, locatorId);
            }

            public BinaryResource update(BinaryResource resource, long locatorId, byte[] data) {
                return updateBinaryResource(mutateBinaryResource(resource, data), locatorId);
            }

            public void delete(BinaryResource resource, long locatorId) {
                deleteBinaryResource(resource, locatorId);
            }

            public Long lookupIdByLocator(String mimetype, long locatorId) {
                return findBinaryResourceId(mimetype, locatorId);
            }
        });
        propertyStore = new PropertyStore() {
            public String get(String name) {
                return getProperty(name);
            }

            public void set(String name, String value) {
                setProperty(name, value);
            }

            public void remove(String name) {
                removeProperty(name);
            }
        };
        dirtyThingQueue = new DirtyThingQueue() {

            private int sizeLimit = Integer.MIN_VALUE;

            public void add(ThingRefImpl<?> thingRef) {
                ensureOpen();
                if (sizeLimit == Integer.MIN_VALUE) {
                    sizeLimit = Configuration.THING_CHECKPOINT_SIZE.getInteger(1500);
                }
                if (dirtyThings.get().put(thingRef.getId(), thingRef) == null) {
                    if (dirtyThingCount.incrementAndGet() > sizeLimit) {
                        synchronized (dirtyThingCheckpoint) {
                            dirtyThingCheckpoint.notifyAll();
                        }
                    }
                }
            }
        };
        IdSourceProvider idSourceProvider = new IdSourceProvider() {
            public UniqueIdSource getIdSource(Sequence sequence) {
                return new IdSource(sequence, instanceUUIDRef);
            }
        };
        commonDataSources = new CommonDataSources(thingCache,
                                                  cubeCache,
                                                  binaryCache,
                                                  propertyStore,
                                                  dirtyThingQueue,
                                                  idSourceProvider);
    }

    public CommonDataSources getCommonDataSources() {
        return commonDataSources;
    }

    public void intersect(BoundingVolume<?> bounds, Visitor<Cube> visitor) {
        CubeUtils.CubeIterator cubeIterator = CubeUtils.intersectionIterator(bounds);
        while (cubeIterator.next()) {
            visitor.visit(cubeCache.get(cubeIterator.getCubeId()));
        }
    }

    public void closeAndFlush(long timeoutMS) {
        if (open.compareAndSet(true, false)) {
            dirtyThingCheckpoint.stop(timeoutMS);
        }
    }

    protected final void ensureOpen() {
        if (!open.get()) {
            throw new IllegalStateException("Data store has been closed");
        }
    }

    protected final void bootstrapPersistentProperties() {
        StringBuilder buffer = new StringBuilder(256);
        for (Configuration configuration : Configuration.values()) {
            if (configuration.isWritable() && configuration.isPersistent()) {
                String defaultValue = configuration.getString();
                if (defaultValue != null) {
                    configuration.setString(defaultValue);
                    buffer.append("\n\t").append(configuration);
                }
            } else if (configuration == Configuration.GEOSPEC) {
                setProperty("geospec", configuration.getString("LARGE"));
                buffer.append("\t").append(configuration);
            }
        }
        System.out.println(buffer);
    }

    protected abstract Tuple2<Long,Long> getNextAvailableIdRange(Sequence seq);
    protected abstract long lookupNextAvailableInRange(Sequence seq, long from, long to);

    protected CubeImpl createEmptyCube() {
        return new CubeImpl();
    }

    protected abstract void populateCube(Long id, CubeImpl cube);

    protected abstract void write(BufferedUpdates bufferedUpdates);

    protected <T extends AbstractThing> ThingRefImpl<T> createEmptyRef() {
        return new ThingRefImpl<T>();
    }

    protected abstract <T extends AbstractThing> void populateRef(Long id, ThingRefImpl<T> ref);

    protected BinaryResource createEmptyBinaryResource() {
        return new BinaryResource();
    }

    protected BinaryResource mutateBinaryResource(BinaryResource resource, byte[] data) {
        BinaryResource mutated = createEmptyBinaryResource();
        Map<String,Object> metadata = resource.getType().generateMetadata(data);
        mutated.init(resource.getId(), resource.getType(), metadata, null, data);
        resource.recycle();
        return mutated;
    }

    protected abstract void populateBinaryResource(Long id, BinaryResource binaryResource);
    protected abstract Long findBinaryResourceId(String mimetype, long locatorId);
    protected abstract BinaryResource insertBinaryResource(BinaryResource resource, long locatorId);
    protected abstract BinaryResource updateBinaryResource(BinaryResource resource, long locatorId);
    protected abstract void deleteBinaryResource(BinaryResource resource, long locatorId);
    protected abstract String getProperty(String name);
    protected abstract void setProperty(String name, String value);
    protected abstract void removeProperty(String name);

    private class IdSource extends AbstractUniqueIdSource {

        private IdSource(Sequence seq, Ref<UUID> instanceUUIDRef) {
            super(seq, instanceUUIDRef);
        }

        @Override
        protected IdRange getNextRange() {
            Tuple2<Long,Long> range = getNextAvailableIdRange(getSequence());
            return new IdRange(range.getA(), range.getB());
        }

        @Override
        protected long lookupNextAvailableInRange(IdRange range) {
            return AbstractDataStore.this.lookupNextAvailableInRange(getSequence(), range.getStart(), range.getEnd());
        }
    }

    private class CheckpointRunnable implements Runnable, ThingWriter {

        private final AtomicInteger running = new AtomicInteger(0);
        private final BufferedUpdates bufferedUpdates = new BufferedUpdates();

        private ThingRef<?> currentThing;
        private Map<Long,ThingRef<?>> dirtyThingsToFlush = new ConcurrentSkipListMap<Long,ThingRef<?>>();

        private int mode = 0;
        private Point position;
        private Long cubeId;
        private ThingProperties properties;

        public void run() {
            final int wait = Configuration.THING_CHECKPOINT_WAIT.getInteger(5000);
            if (!running.compareAndSet(0, 1)) {
                throw new IllegalStateException("CheckpointRunnable was not in a waiting state: " + running.getAndIncrement());
            }
            while (running.get() == 1 || !dirtyThings.get().isEmpty()) {
                if (running.get() == 1) {
                    synchronized (this) {
                        try {
                            wait(wait);
                        } catch (InterruptedException e) {
                            // don't care
                        }
                    }
                }
                dirtyThingsToFlush = dirtyThings.getAndSet(dirtyThingsToFlush);
                for (ThingRef<?> thingRef : dirtyThingsToFlush.values()) {
                    currentThing = thingRef;
                    currentThing.writeChangesTo(this, false);
                    switch (mode) {
                        case 1:
                            bufferedUpdates.update(thingRef.getId(), position, cubeId, properties == null ? null : properties.updateBuffer());
                            break;
                        case 2:
                            bufferedUpdates.update(thingRef.getId(), position, cubeId, properties.updateBuffer());
                            break;
                        case 3:
                            bufferedUpdates.update(thingRef.getId(), position, cubeId, properties.updateBuffer());
                            break;
                        case 4:
                            bufferedUpdates.add(thingRef.getId(), position, cubeId, properties.updateBuffer());
                            break;
                        case 8:
                            bufferedUpdates.remove(thingRef.getId());
                            break;
                    }
                    mode = 0;
                }
                currentThing = null;
                dirtyThingsToFlush.clear();
                if (!isReadonly()) {
                    write(bufferedUpdates);
                }
                bufferedUpdates.clear();
            }
            if (running.compareAndSet(2, 3)) {
                synchronized (this) {
                    notifyAll();
                }
            } else {
                throw new IllegalStateException("CheckpointRunnable was not in a shutting down state");
            }
        }

        public void unchanged(Point position, Cube cube, ThingProperties properties) {
            this.position = position;
            this.cubeId = cube == null ? null : cube.getId();
            this.properties = properties;
        }

        public void updatePosition(Point position, Cube cube) {
            if (position == null) throw new NullPointerException("position");
            if (mode != 0 && mode != 2) throw new IllegalStateException();
            mode |= 1;
            this.position = position;
            this.cubeId = cube.getId();
        }

        public void updateThing(ThingProperties properties) {
            if (properties == null) throw new NullPointerException("properties");
            if (mode != 0 && mode != 1) throw new IllegalStateException();
            mode |= 2;
            this.properties = properties;
        }

        public void addThing(Point position, Cube cube, ThingProperties properties) {
            if (mode != 0) throw new IllegalStateException();
            mode |= 4;
        }

        public void deleteThing() {
            if (mode != 0) throw new IllegalStateException();
            mode |= 8;
        }

        public void stop(long timeout) {
            long waitUntil = System.currentTimeMillis() + timeout;
            if (running.compareAndSet(1, 2)) {
                while (true) {
                    synchronized (this) {
                        this.notifyAll();
                        long wait = waitUntil - System.currentTimeMillis();
                        if (wait > 0) {
                            try {
                                this.wait(wait);
                            } catch (InterruptedException e) {
                                // don't care
                            }
                        } else {
                            return;
                        }
                    }
                }
            }
        }
    }
}
