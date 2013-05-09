package net.venaglia.realms.common.util;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 9:20 PM
 */
public interface Tuple2<A,B> {

    A getA();

    B getB();

    Tuple2<B,A> reverse();
}
