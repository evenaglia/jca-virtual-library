package net.venaglia.gloo.projection;

import net.venaglia.gloo.physical.bounds.Bounded;
import net.venaglia.gloo.physical.decorators.Transformation;

/**
 * User: ed
 * Date: 6/7/13
 * Time: 7:45 AM
 */
public interface Transformable extends Projectable, Bounded {

    /**
     * @return A Transformation object that may be used to apply
     *     transformations to this object.
     */
    Transformation getTransformation();
}
