package net.venaglia.gloo.physical.texture;

/**
 * User: ed
 * Date: 3/9/13
 * Time: 7:47 AM
 */
public class TextureLoadException extends RuntimeException {

    public TextureLoadException() {
    }

    public TextureLoadException(String s) {
        super(s);
    }

    public TextureLoadException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public TextureLoadException(Throwable throwable) {
        super(throwable);
    }
}
