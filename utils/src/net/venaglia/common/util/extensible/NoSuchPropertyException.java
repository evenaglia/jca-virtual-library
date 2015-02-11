package net.venaglia.common.util.extensible;

/**
 * User: ed
 * Date: 1/26/15
 * Time: 8:49 AM
 */
public class NoSuchPropertyException extends RuntimeException {

    private final ExtendedPropertyKey<?> key;

    public NoSuchPropertyException(ExtendedPropertyKey<?> key) {
        super(key == null ? "[unknown property]" : key.getName());
        this.key = key;
    }

    public NoSuchPropertyException(ExtendedPropertyKey<?> key,
                                   Throwable cause) {
        super(key == null ? "[unknown property]" : key.getName(), cause);
        this.key = key;
    }
}
