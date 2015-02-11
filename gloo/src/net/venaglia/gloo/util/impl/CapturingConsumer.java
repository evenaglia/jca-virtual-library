package net.venaglia.gloo.util.impl;

import net.venaglia.common.util.Ref;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.util.BasicSpatialMap;

/**
 * User: ed
 * Date: 2/5/15
 * Time: 10:07 PM
 */
public class CapturingConsumer<E> implements Ref<E> {

    private int count = 0;
    private E value;
    private double x, y, z;

    private final BasicSpatialMap.BasicConsumer<E> consumer = new BasicSpatialMap.BasicConsumer<E>() {
        @Override
        public void found(BasicSpatialMap.BasicEntry<E> entry, double x, double y, double z) {
            if (count++ == 0) {
                value = entry.get();
                CapturingConsumer.this.x = x;
                CapturingConsumer.this.y = y;
                CapturingConsumer.this.z = z;
            }
        }
    };

    public BasicSpatialMap.BasicConsumer<E> use() {
        count = 0;
        value = null;
        return consumer;
    }

    public boolean found() {
        return count > 0;
    }

    public int count() {
        return count;
    }

    public E get() {
        return value;
    }

    public Point getPoint() {
        return new Point(x, y, z);
    }

    public <V> V getPoint(XForm.View<V> view) {
        return view.convert(x, y, z, 1);
    }
}
