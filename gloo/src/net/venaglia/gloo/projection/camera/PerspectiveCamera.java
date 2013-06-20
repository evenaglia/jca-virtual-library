package net.venaglia.gloo.projection.camera;

import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.Camera;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 1:25 PM
 */
public class PerspectiveCamera extends Camera {

    protected double angle = 0.0;

    public PerspectiveCamera() {
    }

    protected PerspectiveCamera(PerspectiveCamera that) {
        super(that);
        this.angle = that.angle;
    }

    public double getViewAngle() {
        computeIfNeeded();
        return angle;
    }

    public Vector getUp() {
        computeIfNeeded();
        return up;
    }

    @Override
    protected void computeImpl() {
        super.computeImpl();
        angle = direction.angle(direction.sum(right));
    }

    @Override
    public PerspectiveCamera copy() {
        return new PerspectiveCamera(this);
    }

    public boolean isOrthogonal() {
        return false;
    }
}
