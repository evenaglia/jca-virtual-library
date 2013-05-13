package com.jivesoftware.jcalibrary.urgency;

import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.NodeDetails;
import com.jivesoftware.jcalibrary.structures.SlotTransformation;
import net.venaglia.realms.common.util.Pair;

import java.util.Arrays;
import java.util.Collection;

/**
 * User: ed
 * Date: 5/12/13
 * Time: 7:48 PM
 */
public enum StandardUrgencyFilter implements UrgencyFilter<Pair<Float,Float>> {

    NONE {
        @Override
        public Pair<Float, Float> buildBaseLine(Collection<JiveInstance> allJiveInstances) {
            return null;
        }

        @Override
        public void apply(JiveInstance jiveInstance,
                          SlotTransformation slotTransformation,
                          Pair<Float, Float> baseLine) {
            slotTransformation.setTarget(1,0);
        }

        @Override
        protected float getValue(JiveInstance jiveInstance) {
            return 0;
        }
    },

    LOAD_AVERAGE {
        @Override
        protected float getValue(JiveInstance jiveInstance) {
            float max = 0;
            for (NodeDetails details : jiveInstance.getAllNodeDetails().values()) {
                max = Math.max(max, details.getLoadAverage());
            }
            return max;
        }
    },

    ACTIVE_CONNECTIONS {
        @Override
        protected float getValue(JiveInstance jiveInstance) {
            float max = 0;
            for (NodeDetails details : jiveInstance.getAllNodeDetails().values()) {
                if (details.getType().startsWith("db") || "thunder".equals(details.getType())) {
                    max = Math.max(max, details.getActiveConnections());
                }
            }
            return max;
        }
    },

    ACTIVE_SESSIONS {
        @Override
        protected float getValue(JiveInstance jiveInstance) {
            float max = 0;
            for (NodeDetails details : jiveInstance.getAllNodeDetails().values()) {
                if ("thunder".equals(details.getType()) || "webapp".equals(details.getType())) {
                    max = Math.max(max, details.getActiveSessions());
                }
            }
            return max;
        }
    };

    private static final float[] STATS_BUFFER = new float[4096];

    protected abstract float getValue(JiveInstance jiveInstance);

    @Override
    public Pair<Float,Float> buildBaseLine(Collection<JiveInstance> allJiveInstances) {
        if (allJiveInstances.size() < 10) {
            return null; // not enough data
        }
        int i = 0;
        for (JiveInstance instance : allJiveInstances) {
            STATS_BUFFER[i++] = getValue(instance);
        }
        Arrays.sort(STATS_BUFFER, 0, i);
        int eighty = Math.max(0, Math.round(i * 0.95f) - 1);
        float base = STATS_BUFFER[eighty];
        float max = STATS_BUFFER[i - 1];
        return new Pair<Float,Float>(base, max - base);
    }

    @Override
    public void apply(JiveInstance jiveInstance, SlotTransformation slotTransformation, Pair<Float,Float> baseLine) {
        if (baseLine == null) {
            return; // not enough data
        }
        float base = baseLine.getA();
        float limit = baseLine.getB();
        float telescope = Math.min(Math.max(0, getValue(jiveInstance) - base), limit) * 10.0f / limit;
        slotTransformation.setTarget(scale(telescope), telescope);
    }

    protected double scale(double telescope) {
        return telescope * 0.2 + 1;
    }
}
