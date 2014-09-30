package net.venaglia.gloo.navigation;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
* User: ed
* Date: 4/21/13
* Time: 8:55 AM
*/
public class Position {

    private static final double TWO_PI = Math.PI * 2.0;
    private static final double HALF_PI = Math.PI * 0.5;
    
    public double cameraX, cameraY, cameraZ;
    public double moveX, moveY, moveZ;
    public double heading, pitch, roll;
    public double fov = 1;

    private long cameraHash = 0;
    private final Lock cameraLock = new ReentrantLock();
    private final Matrix_4x4 cameraRotation = Matrix_4x4.identity();
    private final Matrix_4x4 temp = Matrix_4x4.identity();

    public void doMove(double multiple) {
        doMove(moveX, moveY, moveZ, multiple);
    }

    public void doMove(Vector v) {
        doMove(v.i, v.j, v.k, 1);
    }

    public void doMove(double x, double y, double z, double multiple) {
        cameraX += x * multiple;
        cameraY += y * multiple;
        cameraZ += z * multiple;
    }

    public void dollyCamera(double fwd, double right, double up) {
        updateCameraRotation();
        doMove(cameraRotation.product(right, fwd, up, Vector.VECTOR_XFORM_VIEW));
    }

    private void updateCameraRotation() {
        long cameraHash = cameraHash();
        if (this.cameraHash != cameraHash) {
            cameraLock.lock();
            try {
                this.cameraHash = cameraHash;
                cameraRotation.loadIdentity();
                Matrix_4x4.product(cameraRotation, temp.loadRotation(Axis.Y, roll));
                Matrix_4x4.product(cameraRotation, temp.loadRotation(Axis.Z, heading));
                Matrix_4x4.product(cameraRotation, temp.loadRotation(Axis.X, -pitch));
            } finally {
                cameraLock.unlock();
            }
        }
    }

    public void normalize() {
        while (pitch < -Math.PI) {
            pitch += TWO_PI;
        }
        while (pitch >= Math.PI) {
            pitch -= TWO_PI;
        }
        if (pitch > HALF_PI && pitch <= -HALF_PI) {
            heading += Math.PI;
            roll += Math.PI;
            pitch = Math.PI - pitch;
            if (pitch < -Math.PI) {
                pitch += TWO_PI;
            }
        }
        while (heading < 0) {
            heading += TWO_PI;
        }
        while (heading >= TWO_PI) {
            heading -= TWO_PI;
        }
        while (roll < 0) {
            roll += TWO_PI;
        }
        while (roll >= TWO_PI) {
            roll -= TWO_PI;
        }
    }

    public void setPosition(Position that) {
        this.cameraX = that.cameraX;
        this.cameraY = that.cameraY;
        this.cameraZ = that.cameraZ;
        this.moveX = that.moveX;
        this.moveY = that.moveY;
        this.moveZ = that.moveZ;
        this.heading = that.heading;
        this.pitch = that.pitch;
        this.roll = that.roll;
        this.fov = that.fov;
    }

    protected Position copy() {
        Position p = new Position();
        p.setPosition(this);
        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position)o;

        if (Double.compare(position.cameraX, cameraX) != 0) return false;
        if (Double.compare(position.cameraY, cameraY) != 0) return false;
        if (Double.compare(position.cameraZ, cameraZ) != 0) return false;
        if (Double.compare(position.moveX, moveX) != 0) return false;
        if (Double.compare(position.moveY, moveY) != 0) return false;
        if (Double.compare(position.moveZ, moveZ) != 0) return false;
        if (Double.compare(position.heading, heading) != 0) return false;
        if (Double.compare(position.pitch, pitch) != 0) return false;
        if (Double.compare(position.roll, roll) != 0) return false;
        if (Double.compare(position.fov, fov) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(cameraX);
        result = (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(cameraY);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(cameraZ);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(moveX);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(moveY);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(moveZ);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(heading);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(pitch);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(roll);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(fov);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    private long cameraHash() {
        long result;
        long temp;
        temp = Double.doubleToLongBits(cameraX);
        result = temp;
        temp = Double.doubleToLongBits(cameraY);
        result = 31 * result + temp;
        temp = Double.doubleToLongBits(cameraZ);
        result = 31 * result + temp;
        temp = Double.doubleToLongBits(heading);
        result = 31 * result + temp;
        temp = Double.doubleToLongBits(pitch);
        result = 31 * result + temp;
        temp = Double.doubleToLongBits(roll);
        result = 31 * result + temp;
        temp = Double.doubleToLongBits(fov);
        result = 31 * result + temp;
        return result;
    }
}
