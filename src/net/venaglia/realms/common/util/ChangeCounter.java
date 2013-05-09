package net.venaglia.realms.common.util;

import java.util.EmptyStackException;

/**
 * User: ed
 * Date: 4/27/13
 * Time: 9:31 AM
 */
public class ChangeCounter {

    private long count = 0;
    private long current = 0;
    private Node stack;
    private Node unstack; // used to store unused stack objects to prevent object churn

    public ChangeCounter() {
        this.current = -1;
    }

    public void increment() {
        count++;
    }

    public boolean isCurrent() {
        return current == count;
    }

    public void setCurrent() {
        if (current != count) {
            current = count;
            Node s = stack;
            while (s != null) {
                s.current = false;
                s = s.previous;
            }
        }
    }

    public void push() {
        Node top;
        if (unstack != null) {
            top = unstack;
            unstack = top.previous;
        } else {
            top = new Node();
        }
        top.previous = stack;
        top.current = count == current;
        stack = top;
    }

    public void pop() {
        if (stack == null) {
            throw new EmptyStackException();
        }
        if (stack.current) {
            current = count;
        }
        Node top = stack;
        stack = top.previous;
        top.previous = unstack;
        unstack = top;
    }

    private static final class Node {
        private Node previous;
        private boolean current;
    }
}
