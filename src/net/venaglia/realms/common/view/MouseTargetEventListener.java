package net.venaglia.realms.common.view;

/**
 * User: ed
 * Date: 3/31/13
 * Time: 12:21 PM
 */
public interface MouseTargetEventListener<V> {

    enum MouseButton {
        PRIMARY, SECONDARY
    }

    void mouseOver(MouseTarget<? extends V> target);

    void mouseOut(MouseTarget<? extends V> target);

    void mouseClick(MouseTarget<? extends V> target, MouseButton button);
}
