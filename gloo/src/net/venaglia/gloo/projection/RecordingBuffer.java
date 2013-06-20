package net.venaglia.gloo.projection;

import net.venaglia.gloo.physical.decorators.Brush;

/**
 * User: ed
 * Date: 6/12/13
 * Time: 6:12 PM
 */
public interface RecordingBuffer extends GeometryBuffer {

    /**
     * Sets properties of the active brush, without updating GL state
     * @param brush The brush to load
     */
    void setBrush(Brush brush);

    void setRecordTextureBindings(boolean recordTextureBindings);
}
