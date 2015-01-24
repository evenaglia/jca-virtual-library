package net.venaglia.gloo.physical.geom;

/**
 * User: ed
 * Date: 1/21/15
 * Time: 8:27 AM
 */
public interface PlatonicShape<S extends PlatonicShape<S>> extends Shape<S> {

    /**
     * @return The total number of edges that make up this platonic shape.
     */
    int getEdgeCount();

    /**
     * @return The specified edge
     * @throws IllegalArgumentException if index &lt; 0 or index &gt;= facetCount();
     */
    Edge getEdge(int i);

    PlatonicBaseType getPlatanicBaseType();

    enum PlatonicBaseType {
        TETRAHEDRON,
        CUBE,
        OCTAHEDRON,
        DODECAHEDRON,
        ICOSAHEDRON
    }

    class Edge {
        public final Point a;
        public final Point b;

        public Edge(Point a, Point b) {
            this.a = a;
            this.b = b;
        }
    }
}
