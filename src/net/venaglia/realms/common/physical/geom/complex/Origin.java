package net.venaglia.realms.common.physical.geom.complex;

import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.CompositeShape;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.primitives.Line;
import net.venaglia.realms.common.physical.geom.primitives.Sphere;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.util.Lock;

import java.util.Collections;
import java.util.Iterator;

/**
 * User: ed
 * Date: 9/7/12
 * Time: 9:20 AM
 */
public class Origin implements Shape<Origin> {

    public final Point center;

    private final CompositeShape stuff;
    private final double scale;
    private final Lock lock = Lock.ALWAYS_LOCKED;
    private final Brush brush = new Brush();

    private Transformation transformation;

    public Origin(double magnitude) {
        Sphere sp = new Sphere(DetailLevel.LOW).scale(magnitude * 0.04);
        sp.setMaterial(Material.makeSelfIlluminating(Color.WHITE));
        Line x = new Line(Point.ORIGIN, new Point(1,0,0)).scale(magnitude);
        x.setMaterial(Material.makeSelfIlluminating(Color.RED));
        Line y = new Line(Point.ORIGIN, new Point(0,1,0)).scale(magnitude);
        y.setMaterial(Material.makeSelfIlluminating(Color.GREEN));
        Line z = new Line(Point.ORIGIN, new Point(0,0,1)).scale(magnitude);
        z.setMaterial(Material.makeSelfIlluminating(Color.BLUE));
        this.stuff = new CompositeShape(sp, x, y, z);
        this.center = Point.ORIGIN;
        this.scale = magnitude;
        lock.lock();
        brush.setLighting(false);
        brush.setColor(false);
    }

    public Vector getNormal(int index) {
        return new Vector(1,1,1);
    }

    public Transformation getTransformation() {
        if (transformation == null) {
            transformation = new Transformation(lock);
        }
        return transformation;
    }

    public Lock getLock() {
        return lock;
    }

    public Origin setMaterial(Material material) {
        throw new UnsupportedOperationException();
    }

    public Material getMaterial() {
        throw new UnsupportedOperationException();
    }

    public BoundingVolume<?> getBounds() {
        return BoundingSphere.NULL;
    }

    public Origin scale(double magnitude) {
        return new Origin(this.scale * magnitude);
    }

    public Origin scale(Vector magnitude) {
        throw new UnsupportedOperationException();
    }

    public Origin translate(Vector magnitude) {
        throw new UnsupportedOperationException();
    }

    public Origin rotate(Vector x, Vector y, Vector z) {
        throw new UnsupportedOperationException();
    }

    public Origin rotate(Axis axis, double angle) {
        throw new UnsupportedOperationException();
    }

    public Origin transform(XForm xForm) {
        throw new UnsupportedOperationException();
    }

    public Origin copy() {
        return this;
    }

    public Iterator<Point> iterator() {
        return Collections.singleton(center).iterator();
    }

    public boolean isStatic() {
        return true;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        if (transformation != null) {
            buffer.pushTransform();
            transformation.apply(nowMS, buffer);
            buffer.pushBrush();
            buffer.applyBrush(brush);
            stuff.project(nowMS, buffer);
            buffer.popBrush();
            buffer.popTransform();
        } else {
            buffer.pushBrush();
            buffer.applyBrush(brush);
            stuff.project(nowMS, buffer);
            buffer.popBrush();
        }
    }

    public int size() {
        return 1;
    }
}
