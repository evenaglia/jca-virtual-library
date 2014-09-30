package net.venaglia.realms.common.chemistry.elements;

import net.venaglia.common.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 5/17/14
 * Time: 8:26 AM
 */
public class ChemistryReport {

    private final Map<Element,String> allNames;
    private final Set<Set<Element>> tuples;

    public ChemistryReport() {
        allNames = new LinkedHashMap<Element,String>();
        addAll(allNames, PhotaicElement.values());
        addAll(allNames, BaseElement.values());
        addAll(allNames, MaterialElement.values());
        addAll(allNames, AstralElement.values());
        tuples = new HashSet<Set<Element>>();
    }

    public ChemistryReport buildTuples(List<Element> elements) {
        collectTuples(tuples, new HashSet<Element>(), elements);
        return this;
    }

    public void run() {
        List<Pair<Set<Element>,Float>> scores = new ArrayList<Pair<Set<Element>,Float>>();
        for (Set<Element> tuple : tuples) {
            float intersect = PrimordialAttribute.intersect(tuple);
            if (intersect > 0) {
                scores.add(new Pair<Set<Element>,Float>(tuple, intersect));
            }
        }
        Collections.sort(scores, Pair.<Float>compareB());
        for (Pair<Set<Element>,Float> score : scores) {
            if (score.getA().size() == 1) {
                PrimordialAttribute pa = score.getA().iterator().next().getPrimordialAttribute();
                System.out.printf("%72s   --> %22.3f  [f=%d,m=%.3f]\n",
                                  toString(score.getA(), allNames),
                                  score.getB(),
                                  pa.frequency(),
                                  pa.mass());
            } else if (score.getA().size() == 2) {
                System.out.printf("%72s   --> %22.3f\n",
                                  toString(score.getA(), allNames),
                                  score.getB());
            } else {
                System.out.printf("%72s   --> %22.3f\n",
                                  toString(score.getA(), allNames),
                                  score.getB());
            }
        }
    }

    public static void main(String[] args) {
        new ChemistryReport()
                .buildTuples(asList(PhotaicElement.values(), MaterialElement.values()))
                .buildTuples(asList(PhotaicElement.values(), AstralElement.values()))
                .run();
    }

    private static List<Element> asList(Element[]... elements) {
        List<Element> combined = new ArrayList<Element>();
        for (Element[] element : elements) {
            Collections.addAll(combined, element);
        }
        return combined;
    }

    private static String toString(Set<Element> tuple, Map<Element,String> allNames) {
        StringBuilder buffer = new StringBuilder();
        for (Element element : tuple) {
            if (buffer.length() > 0) buffer.append(" x ");
            String name = allNames.get(element);
            buffer.append(name).append("            ".substring(name.length()));
        }
        return buffer.toString();
    }

    private static <E extends Enum & Element> void addAll(Map<Element,String> allNames, E[] elements) {
        for (E element : elements) {
            allNames.put(element, element.name());
        }
    }

    private static void collectTuples(Set<Set<Element>> tuples, Set<Element> base, List<Element> elements) {
        for (Element e : elements) {
            if (!base.contains(e)) {
                Set<Element> tuple = new HashSet<Element>(base);
                tuple.add(e);
                tuples.add(tuple);
                collectTuples(tuples, tuple, elements);
            }
        }
    }
}
