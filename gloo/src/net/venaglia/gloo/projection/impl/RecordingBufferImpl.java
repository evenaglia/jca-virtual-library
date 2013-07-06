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
    protected BrushOperation applyBrushWhenRun = BrushOperation.INHERIT;
    protected BrushAction actionOnApplyBrush = BrushAction.IGNORE;

    public RecordingBufferImpl() {
        // set a mostly complete brush as default. This ensures that normals and texture coordinates are recorded.
        activeBrush = Brush.TEXTURED;
    }

    public void setBrush(Brush brush) {
        this.activeBrush = new ActiveBrush(brush);
    }

    public Brush getBrush() {
        return this.activeBrush;
    }

    @Override
    public void applyBrush(Brush brush) {
        switch (actionOnApplyBrush) {
            case APPLY:
                super.applyBrush(brush);
                break;
            case IGNORE:
                break;
            case FAIL:
                throw new UnsupportedOperationException("Brushes cannot be changed in the middle of a display list. To set a " +
                                                        "brush to be use for the entire display list, call RecordingBuffer." +
                                                        "setBrush(), then RecordingBuffer.setApplyBrushWhenRun().");
        }
    }

    public void setRecordTextureBindings(boolean recordTextureBindings) {
        this.recordTextureBindings = recordTextureBindings;
    }

    public BrushOperation getApplyBrushWhenRun() {
        return applyBrushWhenRun;
    }

    public void setApplyBrushWhenRun(BrushOperation brushOperation) {
        if (brushOperation == null) {
            brushOperation = BrushOperation.INHERIT;
        }
        if (this.applyBrushWhenRun != brushOperation) {
            this.applyBrushWhenRun = brushOperation;
            count++;
        }
    }

    public void setActionOnApplyBrush(BrushAction actionOnApplyBrush) {
        if (actionOnApplyBrush == null) {
            actionOnApplyBrush = BrushAction.IGNORE;
        }
        if (this.actionOnApplyBrush != actionOnApplyBrush) {
            this.actionOnApplyBrush = actionOnApplyBrush;
            count++;
        }
    }

    @Override
    protected void bindTextureImpl(int glTextureNum, int glTextureId) {
        if (recordTextureBindings) {
            super.bindTextureImpl(glTextureNum, glTextureId);
        }
    }
}
