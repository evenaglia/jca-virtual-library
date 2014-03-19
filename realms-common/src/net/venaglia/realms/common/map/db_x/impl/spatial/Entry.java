package net.venaglia.realms.common.map.db_x.impl.spatial;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.util.impl.AbstractSpatialMap;
import net.venaglia.realms.common.map.db_x.DB;

/**
 * User: ed
 * Date: 8/5/13
 * Time: 5:54 PM
 */
class Entry<V> implements AbstractSpatialMap.Entry<V> {

    protected final Integer key;
    protected final DB<V> db;

    protected double x;
    protected double y;
    protected double z;

    protected ValueNode node;
    protected boolean highValue;

    public Entry(DB<V> db, ValueNode node, boolean highValue) {
        this.db = db;
        this.node = node;
        this.highValue = highValue;
        if (highValue) {
            this.key = node.index2;
            this.x = node.x2;
            this.y = node.y2;
            this.z = node.z2;
        } else {
            this.key = node.index1;
            this.x = node.x1;
            this.y = node.y1;
            this.z = node.z1;
        }
    }

    Integer getKey() {
        return key;
    }

    public boolean move(Point p) throws IndexOutOfBoundsException, UnsupportedOperationException {
        return move(p.x, p.y, p.z);
    }

    public boolean move(double x, double y, double z) throws IndexOutOfBoundsException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public boolean remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public double getAxis(Axis axis) {
        return axis.of(x, y, z);
    }

    public V get() {
        return db.get(key);
    }
}
