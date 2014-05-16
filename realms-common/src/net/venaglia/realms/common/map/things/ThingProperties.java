package net.venaglia.realms.common.map.things;

import net.venaglia.common.util.Pair;
import net.venaglia.common.util.serializer.ObjectSerializer;
import net.venaglia.common.util.serializer.PrimitiveSerializerStrategy;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.common.util.serializer.StringSerializerStrategy;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * User: ed
 * Date: 4/5/14
 * Time: 11:37 AM
 */
public class ThingProperties implements Iterable<Pair<ThingMetadata.PropertyMetadata<?>,Object>> {

    private final ThingMetadata<?> metadata;

    private byte[] buffer;
    private Map<ThingMetadata.PropertyMetadata<?>,Object> dirtyValues = null;

    public ThingProperties(ThingMetadata<?> metadata, byte[] buffer) {
        this.metadata = metadata;
        this.buffer = buffer;
    }

    public byte[] updateBuffer() {
        if (dirtyValues == null || dirtyValues.isEmpty()) {
            return buffer;
        }
        final StringSerializerStrategy stringSerializer = StringSerializerStrategy.INSTANCE;
        final SerializerStrategy<Boolean> booleanSerializer = PrimitiveSerializerStrategy.BOOLEAN;
        Iterator<Pair<ThingMetadata.PropertyMetadata<?>, Object>> iterator = dirtyIterator();
        ByteBuffer out = ObjectSerializer.BUFFERS.get();
        try {
            while (iterator.hasNext()) {
                Pair<ThingMetadata.PropertyMetadata<?>,Object> pair = iterator.next();
                writeProperty(stringSerializer, booleanSerializer, out, pair);
            }
            out.flip();
            byte[] b = new byte[out.limit()];
            out.get(b);
            dirtyValues.clear();
            return buffer = b;
        } finally {
            out.clear();
        }
    }

    private <P> void writeProperty(StringSerializerStrategy stringSerializer,
                                   SerializerStrategy<Boolean> booleanSerializer,
                                   ByteBuffer out,
                                   Pair<ThingMetadata.PropertyMetadata<?>,Object> pair) {
        @SuppressWarnings("unchecked")
        ThingMetadata.PropertyMetadata<P> property = (ThingMetadata.PropertyMetadata<P>)pair.getA();
        stringSerializer.serialize(property.getName(), out);
        @SuppressWarnings("unchecked")
        P value = (P)pair.getB();
        boolean isNull = false;
        if (!property.isRequired()) {
            isNull = value == null;
            booleanSerializer.serialize(isNull, out);
        }
        if (!isNull) {
            property.getType().getSerializer().serialize(value, out);
        }
    }

    public Iterator<Pair<ThingMetadata.PropertyMetadata<?>,Object>> dirtyIterator() {
        final Iterator<Pair<ThingMetadata.PropertyMetadata<?>,Object>> baseIterator = iterator();
        final Set<ThingMetadata.PropertyMetadata<?>> unvisited =
                new HashSet<ThingMetadata.PropertyMetadata<?>>(dirtyValues.keySet());
        return new Iterator<Pair<ThingMetadata.PropertyMetadata<?>,Object>>() {

            private Pair<ThingMetadata.PropertyMetadata<?>,Object> next = null;
            private Iterator<ThingMetadata.PropertyMetadata<?>> overrides;

            public boolean hasNext() {
                if (next != null) return true;
                while (next == null && baseIterator.hasNext()) {
                    next = baseIterator.next();
                    ThingMetadata.PropertyMetadata<?> property = next.getA();
                    if (dirtyValues.containsKey(property)) {
                        unvisited.remove(property);
                        Object value = dirtyValues.get(property);
                        next = (value == null) ? null : new Pair<ThingMetadata.PropertyMetadata<?>,Object>(property, value);
                    }
                }
                if (next == null && overrides == null) overrides = unvisited.iterator();
                while (next == null && overrides.hasNext()) {
                    ThingMetadata.PropertyMetadata<?> property = overrides.next();
                    Object value = dirtyValues.get(property);
                    next = (value == null) ? null : new Pair<ThingMetadata.PropertyMetadata<?>,Object>(property, value);
                }
                return next != null;
            }

            public Pair<ThingMetadata.PropertyMetadata<?>,Object> next() {
                if (!hasNext()) throw new NoSuchElementException();
                try {
                    return next;
                } finally {
                    next = null;
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterator<Pair<ThingMetadata.PropertyMetadata<?>,Object>> iterator() {
        final ByteBuffer in = ByteBuffer.wrap(buffer);
        final StringSerializerStrategy stringSerializer = StringSerializerStrategy.INSTANCE;
        final SerializerStrategy<Boolean> booleanSerializer = PrimitiveSerializerStrategy.BOOLEAN;
        return new Iterator<Pair<ThingMetadata.PropertyMetadata<?>, Object>>() {

            public boolean hasNext() {
                return in.remaining() > 0;
            }

            public Pair<ThingMetadata.PropertyMetadata<?>,Object> next() {
                String key = stringSerializer.deserialize(in);
                ThingMetadata.PropertyMetadata<?> property = metadata.getPropertyMetadata(key);
                boolean isNull = false;
                if (!property.isRequired()) {
                    isNull = booleanSerializer.deserialize(in);
                }
                Object value = null;
                if (!isNull) {
                    value = property.getType().getSerializer().deserialize(in);
                }
                return new Pair<ThingMetadata.PropertyMetadata<?>,Object>(property, value);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void set(ThingMetadata.PropertyMetadata<?> property, Object value) {
        if (dirtyValues == null) {
            dirtyValues = new HashMap<ThingMetadata.PropertyMetadata<?>,Object>(4);
        }
        dirtyValues.put(property, value);
    }
}
