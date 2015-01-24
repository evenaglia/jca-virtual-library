package net.venaglia.gloo.view.impl;

import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.View3DMainLoop;

/**
 * User: ed
 * Date: 1/12/15
 * Time: 7:32 PM
 */
public class View3DMainLoopAdapter implements View3DMainLoop {

    @Override
    public boolean beforeFrame(long nowMS) {
        return true;
    }

    @Override
    public MouseTargets getMouseTargets(long nowMS) {
        return null;
    }

    @Override
    public void renderFrame(long nowMS, ProjectionBuffer buffer) {
        // no-op
    }

    @Override
    public void renderOverlay(long nowMS, GeometryBuffer buffer) {
        // no-op
    }

    @Override
    public void afterFrame(long nowMS) {
        // no-op
    }
}
