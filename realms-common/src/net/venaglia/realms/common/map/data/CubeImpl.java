package net.venaglia.realms.common.map.data;

import net.venaglia.common.util.Ref;
import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.realms.common.map.Cube;
import net.venaglia.realms.common.map.CubeUtils;
import net.venaglia.realms.common.map.things.Thing;
import net.venaglia.realms.common.map.things.ThingMetadata;
import net.venaglia.realms.common.map.things.ThingRef;
import net.venaglia.common.util.Predicate;
import net.venaglia.realms.common.util.cache.Cache;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * User: ed
 * Date: 3/20/14
 * Time: 11:24 PM
 */
public class CubeImpl implements Cube {

    protected Long id = null;
    protected Cache<ThingRef<?>> thingCache;
    protected BoundingBox bounds;

    private Ref<Cache<ThingRef<?>>> cacheRef;
    private int size;
    private int sizeFromModCount;

    protected synchronized void load(Long id, Ref<Cache<ThingRef<?>>> cacheRef) {
        if (this.id != null) {
            throw new IllegalStateException("This cube is already loaded");
        }
        this.id = id;
        this.thingCache = null;
        this.bounds = CubeUtils.getCubeBounds(id);
        this.cacheRef = cacheRef;
        this.size = -1;
        this.sizeFromModCount = -1;
    }

    protected synchronized void recycle() {
        if (!(this.id == null)) {
            this.id = null;
            this.thingCache = null;
            this.bounds = null;
            this.cacheRef = null;
            this.size = -1;
            this.sizeFromModCount = -1;
        }
    }

    protected void ensureLoaded() {
        if (this.id == null) {
            throw new IllegalStateException("This cube is not loaded");
        }
        if (cacheRef == null) {
            return; // already loaded, no-op
        }
        synchronized (this) {
            thingCache = cacheRef.get();
        }
    }

    public Long getId() {
        return id;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public int size() {
        ensureLoaded();
        if (thingCache.getModCount() != sizeFromModCount) {
            sizeFromModCount = thingCache.getModCount();
            size = thingCache.size();
        }
        return size;
    }

    public Iterator<ThingRef<?>> iterator() {
        ensureLoaded();
        return thingCache.iterator();
    }

    public <T extends Thing> Iterator<ThingRef<T>> iterator(final Set<String> types) {
        ensureLoaded();
        return iterator(new Predicate<ThingRef<T>>() {
            public boolean allow(ThingRef<T> value) {
                return types.contains(value.getType());
            }
        });
    }

    public <T extends Thing> Iterator<ThingRef<T>> iterator(final ThingMetadata<T> metadata) {
        ensureLoaded();
        return iterator(new Predicate<ThingRef<T>>() {
            public boolean allow(ThingRef<T> value) {
                return value.getType().equals(metadata.getType());
            }
        });
    }

    protected <T extends Thing> Iterator<ThingRef<T>> iterator(final Predicate<? super ThingRef<T>> predicate) {
        ensureLoaded();
        final Iterator<ThingRef<?>> iter = iterator();
        return new PredicatedThingRefIterator<T>(iter, predicate);
    }

    protected static class PredicatedThingRefIterator<T extends Thing> implements Iterator<ThingRef<T>> {

        private final Iterator<ThingRef<?>> iter;
        private final Predicate<? super ThingRef<T>> predicate;
        private ThingRef<T> current;
        private ThingRef<T> next;

        public PredicatedThingRefIterator(Iterator<ThingRef<?>> iter, Predicate<? super ThingRef<T>> predicate) {
            this.iter = iter;
            this.predicate = predicate;
        }

        public boolean hasNext() {
            if (this.next != null) {
                return true;
            }
            while (iter.hasNext()) {
                //noinspection unchecked
                ThingRef<T> next = (ThingRef<T>)iter.next();
                if (predicate.allow(this.next)) {
                    this.next = next;
                    break;
                }
            }
            return this.next != null;
        }

        public ThingRef<T> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            current = next;
            next = null;
            return current;
        }

        public void remove() {
            if (current != null) {
                if (next != null) {
                    throw new UnsupportedOperationException("This predicated iterator does not support calling remove() after calling hasNext()");
                }
                iter.remove();
                current = null;
            } else {
                throw new IllegalStateException();
            }
        }
    }
}
