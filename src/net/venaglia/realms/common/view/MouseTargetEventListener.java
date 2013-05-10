package net.venaglia.realms.common.view;

/**
 * User: ed
 * Date: 3/31/13
 * Time: 12:21 PM
 */
public interface MouseTargetEventListener<V> {

    enum MouseButton {
        PRIMARY(0), SECONDARY(1);

        public final int glCode;

        private MouseButton(int glCode) {
            this.glCode = glCode;
        }
    }

    void mouseOver(MouseTarget<? extends V> target);

    void mouseOut(MouseTarget<? extends V> target);

    void mouseDown(MouseTarget<? extends V> target, MouseButton button);

    void mouseUp(MouseTarget<? extends V> target, MouseButton button);
}
