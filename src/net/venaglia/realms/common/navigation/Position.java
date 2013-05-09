package net.venaglia.realms.common.navigation;

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

    public void doMove(double multiple) {
        cameraX += moveX * multiple;
        cameraY += moveY * multiple;
        cameraZ += moveZ * multiple;
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
        return result;
    }
}
