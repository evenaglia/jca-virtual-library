package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.projection.CoordinateList;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 1/7/15
 * Time: 8:13 AM
 */
public class CompositeCoordinateList extends AbstractCoordinateList {

    private final Map<Field,CoordinateList> delegates = new EnumMap<Field,CoordinateList>(Field.class);
    private final Set<Field> fieldSet = Collections.unmodifiableSet(delegates.keySet());

    private int size = 0;

    public CompositeCoordinateList(CoordinateList vertices) {
        super(false);
        if (vertices == null) {
            throw new NullPointerException("vertices");
        }
        compose(Field.VERTEX, vertices);
    }

    CompositeCoordinateList compose(Field field, CoordinateList list) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        if (list == null) {
            throw new NullPointerException("list");
        }
        if (!list.has(field)) {
            throw new IllegalArgumentException("Cannot compose a " + field + " when the passed CoordinateList does not store a " + field);
        }
        delegates.put(field, list);
        int size = list.size();
        for (CoordinateList coordinateList : delegates.values()) {
            size = coordinateList == null ? size : Math.min(coordinateList.size(), size);
        }
        this.size = size;
        return this;
    }

    @Override
    protected Set<Field> getFields() {
        return fieldSet;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public ByteBuffer data(Field field) {
        if (field == null) {
            throw new NullPointerException("field");
        }
        CoordinateList list = delegates.get(field);
        return list == null || !list.has(field) ? null : list.data(field);
    }
}
