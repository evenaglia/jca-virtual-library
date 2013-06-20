package net.venaglia.gloo.projection;

import net.venaglia.gloo.physical.bounds.Bounded;
import net.venaglia.gloo.physical.decorators.Material;

/**
 * User: ed
 * Date: 9/28/12
 * Time: 7:36 AM
 */
public interface DisplayList extends Projectable, Bounded {

    int getGlDisplayListId();

    Material getInitialMaterial();

    void setInitialMaterial(Material initialMaterial);

    void record(GeometryRecorder recorder) throws TooManyDisplayListsException, IllegalArgumentException;

    void deallocate();

    Transformable transformable();
}
