package net.venaglia.realms.common.physical.geom;

/**
 * User: ed
 * Date: 8/3/12
 * Time: 7:24 PM
 */
public interface Element<T extends Element<T>> {

    T scale(double magnitude);

    T scale(Vector magnitude);

    T translate(Vector magnitude);

    T rotate(Vector x, Vector y, Vector z);

    T rotate(Axis axis, double angle);

    T transform(XForm xForm);

    T copy();
}
