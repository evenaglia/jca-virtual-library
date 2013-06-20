package net.venaglia.gloo.view;

import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;

/**
 * User: ed
 * Date: 9/6/12
 * Time: 10:04 PM
 */
public interface View3DMainLoop {

    boolean beforeFrame(long nowMS);

    MouseTargets getMouseTargets(long nowMS);

    void renderFrame(long nowMS, ProjectionBuffer buffer);

    void renderOverlay(long nowMS, GeometryBuffer buffer);

    void afterFrame(long nowMS);
}
