package net.venaglia.realms.builder.geoform;

import net.venaglia.realms.builder.map.Acre;
import net.venaglia.realms.builder.map.Edge;
import net.venaglia.realms.builder.map.Sector;
import net.venaglia.realms.common.util.impl.ArrayReferenceList;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* User: ed
* Date: 1/16/13
* Time: 9:17 PM
*/
public class CartographicElementView {

    public enum Element {
        Acre, Triangle
    }

    public enum Position {
        Near, Border, Shared, Corner, Vertex
    }

    private final int subdivisions;
    private final Map<String,int[]> data;

    private CartographicElementView(int subdivisions) {
        this.subdivisions = subdivisions;
        data = init();
    }

    private Map<String,int[]> init() {
        Map<Edge,int[]> acres = new EnumMap<Edge,int[]>(Edge.class);
        int e = subdivisions / 3;
        {
            int l = e * 2 - 1;
            {
                int[] ids = new int[l];
                ids[0] = 0;
                for (int i = 1, j = 1, k = e; i < l; j++, k++) {
                    ids[i++] = k;
                    ids[i++] = j;
                }
                acres.put(Edge.AB, ids);
            }

            {
                int[] ids = new int[l];
                ids[0] = e - 1;
                for (int i = 1, s = e - 1, j = e + s - 1, k = j + s * 2 - 1; i < l; j = k + --s, k = j + s * 2 - 1) {
                    ids[i++] = j;
                    ids[i++] = k;
                }
                acres.put(Edge.BC, ids);
            }

            {
                int[] ids = new int[l];
                ids[e * 2 - 2] = 0;
                for (int i = e * 2 - 3, s = e - 1, j = e, k = j + s * 2 - 1; i >= 0; j = k + 1 + --s, k = j + s * 2 - 1) {
                    ids[i--] = j;
                    ids[i--] = k;
                }
                acres.put(Edge.CA, ids);
            }
        }

        int[] acreCorner = {
                0,
                e - 1,
                subdivisions * (subdivisions - 3) / 6
        };
        int[] acreVertex = {
                0,
                subdivisions / 3,
                subdivisions * 2 / 3,
        };
        int[] triCorner = {
                0,
                subdivisions * 2 - 2,
                subdivisions * subdivisions - 1
        };
        int[] triangles = new int[subdivisions * 3];
        {
            for (int i = 0, j = subdivisions, k = j * 3 - 1, s = 0, t = j * 2 - 1, u = t - 1;
                 i < subdivisions;
                 i++, j++, k--, s += t, t -= 2, u += t) {
                triangles[i] = i * 2;
                triangles[j] = u;
                triangles[k] = s;
            }
        }

        Map<String,int[]> data = new HashMap<String,int[]>(16);
        {
            int l = e - 1;
            for (Edge edge : Edge.values()) {
                int[] ints = acres.get(edge);
                int[] border = new int[e];
                for (int j = 0; j < e; j++) {
                    border[j] = ints[j * 2];
                }
                data.put(getKey(Element.Acre, Position.Border, edge, false), border);
                data.put(getKey(Element.Acre, Position.Border, edge, true), reverse(border));

                int[] near = new int[l];
                for (int j = 0; j < l; j++) {
                    near[j] = ints[j * 2 + 1];
                }
                data.put(getKey(Element.Acre, Position.Near, edge, false), near);
                data.put(getKey(Element.Acre, Position.Near, edge, true), reverse(near));

                int[] shared = new int[e + 1];
                for (int j = 0; j <= e; j++) {
                    shared[j] = (j + edge.ordinal() * e) % subdivisions;
                }
                data.put(getKey(Element.Acre, Position.Shared, edge, false), shared);
                data.put(getKey(Element.Acre, Position.Shared, edge, true), reverse(shared));

                int[] tri = new int[subdivisions];
                for (int j = 0, k = edge.ordinal() * subdivisions; j < subdivisions; j++) {
                    tri[j] = triangles[j + k];
                }
                data.put(getKey(Element.Triangle, Position.Border, edge, false), tri);
                data.put(getKey(Element.Triangle, Position.Border, edge, true), reverse(tri));

                data.put(getKey(Element.Acre, Position.Corner, edge, false), single(acreCorner, edge.ordinal()));
                data.put(getKey(Element.Acre, Position.Vertex, edge, false), single(acreVertex, edge.ordinal()));
                data.put(getKey(Element.Triangle, Position.Corner, edge, false), single(triCorner, edge.ordinal()));
            }

            data.put(getKey(Element.Acre, Position.Corner, null, false), acreCorner);
            data.put(getKey(Element.Acre, Position.Corner, null, true), reverse(acreCorner));

            data.put(getKey(Element.Acre, Position.Vertex, null, false), acreVertex);
            data.put(getKey(Element.Acre, Position.Vertex, null, true), reverse(acreVertex));

            data.put(getKey(Element.Triangle, Position.Corner, null, false), triCorner);
            data.put(getKey(Element.Triangle, Position.Corner, null, true), reverse(triCorner));

            data.put(getKey(Element.Triangle, Position.Border, null, false), triangles);
            data.put(getKey(Element.Triangle, Position.Border, null, true), reverse(triangles));
        }

        return data;
    }

    private int[] sequence (int count) {
        int[] seq = new int[count];
        for (int i = 0; i < count; i++) {
            seq[i] = 0;
        }
        return seq;
    }

    private int[] reverse (int[] indices) {
        indices = indices.clone();
        for (int i = 0, j = indices.length - 1, l = j / 2; i <= l; i++, j--) {
            int t = indices[i];
            indices[i] = indices[j];
            indices[j] = t;
        }
        return indices;
    }

    private int[] single (int[] array, int index) {
        return new int[]{ array[index] };
    }

    public int getSubdivisions() {
        return subdivisions;
    }

    public List<Acre> get(Acre[] elements, Position position, Edge edge, boolean reverse) {
        return getImpl(elements, Element.Acre, position, edge, reverse);
    }

    public List<Sector> get(Sector[] elements, Position position, Edge edge, boolean reverse) {
        return getImpl(elements, Element.Triangle, position, edge, reverse);
    }

    public List<Sector.Triangle> get(Sector.Triangle[] elements, Position position, Edge edge, boolean reverse) {
        return getImpl(elements, Element.Triangle, position, edge, reverse);
    }

    private <E> List<E> getImpl(E[] elements, Element element, Position position, Edge edge, boolean reverse) {
        int[] indices = data.get(getKey(element, position, edge, reverse));
        assert indices != null;
        ArrayReferenceList<E> list = ArrayReferenceList.createFor(elements, indices);
        assert list.validateIndices();
        return list;
    }

    private static String getKey(Element element, Position position, Edge edge, boolean reverse) {
        Object which = null;
        if (edge == null) {
            which = "*";
        } else {
            switch (position) {
                case Near:
                case Border:
                case Shared:
                    which = edge;
                    break;
                case Corner:
                case Vertex:
                    which = edge.toString().substring(0,1);
                    reverse = false;
                    break;
            }
        }
        return String.format("%s%s:%s:%s", reverse ? "!" : "", element, position, which);
    }

    // Static factory method

    private static final CartographicElementView[] CACHE = new CartographicElementView[64];

    public static CartographicElementView getFor(int subdivisions) {
        if (subdivisions <= 0 || subdivisions > CACHE.length) {
            throw new IllegalArgumentException();
        }
        CartographicElementView result = CACHE[subdivisions - 1];
        if (result == null) {
            synchronized (CACHE) {
                result = new CartographicElementView(subdivisions);
                CACHE[subdivisions - 1] = result;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        CartographicElementView cei = new CartographicElementView(9);
        for (String keyFormat : new String[]{ "Acre:Near:%s", "Acre:Border:%s", "Acre:Shared:%s", "Triangle:Border:%s", "Acre:Corner:%2$s", "Acre:Vertex:%2$s", "Triangle:Corner:%2$s" }) {
            for (Edge edge : Edge.values()) {
                String key = String.format(keyFormat, edge, edge.name().substring(0,1));
                System.out.printf("%20s :: %s\n", key, Arrays.toString(cei.data.get(key)));
            }
        }
    }
}
