package net.venaglia.realms.common.map.data;

import net.venaglia.common.util.RecycleBuffer;
import net.venaglia.common.util.impl.RecycleBufferImpl;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.util.impl.AbstractMutableEntry;
import net.venaglia.realms.common.map.Cube;
import net.venaglia.realms.common.map.CubeUtils;
import net.venaglia.realms.common.map.things.ThingFactory;
import net.venaglia.realms.common.map.things.ThingMetadata;
import net.venaglia.realms.common.map.things.ThingProperties;
import net.venaglia.realms.common.map.things.ThingRef;
import net.venaglia.realms.common.map.things.ThingWriter;
import net.venaglia.realms.common.map.things.AbstractThing;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 3/21/14
 * Time: 8:15 PM
 */
public class ThingRefImpl<T extends AbstractThing> extends AbstractMutableEntry<T> implements ThingRef<T> {

    private static final RecycleBuffer<ThingRefImpl<?>> UNUSED =
            RecycleBufferImpl.forParameterizedType(ThingRefImpl.class, 1500);

    public static final Comparator<ThingRefImpl<?>> ORDER_BY_CUBE_AND_TYPE =
            new Comparator<ThingRefImpl<?>>() {
                public int compare(ThingRefImpl<?> r1, ThingRefImpl<?> r2) {
                    long c1 = r1.cube.id;
                    long c2 = r2.cube.id;
                    int cmp = c1 < c2 ? -1 : c1 > c2 ? 1 : 0;
                    return cmp == 0 ? r1.type.compareTo(r2.type) : cmp;
                }
            };

    protected Long id = null;
    protected ThingMetadata<T> metadata;
    protected ThingProperties properties;
    protected CubeImpl cube;
    protected CommonDataSources commonDataSources;

    protected T thing;
    protected String type;

    private Point point;
    private AtomicBoolean moved = new AtomicBoolean();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public ThingRefImpl() {
        super(Double.NaN, Double.NaN, Double.NaN);
    }

    public Long getId() {
        return id;
    }

    public Cube getCube() {
        return cube;
    }

    public Point getPoint() {
        if (point == null) {
            point = new Point(x, y, z);
        }
        return point;
    }

    public boolean move(double x, double y, double z) throws IndexOutOfBoundsException, UnsupportedOperationException {
        if (!metadata.isMutableThing()) {
            throw new UnsupportedOperationException();
        }
        lock(true);
        try {
            if (this.x != x || this.y != y || this.z != z) {
                point = null;
                long newId = CubeUtils.getCubeID(x, y, z);
                this.x = x;
                this.y = y;
                this.z = z;
                moved.set(true);
                if (newId != cube.getId()) {
                    cube = commonDataSources.getCubeCache().get(newId);
                }
                commonDataSources.getDirtyThingQueue().add(this);
                return true;
            }
        } finally {
            unlock(true);
        }
        return false;
    }

    public String getType() {
        return type;
    }

    public ThingMetadata<T> getMetadata() {
        return metadata;
    }

    public boolean isContainedBy(BoundingVolume<?> region) {
        return region.includes(x, y, z);
    }

    public boolean isAt(double x, double y, double z) {
        return this.x == x && this.y == y && this.z == z;
    }

    public boolean move(Point p) throws IndexOutOfBoundsException, UnsupportedOperationException {
        return move(p.x, p.y, p.z);
    }

    public boolean remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public T get() {
        if (thing == null) {
            thing = ThingFactory.getFor(metadata).createEmpty();
            AbstractThing.load(this, thing, properties);
        }
        return thing;
    }

    public void lock(boolean forWrite) {
        if (lock != null) {
            (forWrite ? lock.writeLock() : lock.readLock()).lock();
        }
    }

    public void unlock(boolean forWrite) {
        if (lock != null) {
            (forWrite ? lock.writeLock() : lock.readLock()).unlock();
        }
    }

    public void writeChangesTo(ThingWriter thingWriter, boolean setUnchangedValuesFirst) {
        boolean moved = this.moved.getAndSet(false);
        boolean hydrated = thing != null;
        if (setUnchangedValuesFirst) {
            thingWriter.unchanged(moved ? null : getPoint(), moved ? null : cube, hydrated ? null : properties);
        }
        if (moved) {
            thingWriter.updatePosition(getPoint(), cube);
        }
        if (hydrated) {
            thing.writeChangesTo(properties, thingWriter);
        }
    }

    public void load(Long id,
                     double x,
                     double y,
                     double z,
                     ThingMetadata<?> metadata,
                     CubeImpl cube,
                     ThingProperties properties) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        //noinspection unchecked
        this.metadata = (ThingMetadata<T>)metadata;
        this.moved = metadata.isMutableThing() ? new AtomicBoolean() : null;
        this.lock = metadata.isMutableThing() ? new ReentrantReadWriteLock() : null;
        this.properties = properties;
        this.type = metadata.getType();
        this.cube = cube;
        this.point = null;
        this.thing = null;
    }

    protected void recycle() {
        this.id = null;
        this.x = Double.NaN;
        this.y = Double.NaN;
        this.z = Double.NaN;
        this.metadata = null;
        this.moved = null;
        this.lock = null;
        this.properties = null;
        this.type = null;
        this.cube = null;
        this.point = null;
        this.thing = null;
        UNUSED.recycle(this);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractThing> ThingRefImpl<T> getUnused() {
        return (ThingRefImpl<T>)UNUSED.get();
    }
}
