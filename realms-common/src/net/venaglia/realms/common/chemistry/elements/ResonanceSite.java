package net.venaglia.realms.common.chemistry.elements;

/**
 * User: ed
 * Date: 5/19/14
 * Time: 4:17 PM
 *
 * Every element uses some of the 5 ResonanceSites. Each site has a different
 * frequency, and the properties of that element are defined by the frequency
 * of the standing wave that occurs as the lowest harmonic frequency of all
 * active sites in the element.
 */
public enum ResonanceSite {

    A(2),
    B(8),
    C(5),
    D(13),
    E(3);

    private static final ResonanceSite[] values;

    static {
        values = new ResonanceSite[]{ A, B, C, D, E };
    }

    private final int value;

    ResonanceSite(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    int winding(ResonanceSite property) {
        return (property.ordinal() - ordinal() + 12) % 5 - 2;
    }

    ResonanceSite wind(int wind) {
        return values[(ordinal() + wind + 10) % 5];
    }

    @Override
    public String toString() {
        return String.format("%s[%d]", name(), value);
    }
}
