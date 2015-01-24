package net.venaglia.gloo.view.impl;

import net.venaglia.gloo.view.ViewEventHandler;

/**
 * User: ed
 * Date: 1/12/15
 * Time: 7:33 PM
 */
public class ViewEventAdapter implements ViewEventHandler {

    @Override
    public void handleInit() {
        // no-op
    }

    @Override
    public void handleClose() {
        // no-op
    }

    @Override
    public void handleNewFrame(long now) {
        // no-op
    }

    public static ViewEventHandler exitOnClose() {
        return new ViewEventAdapter() {
            @Override
            public void handleClose() {
                System.exit(0);
            }
        };
    };
}
