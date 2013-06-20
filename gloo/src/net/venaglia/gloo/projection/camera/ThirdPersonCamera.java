package net.venaglia.gloo.projection.camera;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.util.matrix.Matrix_1x4;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

/**
 * User: ed
 * Date: 3/30/13
 * Time: 7:53 PM
 */
public class ThirdPersonCamera extends PerspectiveCamera {

    public static final double DO_NOT_FOLLOW = -1.0;

    protected Point followPosition;
    protected Vector followDirection;
    protected Vector followRight;
    protected Vector followUp;

    protected double followDistance = -1.0;

    protected double followAngle = 0.0;

    protected double followElevation = Math.PI / 6.0;

    public ThirdPersonCamera() {
    }

    protected ThirdPersonCamera(ThirdPersonCamera that) {
        super(that);
        this.followPosition = that.followPosition;
        this.followDirection = that.followDirection;
        this.followRight = that.followRight;
        this.followUp = that.followUp;
        this.followDistance = that.followDistance;
        this.followAngle = that.followAngle;
        this.followElevation = that.followElevation;
    }

    public double getFollowDistance() {
        return followDistance;
    }

    public void setFollowDistance(double followDistance) {
        if (this.followDistance != followDistance) {
            this.followDistance = followDistance;
            modCount++;
        }
    }

    public double getFollowAngle() {
        return followAngle;
    }

    public void setFollowAngle(double followAngle) {
        if (this.followAngle != followAngle) {
            if (followAngle < -1.5 || followAngle > 1.5) {
                throw new IllegalArgumentException("Follow angle is not behind the camera, angle must be -1.5 <= n <= 1.5: " + followAngle);
            }
            this.followAngle = followAngle;
            modCount++;
        }
    }

    public double getFollowElevation() {
        return followElevation;
    }

    public void setFollowElevation(double followElevation) {
        if (this.followElevation != followElevation) {
            if (followElevation < 0 || followElevation > 1.5) {
                throw new IllegalArgumentException("Follow elevation is out of range, elevation must be 0.0 <= n <= 1.5: " + followElevation);
            }
            this.followElevation = followElevation;
            modCount++;
        }
    }

    @Override
    public Point getPosition() {
        computeIfNeeded();
        return followPosition;
    }

    @Override
    public Vector getDirection() {
        computeIfNeeded();
        return followDirection;
    }

    @Override
    public Vector getRight() {
        computeIfNeeded();
        return followRight;
    }

    @Override
    public Vector getUp() {
        computeIfNeeded();
        return followUp;
    }

    @Override
    protected void computeImpl() {
        super.computeImpl();
        if (followDistance < 0.0) {
            followPosition = position;
            followDirection = direction;
            followRight = right;
            followUp = up;
        } else {
            // vector pointing from player's head to camera position
            double a = -Math.sin(followAngle);
            double b = -Math.cos(followAngle) * Math.cos(followElevation);
            double c = Math.sin(followElevation);

            // cross-product and scale, used for camera's "up" value
            double i = a * c - c * b;
            double j = c * a - b * -c;
            double k = b * -b - a * a;
            double s = 1.0 / Vector.computeDistance(i, j, k);

            // compute new camera properties
            Matrix_4x4 rotate = Matrix_4x4.rotate(right.normalize(), direction.normalize(), up.normalize());
            followPosition = new Point(a * followDistance + position.x,
                                       b * followDistance + position.y,
                                       c * followDistance + position.z);
            followDirection = rotate.product(-a, -b, -c, Matrix_1x4.View.VECTOR);
            followRight = rotate.product(b, -a, -c, Matrix_1x4.View.VECTOR);
            followUp = rotate.product(i * s, j * s, k * s, Matrix_1x4.View.VECTOR);
        }
    }

    @Override
    public ThirdPersonCamera copy() {
        return new ThirdPersonCamera(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ThirdPersonCamera that = (ThirdPersonCamera)o;

        if (Double.compare(that.followAngle, followAngle) != 0) return false;
        if (Double.compare(that.followDistance, followDistance) != 0) return false;
        if (Double.compare(that.followElevation, followElevation) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = followDistance != +0.0d ? Double.doubleToLongBits(followDistance) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = followAngle != +0.0d ? Double.doubleToLongBits(followAngle) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = followElevation != +0.0d ? Double.doubleToLongBits(followElevation) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        int angle = (int)(Math.round(followAngle * 180.0 / Math.PI) % 360L);
        int elevation = (int)(Math.round(followElevation * 180.0 / Math.PI) % 360L);
        return String.format("%s<-followed@(|d|=%.2f,\u03B1=%d\u00B0,\u03B8=%d°)",
                             super.toString(), followDistance, angle, elevation);
    }
}
