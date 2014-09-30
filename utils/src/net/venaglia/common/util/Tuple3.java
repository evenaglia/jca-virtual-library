package net.venaglia.common.util;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 9:20 PM
 */
public interface Tuple3<A,B,C> {

    A getA();

    B getB();

    C getC();

    Tuple3<C,B,A> reverse();
}
