package net.venaglia.gloo.view;

/**
 * User: ed
 * Date: 9/6/12
 * Time: 9:59 PM
 */
public interface ViewEventHandler {

    void handleInit();

    void handleClose();

    void handleNewFrame(long now);
}
