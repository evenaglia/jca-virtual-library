package net.venaglia.realms.common.projection;

import net.venaglia.realms.common.physical.bounds.Bounded;
import net.venaglia.realms.common.physical.decorators.Material;

/**
 * User: ed
 * Date: 9/28/12
 * Time: 7:36 AM
 */
public interface DisplayList extends Projectable, Bounded {

    public interface DisplayListRecorder {
        void record(GeometryBuffer buffer);
    }

    int getGlDisplayListId();

    Material getInitialMaterial();

    void setInitialMaterial(Material initialMaterial);

    void record(DisplayListRecorder recorder) throws TooManyDisplayListsException, IllegalArgumentException;

    void recordAndExecute(DisplayListRecorder recorder) throws TooManyDisplayListsException, IllegalArgumentException;

    void deallocate();
}
