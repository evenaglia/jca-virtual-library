package net.venaglia.realms.common.map.things.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: ed
 * Date: 4/15/14
 * Time: 4:09 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Property {

    public enum Flags {
        REQUIRED, READONLY,
    }

    Flags[] value() default {};

    String name() default "";
}
