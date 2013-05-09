package net.venaglia.realms.common.projection.impl;

import net.venaglia.realms.common.projection.Decorator;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 12:23 PM
 */
public abstract class DynamicDecorator implements Decorator {

    public boolean isStatic() {
        return false;
    }
}
