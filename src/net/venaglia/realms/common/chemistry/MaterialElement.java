package net.venaglia.realms.common.chemistry;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 5:42 PM
 */
public enum MaterialElement implements Element {

    EARTH(BaseElement.SOD),
    WATER(BaseElement.AQUA),
    AIR(BaseElement.ETHO),
    FIRE(BaseElement.PELE),
    PLASMA(BaseElement.ZOT);

    private final BaseElement baseElement;

    private MaterialElement(BaseElement baseElement) {
        this.baseElement = baseElement;
    }

    public BaseElement getBaseElement() {
        return baseElement;
    }

    public ElementalFamily getElementalFamily() {
        return ElementalFamily.MATERIAL;
    }
}
