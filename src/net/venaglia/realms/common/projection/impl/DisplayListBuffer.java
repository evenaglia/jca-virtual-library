package net.venaglia.realms.common.projection.impl;

import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.projection.DisplayList;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.TooManyDisplayListsException;

import static net.venaglia.realms.common.util.CallLogger.*;
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

    public void record(DisplayListRecorder recorder) throws TooManyDisplayListsException, IllegalArgumentException {
        recordImpl(recorder, GL_COMPILE);
    }

    public void recordAndExecute(DisplayListRecorder recorder) throws TooManyDisplayListsException, IllegalArgumentException {
        recordImpl(recorder, GL_COMPILE_AND_EXECUTE);
    }

    private void recordImpl(DisplayListRecorder recorder, int glCompileOption) throws TooManyDisplayListsException, IllegalArgumentException {
        if (state == State.NEW) {
            glDisplayListId = glGenLists(1);
            if (logCalls) logCall(glDisplayListId, "glGenLists", 1);
            if (glDisplayListId == 0) {
                throw new TooManyDisplayListsException();
            }
            CountingDirectGeometryBuffer directGeometryBuffer = new CountingDirectGeometryBuffer();
            try {
                state = State.RECORDING;
                glNewList(glDisplayListId, glCompileOption);
                if (logCalls) logCall("glNewList", glDisplayListId, glCompileOption);
                recorder.record(directGeometryBuffer);
            } finally {
                glEndList();
                if (logCalls) logCall("glEndList");
                state = State.LOADED;
            }
            if (directGeometryBuffer.getCount() == 0) {
                deallocate();
                throw new IllegalArgumentException("No Projectable elements were specified");
            }
            bounds = directGeometryBuffer.getBounds();
        } else if (state == State.RECORDING) {
            throw new IllegalStateException("Cannot record to a DisplayListBuffer while it is already recording geometry elsewhere");
        } else if (state == State.LOADED) {
            throw new IllegalStateException("Cannot record to a DisplayListBuffer that has already been loaded");
        }
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

    public boolean isStatic() {
        return true;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        if (state == State.LOADED) {
            try {
                buffer.pushBrush();
                buffer.callDisplayList(glDisplayListId);
            } finally {
                buffer.popBrush();
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
