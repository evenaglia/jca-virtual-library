package net.venaglia.realms.common.physical.geom;

import net.venaglia.realms.common.physical.bounds.BoundingBox;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;
import net.venaglia.realms.common.util.Lock;
import net.venaglia.realms.common.util.impl.ThreadSafeLock;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * User: ed
 * Date: 9/1/12
 * Time: 4:52 PM
 */
public class CompositeShape implements Shape<CompositeShape>, Projectable {

    protected final NavigableMap<Integer,Shape<?>> shapes;
    protected final Lock lock = new ThreadSafeLock();

    protected Transformation transformation;
    protected Material material = Material.INHERIT;
    protected boolean allAreStatic = true;
    protected int total = 0;

    protected BoundingBox box = BoundingBox.NULL;
    protected double[] bounds = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                                 Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE};

    public CompositeShape() {
        shapes = new TreeMap<Integer,Shape<?>>();
    }

    public CompositeShape(Shape... shapes) {
        this.shapes = new TreeMap<Integer,Shape<?>>();
        addShapes(shapes);
    }

    private CompositeShape(NavigableMap<Integer,Shape<?>> shapes, Transformation transformation, Material material) {
        this.shapes = shapes;
        if (transformation != null) getTransformation().transform(transformation);
        this.material = material;
    }

    public Vector getNormal(int index) {
        Map.Entry<Integer,Shape<?>> entry = shapes.floorEntry(index);
        int i = index - entry.getKey();
        Shape<?> shape = entry.getValue();
        if (i >= shape.size()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return shape.getNormal(i);
    }

    public int size() {
        return total;
    }

    public Collection<Shape<?>> getShapes() {
        return Collections.unmodifiableCollection(shapes.values());
    }

    public void addShape(Shape<?> shape) {
        lock.assertUnlocked();
        int count = shape.size();
        if (count <= 0) {
            throw new IllegalArgumentException("passed shape has no size: " + count);
        }
        allAreStatic = allAreStatic && shape.isStatic();
        shapes.put(total, shape);
        adjustBounds(shape.getBounds().asBox());
        total += count;
    }

    public void addShapes(Shape... shapes) {
        lock.assertUnlocked();
        boolean hasNonStatic = false;
        for (int i = 0, l = shapes.length; i < l; i++) {
            Shape shape = shapes[i];
            int count = shape.size();
            if (count <= 0) {
                throw new IllegalArgumentException("passed shape[" + i + "] has no size: " + count);
            }
            hasNonStatic = hasNonStatic || !shape.isStatic();
        }
        allAreStatic = allAreStatic && !hasNonStatic;
        for (Shape shape : shapes) {
            int count = shape.size();
            this.shapes.put(total, shape);
            adjustBounds(shape.getBounds().asBox());
            total += count;
        }
    }

    private void adjustBounds(BoundingBox box) {
        if (box.corner1.x < bounds[0] || box.corner1.y < bounds[1] || box.corner1.z < bounds[2] ||
            box.corner2.x > bounds[3] || box.corner2.y > bounds[4] || box.corner2.z > bounds[5]) {
            this.box = null;
            bounds[0] = Math.min(box.corner1.x, bounds[0]);
            bounds[1] = Math.min(box.corner1.y, bounds[1]);
            bounds[2] = Math.min(box.corner1.z, bounds[2]);
            bounds[3] = Math.max(box.corner2.x, bounds[3]);
            bounds[4] = Math.max(box.corner2.y, bounds[4]);
            bounds[5] = Math.max(box.corner2.z, bounds[5]);
        }
    }

    public BoundingVolume<?> getBounds() {
        if (box == null) {
            box = new BoundingBox(new Point(bounds[0], bounds[1], bounds[2]),
                                  new Point(bounds[3], bounds[4], bounds[5]));
        }
        return box;
    }

    public CompositeShape scale(double magnitude) {
        if (magnitude == 0.0) {
            throw new IllegalArgumentException();
        }
        NavigableMap<Integer,Shape<?>> shapes = new TreeMap<Integer,Shape<?>>(this.shapes);
        for (Map.Entry<Integer,Shape<?>> entry : shapes.entrySet()) {
            entry.setValue(entry.getValue().scale(magnitude));
        }
        return new CompositeShape(shapes, transformation, material);
    }

    public CompositeShape scale(Vector magnitude) {
        if (magnitude.l == 0.0) {
            throw new IllegalArgumentException();
        }
        NavigableMap<Integer,Shape<?>> shapes = new TreeMap<Integer,Shape<?>>(this.shapes);
        for (Map.Entry<Integer,Shape<?>> entry : shapes.entrySet()) {
            entry.setValue(entry.getValue().scale(magnitude));
        }
        return new CompositeShape(shapes, transformation, material);
    }

    public CompositeShape translate(Vector magnitude) {
        NavigableMap<Integer,Shape<?>> shapes = new TreeMap<Integer,Shape<?>>(this.shapes);
        for (Map.Entry<Integer,Shape<?>> entry : shapes.entrySet()) {
            entry.setValue(entry.getValue().scale(magnitude));
        }
        return new CompositeShape(shapes, transformation, material);
    }

    public CompositeShape rotate(Vector x, Vector y, Vector z) {
        NavigableMap<Integer,Shape<?>> shapes = new TreeMap<Integer,Shape<?>>(this.shapes);
        for (Map.Entry<Integer,Shape<?>> entry : shapes.entrySet()) {
            entry.setValue(entry.getValue().rotate(x, y, z));
        }
        return new CompositeShape(shapes, transformation, material);
    }

    public CompositeShape rotate(Axis axis, double angle) {
        NavigableMap<Integer,Shape<?>> shapes = new TreeMap<Integer,Shape<?>>(this.shapes);
        for (Map.Entry<Integer,Shape<?>> entry : shapes.entrySet()) {
            entry.setValue(entry.getValue().rotate(axis, angle));
        }
        return new CompositeShape(shapes, transformation, material);
    }

    public CompositeShape transform(XForm xForm) {
        NavigableMap<Integer,Shape<?>> shapes = new TreeMap<Integer,Shape<?>>(this.shapes);
        for (Map.Entry<Integer,Shape<?>> entry : shapes.entrySet()) {
            entry.setValue(entry.getValue().transform(xForm));
        }
        return new CompositeShape(shapes, transformation, material);
    }

    public CompositeShape copy() {
        NavigableMap<Integer,Shape<?>> shapes = new TreeMap<Integer,Shape<?>>(this.shapes);
        return new CompositeShape(shapes, transformation, material);
    }

    public Iterator<Point> iterator() {
        return new Iterator<Point>() {

            private final Iterator<Shape<?>> shapeIter = shapes.values().iterator();

            private Iterator<Point> pointIter = null;

            public boolean hasNext() {
                return pointIter != null && pointIter.hasNext() || shapeIter.hasNext();
            }

            public Point next() {
                if (pointIter == null || !pointIter.hasNext()) {
                    pointIter = shapeIter.next().iterator();
                }
                return pointIter.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public final boolean isStatic() {
        return allAreStatic;
    }

    public Transformation getTransformation() {
        if (transformation == null) {
            transformation = new Transformation(lock);
        }
        return transformation;
    }

    public CompositeShape setMaterial(Material material) {
        this.material = material;
        return this;
    }

    public Material getMaterial() {
        return material;
    }

    public Lock getLock() {
        return lock;
    }

    public void inheritMaterialToContainedShapes() {
        for (Shape<?> shape : shapes.values()) {
            shape.setMaterial(Material.INHERIT);
        }
    }

    public final void project(long nowMS, GeometryBuffer buffer) {
        if (transformation == null) {
            material.apply(nowMS, buffer);
            for (Shape<?> shape : shapes.values()) {
                shape.project(nowMS, buffer);
            }
        } else {
            buffer.pushTransform();
            transformation.apply(nowMS, buffer);
            material.apply(nowMS, buffer);
            for (Shape<?> shape : shapes.values()) {
                shape.project(nowMS, buffer);
            }
            buffer.popTransform();
        }
    }
}
