package net.venaglia.realms.common.view;

import net.venaglia.realms.common.util.Series;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * User: ed
 * Date: 3/30/13
 * Time: 10:21 PM
 */
public class MouseTargets implements Series<MouseTarget<?>> {

    private static final Comparator<Object> MOUSE_TARGET_ORDER = new Comparator<Object>() {
        public int compare(Object left, Object right) {
            int l = valueOf(left);
            int r = valueOf(right);
            return l - r;
        }

        private int valueOf(Object o) {
            if (o instanceof Integer) {
                return (Integer)o;
            } else if (o instanceof MouseTarget<?>) {
                return ((MouseTarget<?>)o).getGlName();
            } else if (o == null) {
                throw new NullPointerException();
            }
            throw new IllegalArgumentException("MOUSE_TARGET_ORDER only support comparing Integer and MouseTarget objects: " + o.getClass());
        }
    };

    private final TreeSet<Object> targets = new TreeSet<Object>(MOUSE_TARGET_ORDER);

    public MouseTargets() {
    }

    public MouseTargets(Collection<? extends MouseTarget<?>> c) {
        targets.addAll(c);
    }

    public int size() {
        return targets.size();
    }

    public boolean isEmpty() {
        return targets.isEmpty();
    }

    @SuppressWarnings({ "unchecked", "ConstantConditions" })
    public Iterator<MouseTarget<?>> iterator() {
        Iterator iterator = targets.iterator();
        return (Iterator<MouseTarget<?>>)iterator;
    }

    public boolean add(MouseTarget<?> mouseTarget) {
        return targets.add(mouseTarget);
    }

    public boolean addAll(Collection<MouseTarget<?>> mouseTargets) {
        return targets.addAll(mouseTargets);
    }

    public boolean remove(MouseTarget<?> mouseTarget) {
        return targets.remove(mouseTarget);
    }

    public boolean removeAll(Collection<MouseTarget<?>> mouseTargets) {
        return targets.removeAll(mouseTargets);
    }

    public boolean retainAll(Collection<MouseTarget<?>> mouseTargets) {
        return targets.retainAll(mouseTargets);
    }

    MouseTarget<?> getByName(Integer name) {
        NavigableSet<Object> set = targets.subSet(name, true, name, true);
        return set.isEmpty() ? null : (MouseTarget<?>)set.first();
    }
}
