package net.venaglia.realms.common.map_x.db;

/**
 * User: ed
 * Date: 2/23/13
 * Time: 12:38 PM
 */
public class NotWritableException extends DBException {

    public NotWritableException() {
    }

    public NotWritableException(String s) {
        super(s);
    }

    public NotWritableException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NotWritableException(Throwable throwable) {
        super(throwable);
    }
}
