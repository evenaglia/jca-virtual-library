package net.venaglia.gloo.util.impl;

import net.venaglia.gloo.util.OpenGLObject;

/**
 * User: ed
 * Date: 1/7/15
 * Time: 9:44 AM
 */
public abstract class AbstractOpenGLObject implements OpenGLObject {

    protected final String type;

    public AbstractOpenGLObject(String type) {
        if (type == null) throw new NullPointerException("type");
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractOpenGLObject that = (AbstractOpenGLObject)o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "OpenGLObject<" + type + ">";
    }
}
