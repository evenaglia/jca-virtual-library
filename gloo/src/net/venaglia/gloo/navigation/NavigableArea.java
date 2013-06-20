package net.venaglia.gloo.navigation;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 8:08 AM
 */
public class NavigableArea {

    protected double[] xCoords;
    protected double[] yCoords;
    protected int l;
    protected double minX, minY, maxX, maxY;
    protected double z;

    public NavigableArea(double[] xCoords, double[] yCoords, double z) {
        if (xCoords.length != yCoords.length) {
            throw new IllegalArgumentException("Number of x and y coordinates do not match: " +
                                               "xCoords.length = " + xCoords.length + ", " +
                                               "yCoords.length = " + yCoords.length);
        } else if (xCoords.length < 3) {
            throw new IllegalArgumentException("Too few coordinates to define a NavigableArea: " + xCoords.length);
        }
        this.xCoords = xCoords;
        this.yCoords = yCoords;
        this.l = xCoords.length;
        minX = maxX = xCoords[0];
        minY = maxY = yCoords[0];
        for (int i = 1; i < l; i++) {
            minX = Math.min(minX, xCoords[i]);
            minY = Math.min(minY, yCoords[i]);
            maxX = Math.max(maxX, xCoords[i]);
            maxY = Math.max(maxY, yCoords[i]);
        }
        this.z = z;
    }

    public boolean crosses(UserSpace userSpace) {
        if (userSpace.minX() < minX || userSpace.maxX() > maxX || userSpace.minY() < minY || userSpace.maxY() > maxY) {
            return false;
        }
//        for
        return true;
    }

    public double getZ(UserSpace userSpace) {
        return z;
    }
}
