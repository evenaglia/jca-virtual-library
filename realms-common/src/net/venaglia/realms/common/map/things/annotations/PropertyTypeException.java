package net.venaglia.realms.common.map.things.annotations;

import java.lang.reflect.Field;

/**
 * User: ed
 * Date: 4/15/14
 * Time: 5:15 PM
 */
public class PropertyTypeException extends RuntimeException {

    private final Field field;

    public PropertyTypeException(Field field) {
        this.field = field;
    }

    public PropertyTypeException(String message, Field field) {
        super(message);
        this.field = field;
    }

    public PropertyTypeException(String message, Throwable cause, Field field) {
        super(message, cause);
        this.field = field;
    }

    public PropertyTypeException(Throwable cause, Field field) {
        super(cause);
        this.field = field;
    }

    public Field getField() {
        return field;
    }
}
