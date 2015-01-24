package net.venaglia.gloo.view;

import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;

/**
 * User: ed
 * Date: 9/6/12
 * Time: 10:04 PM
 */
public interface View3DMainLoop {

    /**
     * Run something before the frame renders
     * @param nowMS Timestamp when this frame is being to to be shown.
     * @return True to render this frame, false to skip this frame.
     */
    boolean beforeFrame(long nowMS);

    /**
     * Called to obtain any mouse targets for the current frame being rendered.
     * @param nowMS Timestamp when this frame is being to to be shown.
     * @return The MouseTargets to generate possible mouse over events for on screen objects.
     */
    MouseTargets getMouseTargets(long nowMS);

    /**
     * Called once per frame to render 3D objects in the view.
     * @param nowMS Timestamp when this frame is being to to be shown.
     * @param buffer The ProjectionBuffer to draw 3D objects upon.
     */
    void renderFrame(long nowMS, ProjectionBuffer buffer);

    /**
     * Called after renderFrame() to capture any overlay graphics to be rendered to the screen.
     * @param nowMS Timestamp when this frame is being to to be shown.
     * @param buffer The GeometryBuffer to dray overlay elements on the screen.
     */
    void renderOverlay(long nowMS, GeometryBuffer buffer);

    /**
     * Called after the frame has been rendered.
     * @param nowMS Timestamp when this frame is being to to be shown.
     */
    void afterFrame(long nowMS);
}
