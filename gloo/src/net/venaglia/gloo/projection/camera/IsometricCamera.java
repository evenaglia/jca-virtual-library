package net.venaglia.gloo.projection.camera;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;

/**
 * User: ed
 * Date: 5/29/13
 * Time: 8:50 AM
 */
public class IsometricCamera extends OrthagonalCamera {

    public static final double HEADING_POS_X_POS_Y = Math.PI * 0.25;
    public static final double HEADING_POS_X_NEG_Y = Math.PI * 0.75;
    public static final double HEADING_NEG_X_NEG_Y = Math.PI * 1.25;
    public static final double HEADING_NEG_X_POS_Y = Math.PI * 1.75;

    public static final double PITCH_DOWN_30 = Math.PI * 0.166666667;
    public static final double PITCH_DOWN_60 = Math.PI * 0.333333333;

    private double heading;
    private double pitch;
    private double scale;

    /**
     * Creates a new Isometric camera, with a default heading pitch and scale.<br/>
     * Default values are:
     * <ul>
     *     <li><strong>position</strong>: {@link Point#ORIGIN} = (0,0,0)</li>
     *     <li><strong>heading</strong>: {@link #HEADING_POS_X_POS_Y} = &pi; / 4</li>
     *     <li><strong>pitch</strong>: {@link #PITCH_DOWN_30} = &pi; / 6</li>
     *     <li><strong>scale</strong>: 1.0</li>
     * </ul>
     */
    public IsometricCamera() {
        this(Point.ORIGIN, IsometricCamera.HEADING_POS_X_POS_Y, IsometricCamera.PITCH_DOWN_30, 1);
    }

    /**
     * Creates a new Isometric camera, with the specified heading pitch and scale.
     * @param heading The direction, in radians, the camera is pointing. Use one
     *                of HEADING constants for a standard isometric view.
     * @param pitch The pitch of the camera. Positive values look down from
     *              above the z-plane, while negative values look up from below
     *              the z-plane.
     * @param scale The scale to draw x/y coordinates. Larger values show more,
     *              smaller values show less.
     * @throws IllegalArgumentException if any passed value is not a real
     *                                  number, if pitch is zero, or scale is
     *                                  not a positive number.
     */
    public IsometricCamera(double heading, double pitch, double scale) {
    }

    /**
     * Creates a new Isometric camera, with the specified heading pitch and scale.
     * @param heading The direction, in radians, the camera is pointing. Use one
     *                of HEADING constants for a standard isometric view.
     * @param pitch The pitch of the camera. Positive values look down from
     *              above the z-plane, while negative values look up from below
     *              the z-plane.
     * @param scale The scale to draw x/y coordinates. Larger values show more,
     *              smaller values show less.
     * @throws IllegalArgumentException if any passed value is not a real
     *                                  number, if pitch is zero, or scale is
     *                                  not a positive number.
     */
    public IsometricCamera(Point position, double heading, double pitch, double scale) {
        if (position == null) {
            throw new NullPointerException("position");
        }
        if (Double.isNaN(heading) || Double.isInfinite(heading)) {
            throw new IllegalArgumentException("heading must be a real number");
        }
        if (Double.isNaN(pitch) || Double.isInfinite(pitch)) {
            throw new IllegalArgumentException("pitch must be a real number");
        }
        if (pitch == 0) {
            throw new IllegalArgumentException("pitch cannot be zero");
        }
        if (Double.isNaN(scale) || Double.isInfinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("scale must be a positive number");
        }
        this.position = position;
        this.heading = heading;
        this.pitch = pitch;
        this.scale = scale;
        computeImpl();
    }

    protected IsometricCamera(IsometricCamera that) {
        super(that);
        this.heading = that.heading;
        this.pitch = that.pitch;
        this.scale = that.scale;
    }

    @Override
    public IsometricCamera copy() {
        return new IsometricCamera(this);
    }

    @Override
    protected void computeImpl() {
        double t = Math.cos(pitch);
        double sin = Math.sin(heading);
        double cos = Math.cos(heading);
        this.direction = new Vector(sin * t * scale, cos * t * scale, Math.sin(pitch) * scale);
        this.right = new Vector(cos * scale, -sin * scale, 0).reverse();
        super.computeImpl();
    }

    @Override
    public void setDirection(Vector direction) {
        // todo: derive heading and pitch
    }

    @Override
    public void setRight(Vector right) {
        setScale(right.l);
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        if (this.heading != heading) {
            if (Double.isNaN(heading) || Double.isInfinite(heading)) {
                throw new IllegalArgumentException("heading must be a real number");
            }
            this.heading = heading;
            modCount++;
        }
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        if (this.pitch != pitch) {
            if (Double.isNaN(pitch) || Double.isInfinite(pitch)) {
                throw new IllegalArgumentException("pitch must be a real number");
            }
            if (pitch == 0) {
                throw new IllegalArgumentException("pitch cannot be zero");
            }
            this.pitch = pitch;
            modCount++;
        }
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        if (this.scale != scale) {
            if (Double.isNaN(scale) || Double.isInfinite(scale) || scale <= 0) {
                throw new IllegalArgumentException("scale must be a positive number");
            }
            this.scale = scale;
            modCount++;
        }
    }
}
