package net.venaglia.realms.common.chemistry.impl;

import net.venaglia.realms.common.chemistry.AstralElement;
import net.venaglia.realms.common.chemistry.Composite;
import net.venaglia.realms.common.chemistry.Element;
import net.venaglia.realms.common.chemistry.MaterialElement;
import net.venaglia.realms.common.chemistry.PhotaicElement;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 8:16 PM
 */
public class AbstractComposite implements Composite {

    protected static final int CONSTITUTION = 0;
    protected static final int PERSEVERENCE = 1;
    protected static final int VERSATILITY  = 2;
    protected static final int INSTABILITY  = 3;
    protected static final int COMPASSION   = 4;

    protected static final int EARTH        = 5;
    protected static final int WATER        = 6;
    protected static final int AIR          = 7;
    protected static final int FIRE         = 8;
    protected static final int PLASMA       = 9;

    protected static final int LIGHT        = 10;
    protected static final int DARK         = 11;

    protected static final Map<Element,Integer> ELEMENT_INDEX;

    static  {
        Map<Element,Integer> elementIndex = new HashMap<Element,Integer>();
        elementIndex.put(AstralElement.CONSTITUTION, CONSTITUTION);
        elementIndex.put(AstralElement.PERSEVERENCE, PERSEVERENCE);
        elementIndex.put(AstralElement.VERSATILITY, VERSATILITY);
        elementIndex.put(AstralElement.INSTABILITY, INSTABILITY);
        elementIndex.put(AstralElement.COMPASSION, COMPASSION);
        elementIndex.put(MaterialElement.EARTH, EARTH);
        elementIndex.put(MaterialElement.WATER, WATER);
        elementIndex.put(MaterialElement.AIR, AIR);
        elementIndex.put(MaterialElement.FIRE, FIRE);
        elementIndex.put(MaterialElement.PLASMA, PLASMA);
        elementIndex.put(PhotaicElement.LIGHT, LIGHT);
        elementIndex.put(PhotaicElement.DARK, DARK);
        ELEMENT_INDEX = Collections.unmodifiableMap(elementIndex);
    }

    private float[] values = new float[12];

    protected void importFrom(Composite composite) {
        for (Element element : ELEMENT_INDEX.keySet()) {
            setComponent(element, composite.getComponent(element));
        }
    }

    protected void setComponent(Element element, float value) {
        Integer index = ELEMENT_INDEX.get(element);
        if (index != null) {
            values[index] = value;
        }
    }

    public float getComponent(Element element) {
        Integer index = ELEMENT_INDEX.get(element);
        return index != null ? values[index] : 0;
    }

    public float getComponentTotal() {
        return 0;
    }

    public float getPotential() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
