package net.venaglia.realms.common.projection;

import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 1:25 PM
 */
public class Camera {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private int id = SEQUENCE.getAndIncrement();

    protected int modCount = 0;

    /**
     * The point at which the camera looks from.
     */
    protected Point position = new Point(0.0,-4.0,0.0);

    /**
     * The direction in which the camera is looking. The magnitude of this
     * vector determines zoom factor.
     */
    protected Vector direction = new Vector(0.0,0.0,1.5);

    /**
     * A vector that points to the right edge of the view port. If this vector
     * and the direction vector and added to the position of the camera, the
     * resulting point will appear on the right center of the view port.
     */
    protected Vector right = new Vector(0.0,0.5,0.0);

    protected float nearClippingDistance = 0.1f;

    protected float farClippingDistance = 1.0f;

    protected double angle = 0.0;
    protected Vector up = null;
    protected boolean orthagonal = false;
    protected int computedModCount = -1;

    public Camera() {
    }

    public int getId() {
        return id;
    }

    public int getModCount() {
        return modCount;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        if (!this.position.equals(position)) {
            modCount++;
            this.position = position;
        }
    }

    public Vector getDirection() {
        return direction;
    }

    public void setDirection(Vector direction) {
        if (direction == null) {
            throw new NullPointerException("direction");
        }
        if (direction.l == 0.0) {
            throw new IllegalArgumentException("direction cannot have a magnitude of 0: " + right);
        }
        if (!this.direction.equals(direction)) {
            modCount++;
            this.direction = direction;
        }
    }

    public Vector getRight() {
        return right;
    }

    public void setRight(Vector right) {
        if (right == null) {
            throw new NullPointerException("right");
        }
        if (right.l == 0.0) {
            throw new IllegalArgumentException("right cannot have a magnitude of 0: " + right);
        }
        if (!this.right.equals(right)) {
            modCount++;
            this.right = right;
        }
    }

    public float getNearClippingDistance() {
        return nearClippingDistance;
    }

    public void setClippingDistance(float nearClippingDistance, float farClippingDistance) {
        if (nearClippingDistance <= 0.0f) {
            throw new IllegalArgumentException("nearClippingDistance must be greater than 0: " + nearClippingDistance);
        }
        if (this.nearClippingDistance != nearClippingDistance) {
            modCount++;
            this.nearClippingDistance = nearClippingDistance;
        }
        if (farClippingDistance <= 0.0f) {
            throw new IllegalArgumentException("farClippingDistance must be greater than 0: " + farClippingDistance);
        }
        if (this.farClippingDistance != farClippingDistance) {
            modCount++;
            this.farClippingDistance = farClippingDistance;
        }
    }

    public float getFarClippingDistance() {
        return farClippingDistance;
    }

    public double getViewAngle() {
        computeIfNeeded();
        return angle;
    }

    public Vector getUp() {
        computeIfNeeded();
        return up;
    }

    public boolean isOrthagonal() {
        return orthagonal;
    }

    public void setOrthagonal(boolean orthagonal) {
        if (this.orthagonal != orthagonal) {
            modCount++;
            this.orthagonal = orthagonal;
        }
    }

    protected void computeIfNeeded() {
        if (modCount != computedModCount) {
            computeImpl();
            computedModCount = modCount;
        }
    }

    protected void computeImpl() {
        up = direction.cross(right).normalize(right.l);
        angle = direction.angle(direction.sum(right));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Camera camera = (Camera)o;

        if (Double.compare(camera.farClippingDistance, farClippingDistance) != 0) return false;
        if (Double.compare(camera.nearClippingDistance, nearClippingDistance) != 0) return false;
        if (!direction.equals(camera.direction)) return false;
        if (!position.equals(camera.position)) return false;
        if (!right.equals(camera.right)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = position.hashCode();
        result = 31 * result + direction.hashCode();
        result = 31 * result + right.hashCode();
        temp = Double.doubleToLongBits(nearClippingDistance);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(farClippingDistance);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("camera[%.2f,%.2f,%.2f]->[%.2f,%.2f,%.2f]-^[%.2f,%.2f,%.2f]--(%.2f -> %.2f)",
                             position.x, position.y, position.z,
                             direction.i, direction.j, direction.k,
                             right.i, right.j, right.k,
                             nearClippingDistance, farClippingDistance);
    }

    public void computeClippingDistances(BoundingVolume<?> bounds) {
        if (bounds.isInfinite()) {
            throw new IllegalArgumentException("Passed bounding volume must be finite");
        }
        if (bounds.isNull()) {
            throw new IllegalArgumentException("Passed bounding volume is null");
        }
        if (bounds.includes(position)) {
            throw new IllegalArgumentException("Camera position lies inside the passed bounding volume");
        }
        BoundingSphere sphere = bounds.asSphere();
        double d = sphere.center.computeDistance(getPosition());
        nearClippingDistance = (float)(d - sphere.radius);
        farClippingDistance = (float)(d + sphere.radius);
    }
}
