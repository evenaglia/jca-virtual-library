package net.venaglia.realms.common.map.db_x;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: ed
 * Date: 2/25/13
 * Time: 5:41 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface DatabaseOptions {

    int reservedSize() default 64;

    int alignment() default 64;

    String filename() default "";

    String banner() default "";

    boolean spatial() default false;
}
