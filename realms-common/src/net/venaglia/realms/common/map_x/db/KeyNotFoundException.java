package net.venaglia.realms.common.map_x.db;

/**
 * User: ed
 * Date: 2/23/13
 * Time: 12:38 PM
 */
public class KeyNotFoundException extends DBException {

    public KeyNotFoundException() {
    }

    public KeyNotFoundException(String s) {
        super(s);
    }

    public KeyNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public KeyNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
