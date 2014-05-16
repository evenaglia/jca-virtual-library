package net.venaglia.common.util.serializer;

/**
 * User: ed
 * Date: 4/3/14
 * Time: 6:13 PM
 */
public class SerializerException extends RuntimeException {

    public SerializerException() {
    }

    public SerializerException(String message) {
        super(message);
    }

    public SerializerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializerException(Throwable cause) {
        super(cause);
    }
}
