package net.venaglia.realms.common.navigation;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 8:14 AM
 *
 * Defines the navigable space occupied by a user.
 */
public class UserSpace {

    public final double x;
    public final double y;
    public final double r;

    public UserSpace(double x, double y, double r) {
        this.x = x;
        this.y = y;
        this.r = Math.abs(r);
    }

    public double minX() {
        return x - r;
    }

    public double maxX() {
        return x + r;
    }

    public double minY() {
        return y - r;
    }

    public double maxY() {
        return y + r;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserSpace userSpace = (UserSpace)o;

        if (Double.compare(userSpace.r, r) != 0) return false;
        if (Double.compare(userSpace.x, x) != 0) return false;
        if (Double.compare(userSpace.y, y) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(r);
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "UserSpace{" +
                "x=" + x +
                ", y=" + y +
                ", r=" + r +
                '}';
    }
}
