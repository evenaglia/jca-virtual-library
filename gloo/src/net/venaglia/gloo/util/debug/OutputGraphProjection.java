package net.venaglia.gloo.util.debug;

/**
 * User: ed
 * Date: 8/15/14
 * Time: 5:39 PM
 */
public interface OutputGraphProjection<P> {

    /**
     * Transforms the passed object, representing a point, into x/y coordinates,
     * then writes those coordinates to <code>output</code> at
     * <code>offset</code> and <code>offset+1</code>, respectively.
     * @param point The point to project in to x/y coordinates
     * @param output The buffer to hold the projected x/y coordinates
     * @param offset The offset where to write x/y coordinates
     */
    void project(P point, double[] output, int offset);
}
