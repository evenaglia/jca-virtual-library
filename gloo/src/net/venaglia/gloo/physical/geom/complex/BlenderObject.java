package net.venaglia.gloo.physical.geom.complex;

import net.venaglia.gloo.demo.SingleShapeDemo;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.physical.texture.TextureCoordinate;
import net.venaglia.gloo.physical.texture.mapping.SequenceMapping;
import net.venaglia.gloo.projection.GeometryBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ed
 * Date: 5/28/13
 * Time: 12:05 AM
 */
public class BlenderObject extends AbstractShape<BlenderObject> {

    public enum NormalSource {
        FROM_FILE, PER_TRIANGLE, SMOOTH
    }

    private final Vector[] normals;
    private final int[] trianglePoints;
    private final int[] triangleNormals;

    public BlenderObject(Reader reader) throws IOException {
        this(reader, NormalSource.SMOOTH, null);
    }

    public BlenderObject(Reader reader, SequenceMapping.Recorder recorder) throws IOException {
        this(reader, NormalSource.SMOOTH, recorder);
    }

    public BlenderObject(Reader reader, NormalSource normalSource) throws IOException {
        this(reader, normalSource, null);
    }

    public BlenderObject(Reader reader, NormalSource normalSource, SequenceMapping.Recorder recorder) throws IOException {
        this(parse(new BufferedReader(reader), normalSource, recorder));
    }

    private BlenderObject(ParsedObject parse) {
        this(parse.vertices,
             parse.normals,
             parse.faceVertices,
             parse.faceNormals);
    }

    private BlenderObject(Point[] vertices, Vector[] normals, int[] trianglePoints, int[] triangleNormals) {
        super(vertices);
        this.normals = normals;
        this.trianglePoints = trianglePoints;
        this.triangleNormals = triangleNormals;
    }

    @Override
    protected BlenderObject build(Point[] points, XForm xForm) {
        return new BlenderObject(points, xForm.apply(normals), trianglePoints, triangleNormals);
    }

    public BlenderObject flip() {
        Point[] points = this.points.clone();
        Vector[] normals = this.normals.clone();
        for (int i = 0, l = normals.length; i < l; i++) {
            normals[i] = normals[i].reverse();
        }
        int[] trianglePoints = this.trianglePoints.clone();
        for (int i = 0, l = trianglePoints.length; i < l; i += 3) {
            int t = trianglePoints[i];
            trianglePoints[i] = trianglePoints[i + 1];
            trianglePoints[i + 1] = t;
        }
        int[] triangleNormals = this.triangleNormals.clone();
        for (int i = 0, l = triangleNormals.length; i < l; i += 3) {
            int t = triangleNormals[i];
            triangleNormals[i] = triangleNormals[i + 1];
            triangleNormals[i + 1] = t;
        }
        return new BlenderObject(points, normals, trianglePoints, triangleNormals);
    }

    public Vector getNormal(int index) {
        return normals[index];
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLES);
        for (int a = 0; a < trianglePoints.length; a += 3) {
            buffer.normal(normals[triangleNormals[a]]);
            buffer.vertex(points[trianglePoints[a]]);
            int b = a + 1;
            buffer.normal(normals[triangleNormals[b]]);
            buffer.vertex(points[trianglePoints[b]]);
            int c = a + 2;
            buffer.normal(normals[triangleNormals[c]]);
            buffer.vertex(points[trianglePoints[c]]);
        }
        buffer.end();
    }

    private static ParsedObject parse(BufferedReader reader,
                                      NormalSource normalSource,
                                      SequenceMapping.Recorder recorder) throws IOException {
        if (normalSource == null) {
            throw new NullPointerException("normalSource");
        }
        ParseData parseData = new ParseData(normalSource, recorder);
        Pattern match = Pattern.compile("^(v[nt]?|f)\\s+(.*)");
        int lineNumber = 0;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            lineNumber++;
            Matcher matcher = match.matcher(line);
            if (matcher.find()) {
                String type = matcher.group(1);
                String[] data = matcher.group(2).split("\\s+");
                try {
                    parseLine(parseData, type, data);
                } catch (Exception e) {
                    throw new IOException("Unable to parse blender file: exception thrown on line " + lineNumber, e);
                }
            }
        }
        ParsedObject parsedObject = new ParsedObject();
        if (parseData.normals == null) {
            int capacity = normalSource == NormalSource.PER_TRIANGLE
                           ? parseData.faces.size()
                           : parseData.vertices.size();
            parseData.normals = new ArrayList<Vector>(capacity);
        }
        collectNormals(normalSource, parseData, parsedObject);
        recordCoordinates(recorder, parseData.faces, parseData.textureCoordinates);
        parsedObject.vertices = parseData.vertices.toArray(new Point[parseData.vertices.size()]);
        parsedObject.normals = parseData.normals.toArray(new Vector[parseData.normals.size()]);
        return parsedObject;
    }

    private static void parseLine(ParseData parseData,
                                  String type,
                                  String[] data) {
        if ("v".equals(type) && data.length >= 3) {
            parseData.vertices.add(new Point(Double.parseDouble(data[0]),
                                             Double.parseDouble(data[1]),
                                             Double.parseDouble(data[2])));
        } else {
            if ("f".equals(type) && data.length >= 3) {
                Face f = new Face(data[0].split("/"),
                                  data[2].split("/"), // flip triangle order for GL compatibility
                                  data[1].split("/"),
                                  parseData.normalsFromFile,
                                  parseData.textureCoordinatesFromFile);
                parseData.faces.add(f);
                if (parseData.facesByPoint != null) {
                    accumulateTrianglesByPoint(f.p1, f, parseData.facesByPoint);
                    accumulateTrianglesByPoint(f.p2, f, parseData.facesByPoint);
                    accumulateTrianglesByPoint(f.p3, f, parseData.facesByPoint);
                }
            } else if (parseData.normalsFromFile && "vn".equals(type) && data.length >= 3) {
                parseData.normals.add(new Vector(Double.parseDouble(data[0]),
                                                 Double.parseDouble(data[1]),
                                                 Double.parseDouble(data[2])));
            } else if (parseData.textureCoordinatesFromFile && "vt".equals(type) && data.length >= 2) {
                parseData.textureCoordinates.add(new TextureCoordinate((float)Double.parseDouble(data[0]),
                                                                       (float)Double.parseDouble(data[1])));
            }
        }
    }

    private static void collectNormals(NormalSource normalSource,
                                       ParseData parseData,
                                       ParsedObject parsedObject) {
        List<Face> faces = parseData.faces;
        int k = 0;
        switch (normalSource) {
            case FROM_FILE:
                parsedObject.faceVertices = new int[faces.size() * 3];
                parsedObject.faceNormals = new int[faces.size() * 3];
                for (Face face : faces) {
                    parsedObject.faceVertices[k] = face.p1;
                    parsedObject.faceVertices[k + 1] = face.p2;
                    parsedObject.faceVertices[k + 2] = face.p3;
                    parsedObject.faceNormals[k++] = face.n1;
                    parsedObject.faceNormals[k++] = face.n2;
                    parsedObject.faceNormals[k++] = face.n3;
                }
                break;
            case PER_TRIANGLE:
                parsedObject.faceVertices = new int[faces.size() * 3];
                parsedObject.faceNormals = new int[faces.size() * 3];
                for (Face face : faces) {
                    parsedObject.faceVertices[k] = face.p1;
                    parsedObject.faceVertices[k + 1] = face.p2;
                    parsedObject.faceVertices[k + 2] = face.p3;
                    int n = computeFacetNormal(face, parseData.vertices, parseData.normals);
                    parsedObject.faceNormals[k++] = n;
                    parsedObject.faceNormals[k++] = n;
                    parsedObject.faceNormals[k++] = n;
                }
                break;
            case SMOOTH:
                parsedObject.faceVertices = new int[faces.size() * 3];
                parsedObject.faceNormals = parsedObject.faceVertices;
                for (Face face : faces) {
                    parsedObject.faceVertices[k++] = face.p1;
                    parsedObject.faceVertices[k++] = face.p2;
                    parsedObject.faceVertices[k++] = face.p3;
                }
                for (int i = 0, l = parseData.vertices.size(); i < l; i++) {
                    parseData.normals.add(computeAverageNormal(parseData.facesByPoint.get(i), parseData.vertices));
                }
                break;
        }
    }

    private static void recordCoordinates(SequenceMapping.Recorder recorder,
                                          List<Face> faces,
                                          List<TextureCoordinate> textureCoordinates) {
        if (recorder != null) {
            for (Face face : faces) {
                recorder.add(textureCoordinates.get(face.c1));
                recorder.add(textureCoordinates.get(face.c2));
                recorder.add(textureCoordinates.get(face.c3));
            }
        }
    }

    private static void accumulateTrianglesByPoint(Integer point,
                                                   Face face,
                                                   Map<Integer, List<Face>> trianglesByPoint) {
        List<Face> faces = trianglesByPoint.get(point);
        if (faces == null) {
            faces = new ArrayList<Face>(6);
            trianglesByPoint.put(point, faces);
        }
        faces.add(face);
    }

    private static int computeFacetNormal(Face face, List<Point> vertices, List<Vector> normals) {
        Point v1 = vertices.get(face.p1);
        Point v2 = vertices.get(face.p2);
        Point v3 = vertices.get(face.p3);
        int n = normals.size();
        normals.add(Vector.cross(v1, v2, v3).normalize());
        return n;
    }

    private static Vector computeAverageNormal(List<Face> faces, List<Point> vertices) {
        double i = 0, j = 0, k = 0;
        for (Face face : faces) {
            Point v1 = vertices.get(face.p1);
            Point v2 = vertices.get(face.p2);
            Point v3 = vertices.get(face.p3);
            Vector normal = Vector.cross(v1, v2, v3).normalize();
            i += normal.i;
            j += normal.j;
            k += normal.k;
        }
        return new Vector(i, j, k).normalize();
    }

    private static class ParseData {
        boolean normalsFromFile;
        boolean textureCoordinatesFromFile;
        List<Point> vertices;
        List<Vector> normals;
        List<Face> faces;
        List<TextureCoordinate> textureCoordinates;
        Map<Integer,List<Face>> facesByPoint;

        ParseData(NormalSource normalSource, SequenceMapping.Recorder recorder) {
            normalsFromFile = normalSource == NormalSource.FROM_FILE;
            textureCoordinatesFromFile = recorder != null;
            vertices = new ArrayList<Point>();
            normals = normalsFromFile ? new ArrayList<Vector>() : null;
            faces = new ArrayList<Face>();
            textureCoordinates = recorder == null ? null : new ArrayList<TextureCoordinate>();
            facesByPoint = normalSource == NormalSource.SMOOTH
                                           ? new HashMap<Integer,List<Face>>()
                                           : null;
        }
    }

    private static class ParsedObject {
        Point[] vertices;
        Vector[] normals;
        int[] faceVertices;
        int[] faceNormals;
    }

    private static class Face {
        int p1, p2, p3;
        int c1, c2, c3;
        int n1, n2, n3;

        Face(String[] a, String[] b, String[] c, boolean expectNormals, boolean textureCoordinatesFromFile) {
            p1 = parseInt(a[0]) - 1;
            p2 = parseInt(b[0]) - 1;
            p3 = parseInt(c[0]) - 1;
            if (textureCoordinatesFromFile) {
                c1 = parseInt(a[1]) - 1;
                c2 = parseInt(b[1]) - 1;
                c3 = parseInt(c[1]) - 1;
            } else {
                c1 = c2 = c3 = -1;
            }
            if (expectNormals) {
                n1 = parseInt(a[2]) - 1;
                n2 = parseInt(b[2]) - 1;
                n3 = parseInt(c[2]) - 1;
            } else {
                n1 = n2 = n3 = -1;
            }
        }

        private int parseInt(String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String[] modelNames = {
                "bunny",
                "wall",
                "wall-cap",
                "post",
                "post-cap"
        };
        String modelName = modelNames[4];
        InputStream objIn = Thread.currentThread().getContextClassLoader().getResourceAsStream("models/" + modelName + ".model");
//        NormalSource normalSource = NormalSource.SMOOTH;
        NormalSource normalSource = NormalSource.FROM_FILE;
        BlenderObject object = new BlenderObject(new InputStreamReader(objIn), normalSource);
//        object = object.scale(new Vector(-1, 1, 1));
//        object = object.rotate(Axis.X, Math.PI * 0.5)
//        object = object.translate(Vector.Z.scale(-4.5));
        object = object.translate(Vector.Z.scale(-0.6));
//        Color color = new Color(0.25f, 0.15f, 0.075f);
        Color color = new Color(1.0f, 0.9f, 0.75f);
        new SingleShapeDemo(object, color, SingleShapeDemo.Mode.SHADED).start();
    }
}
