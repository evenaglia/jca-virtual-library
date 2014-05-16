package net.venaglia.realms.common.map.things.annotations;

import net.venaglia.realms.common.map.things.PropertyAccessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: ed
 * Date: 4/17/14
 * Time: 9:02 AM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomPropertyAccessor {

    Class<? extends PropertyAccessor> value();
}
