package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.projection.RecordingBuffer;

/**
 * User: ed
 * Date: 6/12/13
 * Time: 6:16 PM
 */
public class RecordingBufferImpl extends CountingDirectGeometryBuffer implements RecordingBuffer {

    protected boolean recordTextureBindings = true;

    public void setBrush(Brush brush) {
        this.activeBrush = new ActiveBrush(brush);
    }

    public void setRecordTextureBindings(boolean recordTextureBindings) {
        this.recordTextureBindings = recordTextureBindings;
    }

    @Override
    protected void bindTextureImpl(int glTextureNum, int glTextureId) {
        if (recordTextureBindings) {
            super.bindTextureImpl(glTextureNum, glTextureId);
        }
    }
}
