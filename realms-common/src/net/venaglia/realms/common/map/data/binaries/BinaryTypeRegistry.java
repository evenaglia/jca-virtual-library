package net.venaglia.realms.common.map.data.binaries;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * User: ed
 * Date: 4/13/14
 * Time: 7:54 PM
 */
public class BinaryTypeRegistry {

    private static final BinaryTypeRegistry INSTANCE = new BinaryTypeRegistry();

    private final ConcurrentMap<String,BinaryType> knownTypes = new ConcurrentHashMap<String,BinaryType>();

    private BinaryTypeRegistry() {
    }

    private void addImpl(BinaryType type) {
        BinaryType prev = knownTypes.putIfAbsent(type.mimeType(), type);
        if (prev != null && prev != type) {
            throw new IllegalStateException("A binary type is already registered for " + type.mimeType() + ": " + prev);
        }
    }

    private BinaryType getImpl(String mimeType) {
        return knownTypes.get(mimeType);
    }

    public static BinaryType get(String mimeType) {
        return INSTANCE.getImpl(mimeType);
    }

    public static void add(BinaryType type) {
        INSTANCE.addImpl(type);
    }
}
