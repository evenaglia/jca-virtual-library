package net.venaglia.gloo.physical.bounds;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;

/**
* User: ed
* Date: 9/6/14
* Time: 2:14 AM
*/
public class MutableSimpleBounds implements SimpleBoundingVolume {

    private final boolean spherical;
    private final double r, r2;

    private boolean loaded = false;
    private double x0, x_, x1, y0, y_, y1, z0, z_, z1;

    public MutableSimpleBounds() {
        this(false, 0.00005);
    }

    public MutableSimpleBounds(boolean spherical, double radius) {
        if (Double.isNaN(radius)) {
            throw new IllegalArgumentException("radius is Nan");
        }
        if (radius < 0) {
            throw new IllegalArgumentException("radius cannot be negative: " + radius);
        }
        this.spherical = spherical;
        this.r = radius;
        this.r2 = radius * radius;
    }

    public MutableSimpleBounds load(Point p) {
        return load(p.x, p.y, p.z);
    }

    public MutableSimpleBounds load(double x, double y, double z) {
        x0 = x - r;
        x_ = x;
        x1 = x + r;
        y0 = y - r;
        y_ = y;
        y1 = y + r;
        z0 = z - r;
        z_ = z;
        z1 = z + r;
        loaded = true;
        return this;
    }

    public Point getCenterPoint() {
        enureLoaded();
        return new Point(x_, y_, z_);
    }

    public double getCenterX() {
        enureLoaded();
        return x_;
    }

    public double getCenterY() {
        enureLoaded();
        return y_;
    }

    public double getCenterZ() {
        enureLoaded();
        return z_;
    }

    public double min(Axis axis) {
        enureLoaded();
        return axis.of(x0, y0, z0);
    }

    public double max(Axis axis) {
        enureLoaded();
        return axis.of(x1, y1, z1);
    }

    public boolean includes(Point p) {
        enureLoaded();
        return includes(p.x, p.y, p.z);
    }

    public boolean includes(double x, double y, double z) {
        enureLoaded();
        if (spherical) {
            return (x - x_) * (x - x_) + (y - y_) * (y - y_) + (z - z_) * (z - z_) <= r2;
        } else {
            return x >= x0 && x < x1 && y >= y0 && y < y1 && z >= z0 && z < z1;
        }
    }

    private void enureLoaded() {
        if (!loaded) {
            throw new IllegalStateException("MutableSimpleBounds is not loaded yet");
        }
    }
}
