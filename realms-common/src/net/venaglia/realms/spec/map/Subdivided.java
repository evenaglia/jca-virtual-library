package net.venaglia.realms.spec.map;

/**
 * User: ed
 * Date: 7/17/12
 * Time: 9:46 PM
 *
 * Interface implemented by objects representing geographic areas, which are
 * further subdivided into smaller elements.
 * @param <S> The type of sub-element that composes this one.
 */
public interface Subdivided<S> {

    S findSubElement(GeoPoint point);
}
