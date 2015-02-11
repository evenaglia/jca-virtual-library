package net.venaglia.common.util;

import java.util.BitSet;

/**
 * User: ed
 * Date: 1/28/15
 * Time: 11:25 PM
 */
public class IntSetHashTest {

    public static void main(String[] args) {
        IntSet test = new IntSet();
        BitSet pos = new BitSet(1 << 30);
        BitSet neg = new BitSet(1 << 30);
        assert test(test.keyFrom(0), 0, pos, neg) : "collision: i = " + 0;
        for (int i = -1; i > Integer.MIN_VALUE; i--) {
            assert test(test.keyFrom(-i), i, pos, neg) : "collision: i = " + -i;
            assert test(test.keyFrom(i), i, pos, neg) : "collision: i = " + i;
            if ((i & 0xFFFFF) == 0) {
                System.out.printf("%dmb\n", (i & 0x7FF00000) >> 20);
            }
        }
        assert test(test.keyFrom(Integer.MIN_VALUE), Integer.MIN_VALUE, pos, neg) : "collision: i = NO_VALUE";
    }

    private static boolean test(int k, int i, BitSet pos, BitSet neg) {
        assert k != i : "Key [" + k + "] is equal to original value [" + i + "]";
        int v = k & 0x7FFFFFFF;
        BitSet set = k < 0 ? neg : pos;
        if (set.get(v)) return false;
        set.set(v);
        return true;
    }

}
