package net.venaglia.gloo.projection;

/**
 * User: ed
 * Date: 7/17/12
 * Time: 9:26 PM
 *
 * An object that can be represented as one or more shapes in 3D space.
 */
public interface Projectable {

    Projectable NULL = new Projectable() {
        public boolean isStatic() {
            return true;
        }

        public void project(long nowMS, GeometryBuffer buffer) {
            // no-op
        }
    };

    boolean isStatic();

    void project(long nowMS, GeometryBuffer buffer);
}
