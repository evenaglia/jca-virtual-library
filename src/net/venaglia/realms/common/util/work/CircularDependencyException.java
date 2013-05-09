package net.venaglia.realms.common.util.work;

/**
 * User: ed
 * Date: 1/23/13
 * Time: 6:06 PM
 */
public class CircularDependencyException extends RuntimeException {

    public CircularDependencyException() {
    }

    public CircularDependencyException(String s) {
        super(s);
    }
}
