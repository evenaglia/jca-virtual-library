package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.decorators.Transformation;
import net.venaglia.gloo.projection.DisplayList;
import net.venaglia.gloo.projection.GeometryRecorder;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.RecordingBuffer;
import net.venaglia.gloo.projection.TooManyDisplayListsException;
import net.venaglia.gloo.projection.Transformable;

import static net.venaglia.gloo.util.CallLogger.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * User: ed
 * Date: 9/28/12
 * Time: 7:11 AM
 */
public class DisplayListBuffer implements DisplayList {

    private enum State {
        NEW, RECORDING, LOADED
    }

    private final String name;

    private int glDisplayListId;
    private State state;
    private Material initialMaterial;
    private BoundingVolume<?> bounds;
    private Brush brush = null;
    private RecordingBuffer.BrushOperation brushOperation = null;

    public DisplayListBuffer(String name) throws TooManyDisplayListsException {
        if (name == null) throw new NullPointerException("name");
        this.name = name;
        this.state = State.NEW;
    }

    public int getGlDisplayListId() {
        return glDisplayListId;
    }

    public Material getInitialMaterial() {
        return initialMaterial;
    }

    public void setInitialMaterial(Material initialMaterial) {
        this.initialMaterial = initialMaterial;
    }

    public void record(GeometryRecorder recorder) throws TooManyDisplayListsException, IllegalArgumentException {
        if (state == State.NEW) {
            glDisplayListId = glGenLists(1);
            if (logCalls) logCall(glDisplayListId, "glGenLists", (long)1);
            if (glDisplayListId == 0) {
                throw new TooManyDisplayListsException();
            }
            RecordingBufferImpl recordingBuffer = new RecordingBufferImpl();
            try {
                state = State.RECORDING;
                glNewList(glDisplayListId, GL_COMPILE);
                if (logCalls) logCall("glNewList", (long)glDisplayListId, GL_COMPILE);
                recorder.record(recordingBuffer);
            } finally {
                glEndList();
                if (logCalls) logCall("glEndList");
                state = State.LOADED;
            }
            if (recordingBuffer.getCount() == 0) {
                deallocate();
                throw new IllegalArgumentException("No Projectable elements were specified");
            }
            bounds = recordingBuffer.getBounds();
            brush = cloneBrush(recordingBuffer.getBrush());
            brushOperation = recordingBuffer.getApplyBrushWhenRun();
        } else if (state == State.RECORDING) {
            throw new IllegalStateException("Cannot record to a DisplayListBuffer while it is already recording geometry elsewhere");
        } else if (state == State.LOADED) {
            throw new IllegalStateException("Cannot record to a DisplayListBuffer that has already been loaded");
        }
    }

    private Brush cloneBrush(Brush brush) {
        return brush == null ? null : (brush.isImmutable() ? brush : new Brush(brush));
    }

    public void deallocate() {
        if (state == State.RECORDING) {
            throw new IllegalStateException("Cannot deallocate a display list while it is being recorded.");
        }
        if (state == State.LOADED) {
            glDeleteLists(glDisplayListId, 1);
            if (logCalls) logCall("glDeleteLists", glDisplayListId, 1);
            glDisplayListId = 0;
            state = State.NEW;
            bounds = null;
        }
    }

    public Transformable transformable() {
        return new Transformable() {

            private Transformation transformation = new Transformation();

            public Transformation getTransformation() {
                return transformation;
            }

            public BoundingVolume<?> getBounds() {
                return bounds;
            }

            public boolean isStatic() {
                return true;
            }

            public void project(long nowMS, GeometryBuffer buffer) {
                if (state == State.LOADED) {
                    buffer.pushTransform();
                    try {
                        transformation.apply(nowMS, buffer);
                        DisplayListBuffer.this.project(nowMS, buffer);
                    } finally {
                        buffer.popTransform();
                    }
                }
            }
        };
    }

    public boolean isStatic() {
        return true;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        if (state == State.LOADED) {
            RecordingBuffer.BrushOperation op = brush != null ? brushOperation : RecordingBuffer.BrushOperation.INHERIT;
            switch (op) {
                case INHERIT:
                    buffer.callDisplayList(glDisplayListId);
                    break;
                case APPLY_BRUSH:
                    buffer.applyBrush(brush);
                    buffer.callDisplayList(glDisplayListId);
                    break;
                case PUSH_APPLY_POP_BRUSH:
                    try {
                        buffer.pushBrush();
                        buffer.applyBrush(brush);
                        buffer.callDisplayList(glDisplayListId);
                    } finally {
                        buffer.popBrush();
                    }
                    break;
            }
        } else if (state == State.NEW) {
            throw new IllegalStateException("Cannot project a DisplayListBuffer before recording geometry");
        } else if (state == State.RECORDING) {
            throw new IllegalStateException("Cannot project a DisplayListBuffer while recording geometry");
        }
    }

    public BoundingVolume<?> getBounds() {
        return bounds;
    }

    @Override
    public String toString() {
        return "DisplayList('" + name + "':" + glDisplayListId + ")";
    }

}
