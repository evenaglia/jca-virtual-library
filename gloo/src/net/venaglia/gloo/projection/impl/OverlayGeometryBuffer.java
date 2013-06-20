package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.projection.GeometryBuffer;

/**
* User: ed
* Date: 4/23/13
* Time: 6:51 PM
*/
class OverlayGeometryBuffer extends DelegatingGeometryBuffer {

    public OverlayGeometryBuffer(GeometryBuffer delegate) {
        super(delegate);
    }

    @Override
    public boolean isScreen() {
        return false;
    }

    @Override
    public boolean isOverlay() {
        return true;
    }

    @Override
    public boolean isTarget() {
        return false;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }
}
