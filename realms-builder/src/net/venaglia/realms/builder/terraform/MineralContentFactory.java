package net.venaglia.realms.builder.terraform;

import net.venaglia.realms.builder.terraform.impl.MineralContentImpl;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * User: ed
 * Date: 12/3/14
 * Time: 8:24 AM
 */
public class MineralContentFactory {

    public enum Neighbor {
        NEIGHBOR_0,
        NEIGHBOR_1,
        NEIGHBOR_2,
        NEIGHBOR_3,
        NEIGHBOR_4,
        NEIGHBOR_5;

        public static Neighbor byIndex(int i) {
            return values()[i];
        }
    }

    private static final int NEIGHBOR_COUNT = Neighbor.values().length;

    private static final ThreadLocal<MineralContentImpl[]> THREAD_LOCAL = new ThreadLocal<MineralContentImpl[]>() {
        @Override
        protected MineralContentImpl[] initialValue() {
            MineralContentImpl[] result = new MineralContentImpl[NEIGHBOR_COUNT + 1];
            for (int i = 0; i <= NEIGHBOR_COUNT; i++) {
                result[i] = new MineralContentImpl();
            }
            return result;
        }
    };

    private MineralContentFactory() {
        // pure static class
    }

    public static MineralContent get(long[] buffer, int index, long[] scratchpad, int scratchpadIndex, Neighbor neighbor) {
        assert buffer != null;
        assert scratchpad != null;
        int i = neighbor == null ? NEIGHBOR_COUNT : neighbor.ordinal();
        return THREAD_LOCAL.get()[i].load(buffer, index, scratchpad, scratchpadIndex);
    }

    public static void markAllClean(long[] scratchpadBuffer) {
        Arrays.fill(scratchpadBuffer, MineralContentImpl.CLEAN_VALUE);
    }

    public static void fillRandom(long[] mineralContentBuffer) {
        final long[] randomInitialValues = { // 120 initial permutations
                0x0111222333444555L, 0x0111222333555444L, 0x0111222444333555L, 0x0111222444555333L,
                0x0111222555333444L, 0x0111222555444333L, 0x0111333222555444L, 0x0111333222444555L,
                0x0111333444555222L, 0x0111333444222555L, 0x0111333555444222L, 0x0111333555222444L,
                0x0111444222333555L, 0x0111444222555333L, 0x0111444333222555L, 0x0111444333555222L,
                0x0111444555222333L, 0x0111444555333222L, 0x0111555222444333L, 0x0111555222333444L,
                0x0111555333444222L, 0x0111555333222444L, 0x0111555444333222L, 0x0111555444222333L,
                0x0222111555333444L, 0x0222111555444333L, 0x0222111333555444L, 0x0222111333444555L,
                0x0222111444555333L, 0x0222111444333555L, 0x0222333555444111L, 0x0222333555111444L,
                0x0222333111444555L, 0x0222333111555444L, 0x0222333444111555L, 0x0222333444555111L,
                0x0222444555111333L, 0x0222444555333111L, 0x0222444111555333L, 0x0222444111333555L,
                0x0222444333555111L, 0x0222444333111555L, 0x0222555444333111L, 0x0222555444111333L,
                0x0222555111333444L, 0x0222555111444333L, 0x0222555333111444L, 0x0222555333444111L,
                0x0333111444555222L, 0x0333111444222555L, 0x0333111555444222L, 0x0333111555222444L,
                0x0333111222444555L, 0x0333111222555444L, 0x0333222444111555L, 0x0333222444555111L,
                0x0333222555111444L, 0x0333222555444111L, 0x0333222111555444L, 0x0333222111444555L,
                0x0333444222555111L, 0x0333444222111555L, 0x0333444555222111L, 0x0333444555111222L,
                0x0333444111222555L, 0x0333444111555222L, 0x0333555222111444L, 0x0333555222444111L,
                0x0333555444111222L, 0x0333555444222111L, 0x0333555111444222L, 0x0333555111222444L,
                0x0444111222333555L, 0x0444111222555333L, 0x0444111333222555L, 0x0444111333555222L,
                0x0444111555222333L, 0x0444111555333222L, 0x0444222111555333L, 0x0444222111333555L,
                0x0444222333555111L, 0x0444222333111555L, 0x0444222555333111L, 0x0444222555111333L,
                0x0444333111222555L, 0x0444333111555222L, 0x0444333222111555L, 0x0444333222555111L,
                0x0444333555111222L, 0x0444333555222111L, 0x0444555111333222L, 0x0444555111222333L,
                0x0444555222333111L, 0x0444555222111333L, 0x0444555333222111L, 0x0444555333111222L,
                0x0555111444222333L, 0x0555111444333222L, 0x0555111222444333L, 0x0555111222333444L,
                0x0555111333444222L, 0x0555111333222444L, 0x0555222444333111L, 0x0555222444111333L,
                0x0555222111333444L, 0x0555222111444333L, 0x0555222333111444L, 0x0555222333444111L,
                0x0555333444111222L, 0x0555333444222111L, 0x0555333111444222L, 0x0555333111222444L,
                0x0555333222444111L, 0x0555333222111444L, 0x0555444333222111L, 0x0555444333111222L,
                0x0555444111222333L, 0x0555444111333222L, 0x0555444222111333L, 0x0555444222333111L
        };

        // normalize the chance
        final double[] chance = new double[randomInitialValues.length];
        double chanceTotal = 0;
        MineralContent mc = THREAD_LOCAL.get()[6].load(randomInitialValues, 0, new long[]{ MineralContent.CLEAN_VALUE }, 0);
        for (int i = 0; i < randomInitialValues.length; i++) {
            ((MineralContentImpl)mc).setIndex(i);
            chanceTotal += chance[i] = mc.chance();
        }
        // the values here are all constants; so we fudge a little on the constant to make it add up to 1048576
        double mul = 1048573.90 / chanceTotal;
        for (int i = 0; i < randomInitialValues.length; i++) {
            chance[i] *= mul;
        }

        final long[] opportunity = new long[1048576];
        int o = 0;
        for (int i = 0; i < randomInitialValues.length; i++) {
            int c = (int)Math.round(chance[i]);
            long initialValue = randomInitialValues[i];
            for (int j = 0; j < c; j++) {
                opportunity[o++] = initialValue;
            }
        }
        assert o == opportunity.length;
        Random random = new SecureRandom();
        for (int i = 0; i < mineralContentBuffer.length; i++) {
            mineralContentBuffer[i] = opportunity[random.nextInt(opportunity.length)];
        }
    }
}
