package com.jivesoftware.jcalibrary.objects;

import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.physical.geom.Element;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;
import net.venaglia.realms.common.view.MouseTarget;

/**
 * User: ed
 * Date: 4/23/13
 * Time: 8:08 AM
 */
public abstract class AbstractLibraryElement<T extends AbstractLibraryElement<T>> implements Projectable {

    protected Transformation transformation;

    public Transformation getTransformation() {
        if (transformation == null) {
            transformation = new Transformation();
        }
        return transformation;
    }

    protected <E extends Element<E>> E[] transform(E[] elements, XForm xForm) {
        elements = elements.clone();
        for (int i = 0; i < elements.length; i++) {
            elements[i] = elements[i] == null ? null : elements[i].transform(xForm);
        }
        return elements;
    }

    public boolean isStatic() {
        return true;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        if (transformation != null) {
            buffer.pushTransform();
            transformation.apply(nowMS, buffer);
            projectImpl(nowMS, buffer);
            buffer.popTransform();
        } else {
            projectImpl(nowMS, buffer);
        }
    }

    protected abstract void projectImpl(long nowMS, GeometryBuffer buffer);

    public abstract MouseTarget<T> getMouseTarget();
}
