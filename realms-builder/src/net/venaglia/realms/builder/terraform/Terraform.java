package net.venaglia.realms.builder.terraform;

import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.realms.common.map_x.WorldMap;
import net.venaglia.realms.common.map_x.elements.DetailAcre;
import net.venaglia.realms.common.map_x.elements.GraphAcre;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.common.util.Ref;
import net.venaglia.common.util.Tuple2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 7:12 PM
 */
public class Terraform implements FlowObserver {

    private final Iterable<? extends FlowQuery> queries;
    private final FlowSimulator flowSimulator;

    public Terraform(WorldMap worldMap) {
        this.queries = buildQueries(worldMap);
        this.flowSimulator = new FlowSimulator(GeoSpec.APPROX_RADIUS_METERS.get(), 5000, 60, 10.0);
        flowSimulator.setObserver(this);
    }

    private Iterable<Query> buildQueries(WorldMap worldMap) {
        int count = (int)GeoSpec.ACRES.get();
        List<Query> queries = new ArrayList<Query>(count);
        WorldMap.Database<GraphAcre> database = worldMap.graph;
        for (Tuple2<Integer,GraphAcre> tuple : database) {
            queries.add(new Query(tuple.getB()));
        }
        return queries;
    }

    public void run() {
        flowSimulator.run();
    }

    public void frame(FlowQueryInterface queryInterface) {
        queryInterface.query(queries);
    }

    private static class Query implements FlowQuery {

        private final GraphAcre graphAcre;
        private final Point center;
        private final GraphAcre[] neighbors;
        private final Point[] vertices;
        private final double[] vertexDistances;
        private final double[] sectorArea;
        private final double totalArea;

        private Query(GraphAcre graphAcre) {
            this.graphAcre = graphAcre;
            Collection<Ref<GraphAcre>> neighborRefs = graphAcre.getNeighbors();
            GeoPoint[] myVertices = graphAcre.getVertices();
            int vertexCount = myVertices.length;
            this.neighbors = new GraphAcre[vertexCount];
            this.vertices = new Point[vertexCount];
            this.sectorArea = new double[vertexCount];
            double totalArea = 0;
            this.vertexDistances = new double[vertexCount];
            double radii[] = new double[vertexCount];
            int idx = 0;
            double radius = GeoSpec.APPROX_RADIUS_METERS.get();
            center = graphAcre.getCenter().toPoint(radius);
            for (Ref<GraphAcre> neighborRef : neighborRefs) {
                GraphAcre neighbor = neighborRef.get();
                this.neighbors[idx] = neighbor;
                Point v = myVertices[idx].toPoint(radius);
                this.vertices[idx] = v;
                radii[idx] = Vector.computeDistance(center, v);
                idx++;
            }
            for (idx = 0; idx < vertexCount; idx++) {
                int next = (idx + 1) % vertexCount;
                double a = radii[idx];
                double b = radii[next];
                double c = vertexDistances[idx] = Vector.computeDistance(this.vertices[idx], this.vertices[next]);
                totalArea += sectorArea[idx] = triangleArea(a, b, c);
            }
            this.totalArea = totalArea;
            // typical 1 acre hex: side 40m, long width 80m, short height 70m
        }

        public GeoPoint getPoint() {
            return graphAcre.getCenter();
        }

        public void processDataForPoint(FlowPointData data) {
            Point p = center.translate(data.getMagnitude());
            int l = neighbors.length;
            double[] spokes = new double[l];
            for (int i = 0; i < l;) {
                Point c = vertices[i];
                spokes[i] = Vector.computeDistance(p, c);
            }
            for (int i = 0; i < l;) {
                int j = (i + 1) % l;
                double area = triangleArea(spokes[i], spokes[j], vertexDistances[i]);
                double drawAmount = (area - sectorArea[i]) / (totalArea - sectorArea[i]);
                draw(neighbors[i].getDetailAcre().get(), graphAcre.getDetailAcre().get(), drawAmount);
            }
            computeRoughness(data.getPressure());
        }

        private float computeRoughness(double pressure) {
            double roughness = Math.exp(pressure) * 0.10;
            return (float)roughness;
        }

        private void draw(DetailAcre from, DetailAcre to, double amount) {
            // todo
        }

        private double triangleArea(double a, double b, double c) {
            double s = (a + b + c) * 0.5;
            return Math.sqrt(s * (s - a) * (s - b) * (s - c));
        }
    }

    public static void main(String[] args) {
        new Terraform(new WorldMap()).run();
    }
}
