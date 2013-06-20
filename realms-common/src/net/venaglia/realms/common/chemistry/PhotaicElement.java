package net.venaglia.realms.common.chemistry;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 8:25 PM
 */
public enum PhotaicElement implements Element {

    LIGHT, DARK;

    public BaseElement getBaseElement() {
        return BaseElement.VOID;
    }

    public ElementalFamily getElementalFamily() {
        return ElementalFamily.PHOTAIC;
    }


}
