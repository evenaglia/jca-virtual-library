package net.venaglia.realms.common.chemistry;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 8:05 PM
 */
public interface Composite {

    float getComponent(Element element);

    float getComponentTotal();

    float getPotential();
}
