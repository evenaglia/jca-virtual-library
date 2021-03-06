package com.jivesoftware.jcalibrary.objects;

import com.jivesoftware.jcalibrary.api.JCAManager;
import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.JiveInstanceState;
import com.jivesoftware.jcalibrary.structures.NodeDetails;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.primitives.QuadSequence;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 5/11/13
 * Time: 11:07 PM
 */
public class VisualObjects {

    public static final Key<Shape<?>> BLANK_ICONS = new Key<Shape<?>>() {
        private final Vector xlate = new Vector(0,-0.5,0.25);
        private final Shape<?> blank = new QuadSequence(new Point(-1,0,0.5), new Point(-1,0,-0.5), new Point(1,0,-0.5), new Point(1,0,0.5),
                                                        new Point(1,0,0.5), new Point(1,0,-0.5), new Point(-1,0,-0.5), new Point(-1,0,0.5))
                .scale(0.45)
                .translate(xlate)
                .setMaterial(Material.makeFrontShaded(Color.BLACK));

        @Override
        public Shape<?> get(JiveInstance instance) {
            return blank;
        }

        @Override
        protected Shape<?> createFor(JiveInstance instance) {
            return blank;
        }
    };

    public static final Key<Shape<?>> ICONS = new Key<Shape<?>>() {
        private final Vector xlate = new Vector(0,-0.5,0.25);
        @Override
        protected Shape<?> createFor(JiveInstance instance) {
            int[] icons = JCAManager.INSTANCE.getCustomerIcons(instance.getCustomerInstallationId());
            if (icons == null) {
                return BLANK_ICONS.get(instance);
            } else {
                return new GlyphIdentifier(icons).scale(0.45).translate(xlate);
            }
        }
    };

    public static final Key<JiveInstanceState> OVERALL_STATE = new Key<JiveInstanceState>() {
        @Override
        protected JiveInstanceState createFor(JiveInstance instance) {
            int up = 0, down = 0;
            Map<String,NodeDetails> detailsMap = instance.getAllNodeDetails();
            for (NodeDetails details : detailsMap.values()) {
                String status = details.getStatus();
                if ("up".equals(status)) up++;
                else if ("down".equals(status)) down++;
            }
            JiveInstanceState state;
            if (up == 0 && down == 0) {
                state = JiveInstanceState.UNKNOWN;
            } else if (up == 0) {
                state = JiveInstanceState.OFFLINE;
            } else if (down == 0) {
                state = JiveInstanceState.OK;
            } else {
                state = JiveInstanceState.PARTIAL_DOWN;
            }
            return state;
        }
    };

    private final Map<Key<?>,Object> data = new HashMap<Key<?>,Object>();

    public void clear() {
        data.clear();
    }

    public abstract static class Key<T> {

        public void clear(JiveInstance instance) {
            instance.getVisualObjects().data.remove(this);
        }

        public T get(JiveInstance instance) {
            Map<Key<?>,Object> map = instance.getVisualObjects().data;
            if (map.containsKey(this)) {
                return cast(map.get(this));
            } else {
                T value = createFor(instance);
                map.put(this, value);
                return cast(value);
            }
        }

        protected abstract T createFor(JiveInstance instance);

        @SuppressWarnings("unchecked")
        private T cast(Object o) {
            return (T)o;
        }
    }
}
