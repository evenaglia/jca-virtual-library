package net.venaglia.common.util;

/**
 * User: ed
 * Date: 3/20/14
 * Time: 10:52 PM
 */
public interface UncaughtExceptionVisitor<E,FAIL extends Throwable> {

    void visit(E value) throws FAIL;
}
