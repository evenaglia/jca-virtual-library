package net.venaglia.realms.common.physical.texture;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 1:20 AM
 */
public class TextureSizeException extends RuntimeException {

    private final int maxSize;

    public TextureSizeException(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
