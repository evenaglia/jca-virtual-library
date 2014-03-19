package net.venaglia.realms.common.map.db_x.impl.spatial;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 8/2/13
* Time: 9:07 PM
* To change this template use File | Settings | File Templates.
*/
enum EntryType {
    NULL_NODE,
    BOUNDARY_NODE,
    ROOT_NODE,
    VOXEL_NODE,
    VALUE_NODE,
    FREE_NODE;

    private final String simpleName;

    private EntryType() {
        String n = name();
        simpleName = n.substring(0, n.indexOf('_')).toLowerCase();
    }

    public byte[] initialData() {
        return INITIAL_DATA.get(this);
    }

    public byte encode() {
        return (byte)ordinal();
    }

    @Override
    public String toString() {
        return simpleName;
    }

    private static final Map<EntryType,byte[]> INITIAL_DATA;

    static {
        Map<EntryType,byte[]> initialData = new EnumMap<EntryType,byte[]>(EntryType.class);
        initialData.put(NULL_NODE, NullNode.getInitialData());
        initialData.put(BOUNDARY_NODE, BoundaryNode.getInitialData());
        initialData.put(ROOT_NODE, RootNode.getInitialData());
        initialData.put(VOXEL_NODE, VoxelNode.getInitialData());
        initialData.put(VALUE_NODE, ValueNode.getInitialData());
        initialData.put(FREE_NODE, FreeNode.getInitialData());
        INITIAL_DATA = Collections.unmodifiableMap(initialData);
    }

    public static EntryType decode(byte b) {
        return values()[b];
    }
}
