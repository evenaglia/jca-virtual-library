package net.venaglia.realms.common.chemistry.compounds;

import net.venaglia.common.util.Ref;
import net.venaglia.common.util.impl.AbstractCachingRef;
import net.venaglia.realms.common.chemistry.elements.AstralElement;
import net.venaglia.realms.common.chemistry.elements.Element;
import net.venaglia.realms.common.chemistry.elements.ElementalFamily;
import net.venaglia.realms.common.chemistry.elements.MaterialElement;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * User: ed
 * Date: 5/21/14
 * Time: 7:46 PM
 */
public class AllCompounds {

    public static final Ref<AllCompounds> INSTANCE = new AbstractCachingRef<AllCompounds>() {
        @Override
        protected AllCompounds getImpl() {
            return new AllCompounds();
        }
    };

    private Map<Formula<?>,Compound<?>> unabridged = new LinkedHashMap<Formula<?>,Compound<?>>();

    private AllCompounds() {
        // lazily instantiated singleton
        CompoundFactory<MaterialElement> materialFactory = new CompoundFactory<MaterialElement>(ElementalFamily.MATERIAL) {
            @Override
            Compound<MaterialElement> create(Formula<MaterialElement> formula, String name) {
                return new MaterialCompound(formula, name);
            }
        };
        permutateAndMerge(MaterialCompound.PRIMARY_COMPOUNDS, materialFactory);
        CompoundFactory<AstralElement> astralFactory = new CompoundFactory<AstralElement>(ElementalFamily.ASTRAL) {
            @Override
            Compound<AstralElement> create(Formula<AstralElement> formula, String name) {
                return new AstralCompound(formula, name);
            }
        };
        permutateAndMerge(AstralCompound.PRIMARY_COMPOUNDS, astralFactory);
    }

    private <E extends Element> void permutateAndMerge(Compound<E>[] values, CompoundFactory<E> factory) {
        List<Compound<E>> compounds = new ArrayList<Compound<E>>();
        List<Formula<E>> formulae2 = new ArrayList<Formula<E>>();
        List<Formula<E>> formulae3 = new ArrayList<Formula<E>>();
        for (Compound<E> value : values) {
            Formula<E> formula = value.getFormula();
            switch (formula.size()) {
                case 2:
                    formulae2.add(formula);
                    compounds.add(value);
                    break;
                case 3:
                    formulae3.add(formula);
                    compounds.add(value);
                    break;
            }
        }
        List<Formula<E>> formulae5 = permutate(formulae2, formulae3);
        List<Formula<E>> formulae8 = permutate(formulae3, formulae5);
        List<Formula<E>> formulae13 = permutate(formulae5, formulae8);
        for (Formula<E> formula : formulae5) {
            compounds.add(factory.create(formula));
        }
        for (Formula<E> formula : formulae8) {
            compounds.add(factory.create(formula));
        }
        for (Formula<E> formula : formulae13) {
            compounds.add(factory.create(formula));
        }
        for (Compound<E> compound : compounds) {
            Formula<E> formula = compound.getFormula();
            unabridged.put(formula, compound);
        }
    }

    private <E extends Element> List<Formula<E>> permutate(List<Formula<E>> aSet, List<Formula<E>> bSet) {
        List<Formula<E>> cSet = new ArrayList<Formula<E>>(aSet.size() * bSet.size());
        for (Formula<E> a : aSet) {
            for (Formula<E> b : bSet) {
                cSet.add(Formula.combine(a, b));
            }
        }
        return cSet;
    }

    private abstract static class CompoundFactory<E extends Element> {

        private final Properties compoundNames;

        protected CompoundFactory(ElementalFamily family) {
            this.compoundNames = new Properties();
            String filename = family.name().toLowerCase() + "-compound-names.properties";
            try {
                compoundNames.load(getClass().getResourceAsStream(filename));
            } catch (IOException e) {
                throw new RuntimeException("Unable to load " + filename, e);
            }
        }

        Compound<E> create(Formula<E> formula) {
            String formulaString = formula.getFormulaString();
            String key = formulaString.replaceAll("[~()]+", ".");
            String name = compoundNames.getProperty(key);
            return create(formula, name);
        }

        abstract Compound<E> create(Formula<E> formula, String name);
    }

    public static void main(String[] args) throws Exception {
        AllCompounds allCompounds = AllCompounds.INSTANCE.get();
        for (ElementalFamily target : new ElementalFamily[]{ ElementalFamily.MATERIAL, ElementalFamily.ASTRAL }) {
            FileWriter out = new FileWriter(target.name().toLowerCase() + "-compounds.csv");
            out.write("\"Seq\",\"Formula\",\"Name\",\"Mass\",\"Potential\",\"Stability\"\n");
            int row = 1;
            double minMass = 9999;
            Compound<?> minMassCompound = null;
            double maxMass = -1;
            Compound<?> maxMassCompound = null;
            for (Compound<?> compound : allCompounds.unabridged.values()) {
                if (compound.getFamily() == target) {
                    out.write(String.format("%d,\"%s\",\"%s\",%d,%.3f,%.3f\n",
                                            row++,
                                            compound.getFormula(),
                                            compound.getName() == null ? "" : compound.getName(),
                                            compound.getMass(),
                                            compound.getPotential(),
                                            compound.getStability()
                    ));
                    if (compound.getMass() < minMass) {
                        minMassCompound = compound;
                        minMass = compound.getMass();
                    }
                    if (compound.getMass() > maxMass) {
                        maxMassCompound = compound;
                        maxMass = compound.getMass();
                    }
                }
            }
            out.close();
            if (minMassCompound != null) {
                System.out.println("Lightest: " + minMassCompound.getName());
            }
            if (maxMassCompound != null) {
                System.out.println("Heaviest: " + maxMassCompound.getFormula());
            }
        }
    }
}
