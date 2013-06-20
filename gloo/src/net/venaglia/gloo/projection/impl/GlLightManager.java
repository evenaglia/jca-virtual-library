package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.projection.TooManyLightsException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static net.venaglia.gloo.util.CallLogger.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * User: ed
 * Date: 9/27/12
 * Time: 10:06 AM
 */
class GlLightManager {

    public enum GlLight {
        LIGHT_0(GL_LIGHT0),
        LIGHT_1(GL_LIGHT1),
        LIGHT_2(GL_LIGHT2),
        LIGHT_3(GL_LIGHT3),
        LIGHT_4(GL_LIGHT4),
        LIGHT_5(GL_LIGHT5),
        LIGHT_6(GL_LIGHT6),
        LIGHT_7(GL_LIGHT7);

        public final int glCode;

        GlLight(int glCode) {
            this.glCode = glCode;
        }
    }

    private Map<GlLight,Light> activeLights = new EnumMap<GlLight,Light>(GlLight.class);
    private Map<Integer,GlLight> activeLightsByLightId = new HashMap<Integer,GlLight>();
    private EnumSet<GlLight> available = EnumSet.allOf(GlLight.class);

    private Set<Light> add = new LinkedHashSet<Light>();
    private Set<Light> update = new LinkedHashSet<Light>();
    private EnumSet<GlLight> keep = EnumSet.noneOf(GlLight.class);

    public void use(Light[] lights) throws TooManyLightsException {
        try {
            keep.clear();
            useImpl(lights, add, update, keep);
        } finally {
            add.clear();
            update.clear();
        }
    }

    private void useImpl(Light[] lights, Set<Light> add, Set<Light> update, EnumSet<GlLight> keep) throws TooManyLightsException {
        // decisions: add, remove, update
        for (Light light : lights) {
            GlLight glLight = activeLightsByLightId.get(light.getId());
            if (glLight != null) {
                if (!light.isStatic()) {
                    update.add(light);
                }
                keep.add(glLight);
            } else if (add.size() >= 8) {
                throw new TooManyLightsException();
            } else {
                add.add(light);
            }
        }
        if (add.size() + keep.size() > 8) {
            throw new TooManyLightsException();
        }

        // remove
        for (GlLight glLight : GlLight.values()) {
            if (!keep.contains(glLight)) {
                Light light = activeLights.remove(glLight);
                if (light != null) {
                    activeLightsByLightId.remove(light.getId());
                    available.add(glLight);
                    glDisable(glLight.glCode);
                    if (logCalls) logCall("glDisable", glLight.glCode);
                }
            }
        }

        // add
        if (!add.isEmpty()) {
            Iterator<GlLight> glLightIterator = available.iterator();
            for (Light light : add) {
                if (!glLightIterator.hasNext()) {
                    throw new TooManyLightsException();
                }
                GlLight glLight = glLightIterator.next();
                activeLights.put(glLight, light);
                activeLightsByLightId.put(light.getId(), glLight);
                glEnable(glLight.glCode);
                if (logCalls) logCall("glEnable", glLight.glCode);
                light.updateGL(glLight.glCode);
            }
        }

        // update
        for (Light light : update) {
            GlLight glLight = activeLightsByLightId.get(light.getId());
            light.updateGL(glLight.glCode);
        }
    }
}
