package net.venaglia.realms.common.map.things.surface;

import net.venaglia.common.util.ThreadSingletonSource;

import java.util.Random;

/**
 * User: ed
 * Date: 4/22/14
 * Time: 5:27 PM
 */
public class RandomSequence {

    public static final ThreadSingletonSource<RandomSequence> SOURCE = ThreadSingletonSource.forType(RandomSequence.class);

    private Random rand;

    private RandomSequence() {
    }

    public RandomSequence load(long seed) {
        this.rand = new Random(seed);
        return this;
    }

    public void recycle() {
        rand = null;
    }

    public double getNext() {
        return rand.nextGaussian();
    }
}
