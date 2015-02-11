package net.venaglia.common.util.extensible;

/**
 * User: ed
 * Date: 1/25/15
 * Time: 9:29 PM
 */
public class ExtendedPropertyProviderException extends RuntimeException {

    public ExtendedPropertyProviderException() {
    }

    public ExtendedPropertyProviderException(String message) {
        super(message);
    }

    public ExtendedPropertyProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtendedPropertyProviderException(Throwable cause) {
        super(cause);
    }
}
