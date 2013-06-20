package net.venaglia.realms.common.map.db;

/**
 * User: ed
 * Date: 2/23/13
 * Time: 12:38 PM
 */
public class DuplicateKeyException extends DBException {

    public DuplicateKeyException() {
    }

    public DuplicateKeyException(String s) {
        super(s);
    }

    public DuplicateKeyException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DuplicateKeyException(Throwable throwable) {
        super(throwable);
    }
}
