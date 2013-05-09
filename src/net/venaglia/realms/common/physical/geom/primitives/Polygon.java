package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.geom.FlippableShape;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.projection.GeometryBuffer;

import java.util.Arrays;
import java.util.Collections;

public final class Polygon extends AbstractShape<Polygon> implements FlippableShape<Polygon> {

    private final Vector normal;

    public Polygon(Vector normal, Point... points) {
        super(assertMinLength(points, 3));
        this.normal = normal;
    }

    @Override
    protected Polygon build(Point[] points, XForm xForm) {
        return new Polygon(xForm.apply(normal), points);
    }

    public Polygon flip() {
        Point[] points = this.points.clone();
        Collections.reverse(Arrays.asList(points));
        return new Polygon(normal.reverse(), points);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.POLYGON);
        buffer.normal(normal);
        for (Point point : points) {
            buffer.vertex(point);
        }
        buffer.end();
    }

    public Vector getNormal(int index) {
        return normal;
    }
}
