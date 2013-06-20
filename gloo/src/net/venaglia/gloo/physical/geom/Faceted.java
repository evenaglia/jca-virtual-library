package net.venaglia.gloo.physical.geom;

/**
 * User: ed
 * Date: 9/1/12
 * Time: 5:03 PM
 */
public interface Faceted {

    /**
     * @return The total number of facets that make up this shape.
     */
    int facetCount();

    /**
     * @return The type of facet this shape decomposes into. May be {@link Facet.Type#MIXED} if this shape decomposes into more than one type of facets.
     */
    Facet.Type getFacetType();

    /**
     * @return The specified facet; points are in CCW order.
     * @throws IllegalArgumentException if index &lt; 0 or index &gt;= facetCount();
     */
    Facet getFacet(int index);
}
