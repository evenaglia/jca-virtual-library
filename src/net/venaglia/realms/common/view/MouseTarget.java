package net.venaglia.realms.common.view;

import net.venaglia.realms.common.projection.Projectable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 3/30/13
 * Time: 9:46 PM
 */
public class MouseTarget<V> {

    private static final AtomicInteger NAME_SEQ = new AtomicInteger(1);

    private final int glName = NAME_SEQ.getAndIncrement();

    protected Projectable projectableObject;
    protected MouseTargetEventListener<? super V> listener;
    protected V value;
    protected String name;
    protected MouseTargets children;

    public MouseTarget() {
    }

    public MouseTarget(Projectable projectableObject, MouseTargetEventListener<? super V> listener, V value) {
        this(projectableObject, listener, value, null);
    }

    public MouseTarget(Projectable projectableObject, MouseTargetEventListener<? super V> listener, V value, String name) {
        this.projectableObject = projectableObject;
        this.value = value;
        this.listener = listener;
        this.name = name == null ? "@" + System.identityHashCode(this) : name;
    }

    public final int getGlName() {
        return glName;
    }

    public Projectable getProjectableObject() {
        return projectableObject;
    }

    public void setProjectableObject(Projectable projectableObject) {
        this.projectableObject = projectableObject;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public MouseTargetEventListener<? super V> getListener() {
        return listener;
    }

    public void setListener(MouseTargetEventListener<? super V> listener) {
        this.listener = listener;
    }

    public void setChildren(MouseTargets children) {
        this.children = children;
    }

    public MouseTargets getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return String.format("MouseTarget[%s]", name);
    }
}
