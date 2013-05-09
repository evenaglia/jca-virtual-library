package net.venaglia.realms.common.chemistry;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 5:44 PM
 */
public enum AstralElement implements Element {

    CONSTITUTION(BaseElement.SOD),
    PERSEVERENCE(BaseElement.AQUA),
    VERSATILITY(BaseElement.ETHO),
    INSTABILITY(BaseElement.PELE),
    COMPASSION(BaseElement.ZOT);

    private final BaseElement baseElement;

    private AstralElement(BaseElement baseElement) {
        this.baseElement = baseElement;
    }

    public BaseElement getBaseElement() {
        return baseElement;
    }

    public ElementalFamily getElementalFamily() {
        return ElementalFamily.ASTRAL;
    }
}
