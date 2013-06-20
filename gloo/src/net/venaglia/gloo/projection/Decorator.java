package net.venaglia.gloo.projection;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 10:42 AM
 */
public interface Decorator {

    boolean isStatic();

    void apply(long nowMS, GeometryBuffer buffer);

}
