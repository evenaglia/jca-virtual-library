package net.venaglia.realms.common.map.data;

import net.venaglia.realms.common.map.Cube;
import net.venaglia.realms.common.map.Source;
import net.venaglia.realms.common.map.things.AbstractThing;
import net.venaglia.realms.common.util.cache.BasicWorkingCache;
import net.venaglia.realms.common.util.cache.Cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
* User: ed
* Date: 3/27/14
* Time: 10:29 PM
*/
public class ThingCache extends BasicWorkingCache<ThingRefImpl<?>> {

    protected final Source<ThingRefImpl<? extends AbstractThing>> source;
    protected final NavigableMap<ThingRefImpl<?>,Node<ThingRefImpl<?>>> thingsByRef;

    public ThingCache(Source<ThingRefImpl<? extends AbstractThing>> source) {
        this.source = source;
        this.thingsByRef = new ConcurrentSkipListMap<ThingRefImpl<?>,Node<ThingRefImpl<?>>>(ThingRefImpl.ORDER_BY_CUBE_AND_TYPE);
    }

    public Cache<ThingRefImpl<?>> forCube(Cube cube) {
        BoundaryRef boundaryRef = BoundaryRef.forCubeAndType(cube.getId(), "");
        return new CubeSubCache(cube.getId(), thingsByRef.subMap(boundaryRef, boundaryRef.next()));
    }

    @Override
    protected ThingRefImpl<?> miss(Long id) {
        ThingRefImpl<? extends AbstractThing> ref = source.createEmpty();
        source.populate(id, ref);
        return ref;
    }

    @Override
    protected void admit(Node<ThingRefImpl<?>> node) {
        thingsByRef.put(node.getValue(), node);
    }

    @Override
    protected void evict(Node<ThingRefImpl<?>> node) {
        thingsByRef.remove(node.getValue());
    }

    public void update(ThingRefImpl<?> ref, Runnable doUpdate) {
        lock.lock();
        try {
            Node<ThingRefImpl<?>> node = thingsByRef.remove(ref);
            doUpdate.run();
            if (node != null) {
                thingsByRef.put(ref, node);
            }
        } finally {
            lock.unlock();
        }
    }

    private static class BoundaryRef extends ThingRefImpl<AbstractThing> {

        private BoundaryRef next() {
            if ("".equals(type)) {
                return forCubeAndType(id + 1L, type);
            } else {
                return forCubeAndType(id, type + '\0');
            }
        }

        protected static BoundaryRef forCubeAndType(Long cubeID, String type) {
            BoundaryRef ref = new BoundaryRef();
            ref.cube.id = cubeID;
            ref.type = type == null ? "" : type;
            return ref;
        }
    }

    private class CubeSubCache implements Cache<ThingRefImpl<?>> {

        private final Long cubeId;
        private final SortedMap<ThingRefImpl<?>, Node<ThingRefImpl<?>>> subMap;

        public CubeSubCache(Long cubeId, SortedMap<ThingRefImpl<?>, Node<ThingRefImpl<?>>> subMap) {
            this.cubeId = cubeId;
            this.subMap = subMap;
        }

        public int size() {
            return subMap.size();
        }

        public int getModCount() {
            return ThingCache.this.getModCount();
        }

        public Iterator<ThingRefImpl<?>> iterator() {
            final Iterator<Node<ThingRefImpl<?>>> iter = subMap.values().iterator();
            return new Iterator<ThingRefImpl<?>>() {
                public boolean hasNext() {
                    return iter.hasNext();
                }

                public ThingRefImpl<?> next() {
                    return iter.next().getValue();
                }

                public void remove() {
                    iter.remove();
                }
            };
        }

        public ThingRefImpl<?> get(Long id) {
            ThingRefImpl<?> thingRef = ThingCache.this.get(id);
            return thingRef.cube.id.equals(cubeId) ? thingRef : null;
        }

        public boolean seed(ThingRefImpl<?> value) {
            if (value.cube.id.equals(cubeId)) {
                return ThingCache.this.seed(value);
            } else {
                throw new IllegalArgumentException();
            }
        }

        public boolean seed(Collection<? extends ThingRefImpl<?>> values) {
            for (ThingRefImpl<?> value : values) {
                if (!value.cube.id.equals(cubeId)) {
                    throw new IllegalArgumentException();
                }
            }
            return ThingCache.this.seed(values);
        }

        public boolean remove(ThingRefImpl<?> value) throws UnsupportedOperationException {
            if (value.cube.id.equals(cubeId)) {
                return ThingCache.this.remove(value);
            } else {
                throw new IllegalArgumentException();
            }
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public void evictOldest() {
            throw new UnsupportedOperationException();
        }
    }
}
