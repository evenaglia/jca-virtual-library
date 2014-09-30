package net.venaglia.realms.common.chemistry.elements;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 5:42 PM
 */
public enum MaterialElement implements Element {

    EARTH ("E", BaseElement.SOD , 1000.0f),
    WATER ("W", BaseElement.AQUA,  650.0f),
    AIR   ("A", BaseElement.ETHO,  350.0f),
    FIRE  ("F", BaseElement.PELE,   85.0f),
    PLASMA("P", BaseElement.ZOT ,   25.0f);

    private final String abbr;
    private final BaseElement baseElement;
    private final float naturalOccurrence;
    private final PrimordialAttribute pa;

    private MaterialElement(String abbr, BaseElement baseElement, float naturalOccurrence) {
        this.abbr = abbr;
        this.baseElement = baseElement;
        this.naturalOccurrence = naturalOccurrence;
        ResonanceSite p = baseElement.getProp();
        this.pa = new PrimordialAttribute(p, -1, 2);
    }

    public String getAbbreviation() {
        return abbr;
    }

    public BaseElement getBaseElement() {
        return baseElement;
    }

    public ElementalFamily getElementalFamily() {
        return ElementalFamily.MATERIAL;
    }

    public PrimordialAttribute getPrimordialAttribute() {
        return pa;
    }

    public float getNaturalOccurrence() {
        return naturalOccurrence;
    }
}
