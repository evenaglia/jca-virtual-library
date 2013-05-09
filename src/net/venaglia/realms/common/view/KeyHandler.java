package net.venaglia.realms.common.view;

import static org.lwjgl.input.Keyboard.KEY_ESCAPE;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 4/16/13
* Time: 5:35 PM
* To change this template use File | Settings | File Templates.
*/
public class KeyHandler {

    public static final KeyHandler EXIT_JVM_ON_ESCAPE = new KeyHandler(KEY_ESCAPE) {
        @Override
        protected void handleKeyDown(int keyCode) {
            System.exit(0);
        }
    };

    public final int keyCode;

    public KeyHandler(int keyCode) {
        this.keyCode = keyCode;
        if (keyCode < 0 || keyCode > 255) {
            throw new IllegalArgumentException("Key codes must be between 0 and 255: " + keyCode);
        }
    }

    protected void handleKeyDown(int keyCode) {
        // no-op
    }

    protected void handleKeyUp(int keyCode) {
        // no-op
    }
}
