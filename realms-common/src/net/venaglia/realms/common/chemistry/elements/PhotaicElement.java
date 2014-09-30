package net.venaglia.realms.common.chemistry.elements;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 8:25 PM
 *
 * Photaic elements can behave as if all or none of their ResonantSites are
 * active, but they have no mass, energy or gate. They do have a spin.
 */
public enum PhotaicElement implements Element {

    LIGHT("Lt", 0.5f), DARK("Sh", -0.5f);

    private final String abbr;
    private final PrimordialAttribute pa;

    private PhotaicElement(String abbr, float spin) {
        this.abbr = abbr;
        pa = new PrimordialAttribute(spin);
    }

    public String getAbbreviation() {
        return abbr;
    }

    public BaseElement getBaseElement() {
        return BaseElement.VOID;
    }

    public ElementalFamily getElementalFamily() {
        return ElementalFamily.PHOTAIC;
    }

    public PrimordialAttribute getPrimordialAttribute() {
        return pa;
    }

    public float getNaturalOccurrence() {
        return 10000.0f;
    }

}
