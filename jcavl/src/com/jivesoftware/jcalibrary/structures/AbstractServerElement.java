package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.objects.AbstractLibraryElement;
import com.jivesoftware.jcalibrary.objects.Objects;

/**
 * User: ed
 * Date: 5/3/13
 * Time: 9:21 PM
 */
public abstract class AbstractServerElement<T extends AbstractServerElement<T>> extends AbstractLibraryElement<T> {

    protected final Objects obj;

    protected AbstractServerElement(Objects obj) {
        this.obj = obj;
    }
}
