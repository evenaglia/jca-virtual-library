package net.venaglia.realms.common.map_x.db;

/**
 * User: ed
 * Date: 2/23/13
 * Time: 12:30 PM
 */
public class DBException extends RuntimeException {

    public DBException() {
    }

    public DBException(String s) {
        super(s);
    }

    public DBException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DBException(Throwable throwable) {
        super(throwable);
    }
}
