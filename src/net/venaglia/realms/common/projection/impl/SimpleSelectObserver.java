package net.venaglia.realms.common.projection.impl;

import net.venaglia.realms.common.projection.SelectObserver;

/**
* User: ed
* Date: 4/11/13
* Time: 5:23 PM
*/
public class SimpleSelectObserver implements SelectObserver {

    private int closestDepth;
    private int closestName;

    public SimpleSelectObserver() {
        reset();
    }

    public void reset() {
        closestName = Integer.MIN_VALUE;
        closestDepth = Integer.MAX_VALUE;
    }

    public void selected(int name, int depth) {
        if (depth < closestDepth) {
            closestDepth = depth;
            closestName = name;
        }
    }

    public int getClosestName() {
        return closestName;
    }
}
