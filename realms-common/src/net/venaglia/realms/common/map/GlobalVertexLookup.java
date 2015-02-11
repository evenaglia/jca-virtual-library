package net.venaglia.realms.common.map;

import net.venaglia.common.util.IntLookup;
import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.realms.spec.GeoSpec;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 2/6/15
 * Time: 8:39 AM
 */
public class GlobalVertexLookup implements IntLookup<Point> {

    private final double[] vertices;
    private final int size;

    public GlobalVertexLookup(VertexStore vertexStore) {
        int globalPointCount = GeoSpec.POINTS_SHARED_MANY_ZONE.iGet();
        this.vertices = new double[globalPointCount * 3 + 3];
        RangeBasedLongSet ids = new RangeBasedLongSet();
        ids.addAll(0, globalPointCount);
        AtomicInteger size = new AtomicInteger();
        System.out.println("Caching vertices...");
        vertexStore.read(ids, new VertexStore.VertexConsumer() {

            private int count = 0;
            private int index = 0;

            @Override
            public void next(int rgbColor, double i, double j, double k, float elevation) {
                if (count++ >= 16384) {
                    count -= 16384;
                    System.out.print(".");
                }
                vertices[index++] = i;
                vertices[index++] = j;
                vertices[index++] = k;
            }

            @Override
            public void done() {
                size.set(index / 3);
            }
        });
        System.out.println();
        this.size = size.get();
    }

    @Override
    public Point get(int index) {
        int i = index * 3;
        return new Point(vertices[i++], vertices[i++], vertices[i]);
    }

    public <V> V get(int index, XForm.View<V> view) {
        int i = index * 3;
        return view.convert(vertices[i++], vertices[i++], vertices[i], 1);
    }

    public int size() {
        return size;
    }
}
