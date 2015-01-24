package net.venaglia.gloo.util.impl;

/**
 * User: ed
 * Date: 1/7/15
 * Time: 9:44 AM
 */
public class SimpleOpenGLObject extends AbstractOpenGLObject {

    protected final int id;

    public SimpleOpenGLObject(String type, int id) {
        super(type);
        this.id = id;
    }

    @Override
    public int getOpenGLObjectId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        SimpleOpenGLObject that = (SimpleOpenGLObject)o;
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + id;
        return result;
    }

    @Override
    public String toString() {
        return "OpenGLObject<" + type + ">[id=" + id + "]";
    }
}
