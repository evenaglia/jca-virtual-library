package net.venaglia.realms.common.map.things.annotations;

import net.venaglia.common.util.Factory;
import net.venaglia.common.util.Predicate;
import net.venaglia.common.util.Visitor;
import net.venaglia.common.util.serializer.AbstractSerializerStrategy;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.realms.common.map.things.AbstractThing;
import net.venaglia.realms.common.map.things.PropertyAccessor;
import net.venaglia.realms.common.map.things.PropertyType;
import net.venaglia.realms.common.map.things.Thing;
import net.venaglia.realms.common.map.things.ThingMetadata;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: ed
 * Date: 4/15/14
 * Time: 4:48 PM
 */
public class AnnotationDrivenThingProcessor {

    <O extends AbstractThing> void processImpl(Class<O> type,
                                               ThingDefinition<O> definition,
                                               PropertyAccessor propertyAccessor,
                                               O prototype) {
        if (!AbstractThing.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Passed type does not extend AbstractThing");
        }
        Map<String,FieldAccessor<O,?>> fieldAccessors = new HashMap<String,FieldAccessor<O,?>>();
        for (Field field : getAllFields(type)) {
            Property property = field.getAnnotation(Property.class);
            if (property != null) {
                PropertyType<?> propertyType = PropertyType.resolveForType(field.getType());
                if (propertyType == null) {
                    throw new PropertyTypeException("Could not find instance of PropertyType<" + field.getType() + ">", field);
                }
                FieldAccessor<O,?> accessor = buildAccessor(definition, prototype, field, property, propertyType);
                fieldAccessors.put(field.getName(), accessor);
            }
        }
        if (propertyAccessor == null) {
            propertyAccessor = new PropertyAccessorImpl<O>(type, fieldAccessors);
        }
        definition.setPropertyAccessor(propertyAccessor);
    }

    private <O extends AbstractThing,P> FieldAccessor<O,P> buildAccessor(ThingDefinition<O> definition,
                                                                         O prototype,
                                                                         Field field,
                                                                         Property property,
                                                                         PropertyType<P> propertyType) {
        FieldAccessor<O,P> accessor = new FieldAccessor<O,P>(field, propertyType);
        P defaultValue = accessor.get(prototype, null);
        Collection<Property.Flags> flags = Arrays.asList(property.value());
        boolean required = flags.contains(Property.Flags.REQUIRED);
        boolean writable = !flags.contains(Property.Flags.READONLY);
        String name = property.name().length() == 0 ? field.getName() : property.name();
        definition.addProperty(name, propertyType, defaultValue, required, writable);
        return accessor;
    }

    private static <O,P> FieldAccessor<O,P> buildAccessor(Field field,
                                                          PropertyType<P> propertyType) {
        return new FieldAccessor<O,P>(field, propertyType);
    }

    private static Map<Class<?>,ThingDefinition<?>> DEFINITION_CACHE = new ConcurrentHashMap<Class<?>,ThingDefinition<?>>(32);
    private static Map<Class<?>,SerializerStrategy<?>> SERIALIZER_CACHE = new ConcurrentHashMap<Class<?>,SerializerStrategy<?>>(32);

    public static <O extends AbstractThing> ThingDefinition<O> process(O prototype) {
        @SuppressWarnings("unchecked")
        Class<O> type = (Class<O>)prototype.getClass();
        @SuppressWarnings("unchecked")
        ThingDefinition<O> definition = (ThingDefinition<O>)DEFINITION_CACHE.get(type);
        if (definition == null) {
            ThingType thingType = type.getAnnotation(ThingType.class);
            definition = ThingDefinition.define(thingType == null ? type.getSimpleName() : thingType.value(), type);
            DEFINITION_CACHE.put(type, definition);
            CustomPropertyAccessor customPropertyAccessor = type.getAnnotation(CustomPropertyAccessor.class);
            PropertyAccessor accessor = null;
            if (customPropertyAccessor != null) {
                try {
                    accessor = customPropertyAccessor.value().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            new AnnotationDrivenThingProcessor().processImpl(type, definition, accessor, prototype);
            definition.done();
        }
        return definition;
    }

    public static <O> SerializerStrategy<O> generateSerializer(final Class<O> type, final Factory<O> factory) {
        return generateSerializer(type, factory, null, null);
    }

    @SuppressWarnings("unchecked")
    public static <O> SerializerStrategy<O> generateSerializer(final Class<O> type,
                                                               final Factory<O> factory,
                                                               final Visitor<O> preProcessor,
                                                               final Visitor<O> postProcessor) {
        @SuppressWarnings("unchecked")
        SerializerStrategy<O> serializer = (SerializerStrategy<O>)SERIALIZER_CACHE.get(type);
        if (serializer == null) {
            final Map<String,FieldAccessor<O,?>> fieldAccessors = new LinkedHashMap<String,FieldAccessor<O,?>>();
            for (Field field : getAllFields(type)) {
                Property property = field.getAnnotation(Property.class);
                if (property != null) {
                    PropertyType<?> propertyType = PropertyType.resolveForType(field.getType());
                    if (propertyType == null) {
                        throw new PropertyTypeException("Could not find instance of PropertyType<" + field.getType() + ">", field);
                    }
                    FieldAccessor<O,?> accessor = buildAccessor(field, propertyType);
                    fieldAccessors.put(field.getName(), accessor);
                }
            }
            serializer = new GeneratedSerializerStrategy<O>(fieldAccessors, factory, preProcessor, postProcessor);
            SERIALIZER_CACHE.put(type, serializer);
        }
        return serializer;
    }

    private static Collection<Field> getAllFields(Class<?> type) {
        Map<String,Field> allFields = new LinkedHashMap<String,Field>();
        while (!Object.class.equals(type)) {
            for (Field field : type.getDeclaredFields()) {
                if (!allFields.containsKey(field.getName())) {
                    allFields.put(field.getName(), field);
                }
            }
            type = type.getSuperclass();
        }
        return allFields.values();
    }

    private static class PropertyAccessorImpl<O extends AbstractThing> implements PropertyAccessor {

        private final Class<O> type;
        private final Map<String,FieldAccessor<O,?>> fieldAccessors;

        private PropertyAccessorImpl(Class<O> type,
                                     Map<String,FieldAccessor<O,?>> fieldAccessors) {
            this.type = type;
            this.fieldAccessors = fieldAccessors;
        }

        public void beforeUpdate() {
            // no-op
        }

        public void afterUpdate() {
            // no-op
        }

        public void beforeRead() {
            // no-op
        }

        public void afterRead() {
            // no-op
        }

        public <P> P get(ThingMetadata.PropertyMetadata<P> property, Thing thing) {
            @SuppressWarnings("unchecked")
            FieldAccessor<O,P> accessor = (FieldAccessor<O,P>)fieldAccessors.get(property.getName());
            return accessor.get(type.cast(thing), property.getDefaultValue());
        }

        public <P> void set(ThingMetadata.PropertyMetadata<P> property, Thing thing, P value) {
            if (value == null) {
                value = property.getDefaultValue();
            }
            @SuppressWarnings("unchecked")
            FieldAccessor<O,P> accessor = (FieldAccessor<O,P>)fieldAccessors.get(property.getName());
            accessor.set(type.cast(thing), value);
        }
    }

    private static class FieldAccessor<O,F> {

        private final Field field;
        private final PropertyType<F> type;

        private FieldAccessor(Field field, PropertyType<F> type) {
            field.setAccessible(true);
            this.field = field;
            this.type = type;
        }

        PropertyType<F> getType() {
            return type;
        }

        void set(O object, F value) {
            try {
                field.set(object, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        F get(O object, F defaultValue) {
            Object value = null;
            try {
                value = field.get(object);
                return value == null ? defaultValue : type.cast(value);
            } catch (ClassCastException e) {
                if (value == null) value = Void.TYPE;
                throw new RuntimeException("Unable to convert " + value + " <" + value.getClass().getSimpleName() + "> to " + type);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public static class GeneratedSerializerStrategy<O> extends AbstractSerializerStrategy<O> {

        private final Map<String, FieldAccessor<O, ?>> fieldAccessors;
        private final Factory<O> factory;
        private final Visitor<O> preProcessor;
        private final Visitor<O> postProcessor;

        public GeneratedSerializerStrategy(Map<String, FieldAccessor<O, ?>> fieldAccessors,
                                           Factory<O> factory,
                                           Visitor<O> preProcessor,
                                           Visitor<O> postProcessor) {
            this.fieldAccessors = fieldAccessors;
            this.factory = factory;
            this.preProcessor = preProcessor;
            this.postProcessor = postProcessor;
        }

        public void serialize(O value, ByteBuffer out) {
            if (preProcessor != null) {
                preProcessor.visit(value);
            }
            serializeSmallNonNegativeInteger("<fields>", fieldAccessors.size(), out);
            for (FieldAccessor<O, ?> accessor : fieldAccessors.values()) {
                writeField(accessor, value, out);
            }
        }

        private <O,P> void writeField(FieldAccessor<O,P> accessor, O value, ByteBuffer out) {
            String name = accessor.field.getName();
            serializeString("<name>", name, out);
            P fieldValue = accessor.get(value, accessor.getType().getDefaultValue());
            if (fieldValue == null) {
                serializeInt(name + ".bytes", -1, out);
            } else {
                SizeMarker sizeMarker = serializeSize(out);
                accessor.getType().getSerializer().serialize(fieldValue, out);
                sizeMarker.close();
            }
        }

        public void deserializePartial(ByteBuffer in, Predicate<? super String> filter, Map<String, Object> out) {
            int n = (int)deserializeSmallNonNegativeInteger("<fields>", in);
            for (int i = 0; i < n; i++) {
                String name = deserializeString("<name>", in);
                if (filter.allow(name)) {
                    FieldAccessor<O,?> fieldAccessor = fieldAccessors.get(name);
                    readField(fieldAccessor, out, in);
                }
            }
        }

        public O deserialize(ByteBuffer in) {
            return deserializePartial(in, Predicate.ALWAYS_TRUE);
        }

        public O deserializePartial(ByteBuffer in, Predicate<? super String> filter) {
            O value = factory.createEmpty();
            int n = (int)deserializeSmallNonNegativeInteger("<fields>", in);
            for (int i = 0; i < n; i++) {
                String name = deserializeString("<name>", in);
                FieldAccessor<O,?> fieldAccessor = fieldAccessors.get(name);
                readField(fieldAccessor, value, in, filter.allow(name));
            }
            if (postProcessor != null) {
                postProcessor.visit(value);
            }
            return value;
        }

        private <P> void readField(FieldAccessor<O,P> fieldAccessor, O object, ByteBuffer in, boolean set) {
            int size = deserializeInt("<size>", in);
            if (fieldAccessor == null) {
                if (size > 0) {
                    skip(size, in);
                }
                return;
            }
            P value = size >= 0 ? fieldAccessor.getType().read(in) : fieldAccessor.getType().getDefaultValue();
            if (set) {
                fieldAccessor.set(object, value);
            }
        }

        private <P> void readField(FieldAccessor<O,P> fieldAccessor, Map<String,Object> out, ByteBuffer in) {
            String name = fieldAccessor == null ? "<?>" : fieldAccessor.field.getName();
            int size = deserializeInt(name + ".bytes", in);
            if (fieldAccessor == null) {
                if (size > 0) {
                    skip(size, in);
                }
                return;
            }
            P value = size >= 0 ? fieldAccessor.getType().read(in) : fieldAccessor.getType().getDefaultValue();
            out.put(fieldAccessor.field.getName(), value);
        }
    }
}
