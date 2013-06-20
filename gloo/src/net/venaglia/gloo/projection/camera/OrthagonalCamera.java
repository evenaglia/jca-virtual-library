package net.venaglia.gloo.projection.camera;

import net.venaglia.gloo.projection.Camera;

/**
 * User: ed
 * Date: 5/27/13
 * Time: 11:05 PM
 */
public class OrthagonalCamera extends Camera {

    public OrthagonalCamera() {
    }

    protected OrthagonalCamera(OrthagonalCamera that) {
        super(that);
    }

    @Override
    public OrthagonalCamera copy() {
        return new OrthagonalCamera(this);
    }

    @Override
    public double getViewAngle() {
        return 0.0;
    }

    @Override
    public boolean isOrthogonal() {
        return true;
    }
}
