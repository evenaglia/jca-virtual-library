package net.venaglia.common.util;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 1/28/15
 * Time: 11:24 PM
 */
public class IntSetTest {

    @SuppressWarnings({ "AssertWithSideEffects", "ObjectEqualsNull", "EqualsBetweenInconvertibleTypes" })
    public static void main(String[] args) {
        int[] values = {38776,17751,42511,18701,43262,40111,65050,8413,25052,33527,51231,3577,15120,3581,10793,26423,18559,63833,24661,29080,22098,31583,36507,26164,46895,945,58294,35344,28697,18112,32008,50708,15565,13925,44283,38005,64871,53723,13534,39202,42255,7939,7165,64981,14007,63532,61160,16169,53718,32049,61098,45117,10947,45,38831,44205,537,54883,42485,30422,37297,61670,34761,47398,21859,61032};
        IntSet set = new IntSet();
        for (int value : values) {
            set.add(value);
            set.add(value);
            set.remove(value);
            set.remove(value);
            set.add(value);
        }
        assert set.size() == 66;
        int[] array = set.toArray();
        Arrays.sort(values);
        Arrays.sort(array);
        assert Arrays.equals(values, array);
        IntSet set2 = set.clone();
        assert set2.equals(set);
        assert set.equals(set2);
        IntIterator iterator1 = set.iterator();
        IntIterator iterator2 = set.iterator();
        iterator1.next();
        iterator1.remove();
        assert set.size() == 65;
        assert !set.containsAll(set2);
        assert set2.containsAll(set);
        set2.remove(values[0]);
        set2.remove(values[1]);
        assert !set2.equals(set);
        assert !set.equals(set2);
        assert set2.add(-1);
        assert !set2.add(-1);
        assert !set2.equals(set);
        assert !set.equals(set2);
        assert !set2.equals(null);
        assert !set2.equals("null");
        assert !set.containsAll(set2);
        assert !set2.containsAll(set);
        try {
            iterator1.remove();
            assert false : "fail";
        } catch (IllegalStateException e) {
            // expected
        }
        int c = 0;
        while (iterator1.hasNext()) {
            iterator1.next();
            c++;
        }
        assert c == 65;
        try {
            iterator1.next();
            assert false : "fail";
        } catch (NoSuchElementException e) {
            // expected
        }
        try {
            iterator2.next();
            assert false : "fail";
        } catch (ConcurrentModificationException e) {
            // expected
        }
        IntSet set3 = new IntSet();
        assert "[]".equals(set3.toString());
        assert set3.hashCode() == 0;
        assert set3.isEmpty();
        set3.add(999);
        assert "[999]".equals(set3.toString());
        assert set3.hashCode() == 0x4bd93688;
        assert !set3.isEmpty();
        set3.remove(999);
        assert set3.hashCode() == 0;
        set3.add(-123);
        assert set3.hashCode() == 0xa126cbfe;
        set3.add(456);
        assert "[456,-123]".equals(set3.toString());
        assert set3.hashCode() == 0xf00000a0;
        set3.add(789);
        assert "[456,789,-123]".equals(set3.toString());
        assert set3.hashCode() == 0xbd936ca;
        set2.clear();
        set2.add(123);
        set2.add(456);
        set2.add(789);
        assert set3.retainAll(set2);
        assert "[456,789]".equals(set3.toString());
        assert !set3.retainAll(set2);
        assert "[456,789]".equals(set3.toString());
        set2.remove(456);
        set2.add(-456);
        assert set3.removeAll(set2);
        assert "[456]".equals(set3.toString());
        assert !set3.removeAll(set2);
        assert "[456]".equals(set3.toString());
        assert !set3.containsAll(set2);
        assert set3.addAll(set2);
        assert !set3.addAll(set2);
        assert "[123,456,789,-456]".equals(set3.toString());
        try {
            new IntSet(-1);
            assert false : "fail";
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new IntSet(Integer.MAX_VALUE, 0.0f);
            assert false : "fail";
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new IntSet(Integer.MAX_VALUE, Float.NaN);
            assert false : "fail";
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            set3.add(Integer.MIN_VALUE);
            assert false : "fail";
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

}
