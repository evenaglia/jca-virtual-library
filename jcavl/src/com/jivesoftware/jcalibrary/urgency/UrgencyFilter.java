package com.jivesoftware.jcalibrary.urgency;

import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.SlotTransformation;

import java.util.Collection;

/**
 * User: ed
 * Date: 5/12/13
 * Time: 7:47 PM
 */
public interface UrgencyFilter<T> {

    T buildBaseLine(Collection<JiveInstance> allJiveInstances);

    void apply(JiveInstance jiveInstance, SlotTransformation slotTransformation, T baseLine);
}
