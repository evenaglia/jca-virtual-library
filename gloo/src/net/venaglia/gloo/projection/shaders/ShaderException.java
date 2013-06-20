package net.venaglia.gloo.projection.shaders;

/**
 * User: ed
 * Date: 3/15/13
 * Time: 11:28 PM
 */
public class ShaderException extends RuntimeException {

    public ShaderException() {
    }

    public ShaderException(String message) {
        super(message);
    }

    public ShaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShaderException(Throwable cause) {
        super(cause);
    }
}
