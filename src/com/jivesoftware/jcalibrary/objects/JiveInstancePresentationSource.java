package com.jivesoftware.jcalibrary.objects;

import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.detail.DynamicDetailSource;
import net.venaglia.realms.common.physical.geom.primitives.Box;

/**
 * User: ed
 * Date: 5/11/13
 * Time: 9:16 PM
 */
public class JiveInstancePresentationSource implements DynamicDetailSource<JiveInstancePresentation> {

    private final Box target = new Box();

    @Override
    public float getSizeFactor() {
        return 0.5f;
    }

    @Override
    public JiveInstancePresentation produceAt(DetailLevel detailLevel) {
        return new JiveInstancePresentation(detailLevel);
    }

    @Override
    public Shape<?> getTarget() {
        return target;
    }
}
