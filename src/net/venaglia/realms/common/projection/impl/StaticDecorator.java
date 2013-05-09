package net.venaglia.realms.common.projection.impl;

import net.venaglia.realms.common.projection.Decorator;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 9/2/12
* Time: 12:17 PM
* To change this template use File | Settings | File Templates.
*/
public abstract class StaticDecorator implements Decorator {

    public final boolean isStatic() {
        return true;
    }

    public final void apply(long nowMS, GeometryBuffer buffer) {
        apply(buffer);
    }

    protected abstract void apply(GeometryBuffer buffer);
}
