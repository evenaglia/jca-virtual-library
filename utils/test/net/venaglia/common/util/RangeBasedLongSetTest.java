/*
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2011 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package net.venaglia.common.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.*;

/**
 * Performs a 100% code coverage unit test of RangeBasedLongSet. :)
 */
public class RangeBasedLongSetTest {

    private static final long TEST_EXPECT_NULL = 3999999999L;
    private static final long TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION = 4000000000L;
    private static final long TEST_EXPECT_ILLEGAL_STATE_EXCEPTION = 4000000001L;
    private static final long TEST_CONCURRENT_MODIFICATION_EXCEPTION = 4000000002L;

    private TestSet getStandardSet1() {
        TestSet set = new TestSet();
        set.addAll(10, 19);
        set.addAll(30, 39);
        set.addAll(50, 59);
        return set;
    }

    private TestSet getStandardSet2() {
        TestSet set = new TestSet();
        set.addAll(15, 24);
        set.addAll(35, 44);
        set.addAll(55, 64);
        return set;
    }

    private TestSet getStandardSet3() {
        TestSet set = new TestSet();
        set.addAll(20, 29);
        set.addAll(40, 49);
        set.addAll(60, 69);
        return set;
    }

    private void check(TestSet set, int size, String toString) {
        assertEquals("The set did not contain the expected number of elements", size, set.size());
        assertEquals("The set did not contain the expected ranges", toString, set.toString());
        if (size == 0) {
            assertTrue("The set identified itself as empty when it was not", set.isEmpty());
        }
        else {
            assertFalse("The set identified itself as not empty when it was", set.isEmpty());
        }
        set.assertOptimized();
    }

    public void testBasicOperations() {
        TestSet set;

        set = new TestSet();
        check(set, 0, "[]");
        set.add(999);
        check(set, 1, "[999]");
        set.add(1001);
        check(set, 2, "[999,1001]");
        set.add(1000);
        check(set, 3, "[999..1001]");
        set.add(1000);
        check(set, 3, "[999..1001]");
        set.remove(1000);
        check(set, 2, "[999,1001]");
        set.remove(1000);
        check(set, 2, "[999,1001]");
        set.add(1000);
        check(set, 3, "[999..1001]");
        set.remove(999);
        check(set, 2, "[1000..1001]");
        set.remove(1002);
        check(set, 2, "[1000..1001]");
        set.add(1001);
        check(set, 2, "[1000..1001]");
        set.add(1002);
        check(set, 3, "[1000..1002]");
        set.remove(1002);
        check(set, 2, "[1000..1001]");
        set.add(999);
        check(set, 3, "[999..1001]");
        set.remove(999);
        check(set, 2, "[1000..1001]");
        set.removeAll(999, 1001);
        check(set, 0, "[]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.add(24);
        check(set, 31, "[10..19,24,30..39,50..59]");
        set.addAll(21, 27);
        check(set, 37, "[10..19,21..27,30..39,50..59]");
        set.addAll(20, 20);
        check(set, 38, "[10..27,30..39,50..59]");
        set.removeAll(20, 20);
        check(set, 37, "[10..19,21..27,30..39,50..59]");
        set.add(20);
        check(set, 38, "[10..27,30..39,50..59]");
        set.addAll(0, 100);
        check(set, 101, "[0..100]");
        set.removeAll(0, 9);
        check(set, 91, "[10..100]");
        set.removeAll(90, 99);
        check(set, 81, "[10..89,100]");
        set.remove(100);
        check(set, 80, "[10..89]");
        set.removeAll(50, 59);
        check(set, 70, "[10..49,60..89]");
        set.removeAll(60, 89);
        check(set, 40, "[10..49]");
        set.addAll(0, 4);
        check(set, 45, "[0..4,10..49]");
        set.addAll(2, 34);
        check(set, 50, "[0..49]");

        set = getStandardSet2();
        check(set, 30, "[15..24,35..44,55..64]");
        set.retainAll(0, 100);
        check(set, 30, "[15..24,35..44,55..64]");
        set.addAll(25, 54);
        check(set, 50, "[15..64]");
        set.retainAll(15, 64);
        check(set, 50, "[15..64]");
        set.retainAll(5, 64);
        check(set, 50, "[15..64]");
        set.retainAll(15, 74);
        check(set, 50, "[15..64]");
        set.retainAll(24, 55);
        check(set, 32, "[24..55]");
        set.retainAll(getStandardSet2());
        check(set, 12, "[24,35..44,55]");
        set.invert();
        check(set, 2147483647, "[-9223372036854775808..23,25..34,45..54,56..9223372036854775807]");
        set.removeAll(999, 1001);
        check(set, 2147483647, "[-9223372036854775808..23,25..34,45..54,56..998,1002..9223372036854775807]");

        set = getStandardSet3();
        check(set, 30, "[20..29,40..49,60..69]");
        set.retainAll(getStandardSet3());
        check(set, 30, "[20..29,40..49,60..69]");
        set.retainAll(getStandardSet2());
        check(set, 15, "[20..24,40..44,60..64]");
        set.retainAll(getStandardSet1());
        check(set, 0, "[]");
        set.addAll(getStandardSet2());
        check(set, 30, "[15..24,35..44,55..64]");
        set.addAll(getStandardSet3());
        check(set, 45, "[15..29,35..49,55..69]");
        set.addAll(getStandardSet3());
        check(set, 45, "[15..29,35..49,55..69]");
        set.addAll(getStandardSet2());
        check(set, 45, "[15..29,35..49,55..69]");
        assertFalse("The set should not have contained all elements but " + "indicated as such",
                set.containsAll(getStandardSet1()));
        check(set, 45, "[15..29,35..49,55..69]");
        assertTrue("The set should have contained all elements but did not " + "indicate as such",
                set.containsAll(getStandardSet2()));
        check(set, 45, "[15..29,35..49,55..69]");
        set.addAll(getStandardSet1());
        check(set, 60, "[10..69]");
        set.clear();
        check(set, 0, "[]");
        set.removeAll(getStandardSet3());
        check(set, 0, "[]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.addAll(20, 44);
        check(set, 45, "[10..44,50..59]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.addAll(25, 49);
        check(set, 45, "[10..19,25..59]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.removeAll(25, 44);
        check(set, 20, "[10..19,50..59]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.removeAll(15, 44);
        check(set, 15, "[10..14,50..59]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.removeAll(25, 54);
        check(set, 15, "[10..19,55..59]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.removeAll(30, 44);
        check(set, 20, "[10..19,50..59]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.removeAll(15, 39);
        check(set, 15, "[10..14,50..59]");

        set = getStandardSet1();
        check(set, 30, "[10..19,30..39,50..59]");
        set.removeAll(15, 54);
        check(set, 10, "[10..14,55..59]");
        assertTrue("Set did not indicate that it contains a value that is does.", set.contains(10));
        assertTrue("Set did not indicate that it contains a value that is does.", set.contains(13));
        assertTrue("Set did not indicate that it contains a value that is does.", set.contains(14));
        assertFalse("Set indicated that it contains a value that is does not.", set.contains(15));
        assertFalse("Set indicated that it contains a value that is does not.", set.contains(16));
        assertFalse("Set indicated that it contains a value that is does not.", set.contains(54));
        assertFalse("Set indicated that it contains a value that is does not.", set.contains(Long.MIN_VALUE));
        assertFalse("Set indicated that it contains a value that is does not.", set.contains(Long.MAX_VALUE));

        set = getStandardSet1();
        assertTrue("Set indicated that it does not contain values that it does", set.containsAny(getStandardSet2()));
        assertFalse("Set indicated that it contains values that it does not.", set.containsAny(getStandardSet3()));
    }

    public void testIteratorFunctions() {
        final TestSet set = getStandardSet1();

        new TestIterator<Iterator<Long>>(set.iterator(), 31, null,
                new long[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 50, 51, 52,
                        53, 54, 55, 56, 57, 58, 59, TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return testObj.hasNext();
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                return testObj.next();
            }
        }.test();

        new TestIterator<Iterator<Long>>(set.iterator(40), 11, null,
                new long[]{50, 51, 52, 53, 54, 55, 56, 57, 58, 59, TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return testObj.hasNext();
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                return testObj.next();
            }
        }.test();

        new TestIterator<Iterator<Long>>(set.iterator(55), 6, null,
                new long[]{55, 56, 57, 58, 59, TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return testObj.hasNext();
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                return testObj.next();
            }
        }.test();

        set.addAll(Long.MIN_VALUE, Long.MIN_VALUE + 1);
        check(set, 32, "[-9223372036854775808..-9223372036854775807,10..19,30..39,50..59]");
        set.addAll(Long.MAX_VALUE - 1, Long.MAX_VALUE);
        check(set, 34, "[-9223372036854775808..-9223372036854775807,10..19,30..39,50..59,9223372036854775806..9223372036854775807]");
        new TestIterator<Iterator<Long>>(set.iterator(), 35, null,
                new long[]{Long.MIN_VALUE, Long.MIN_VALUE + 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 30, 31, 32,
                        33, 34, 35, 36, 37, 38, 39, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, Long.MAX_VALUE - 1,
                        Long.MAX_VALUE, TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return testObj.hasNext();
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                return testObj.next();
            }
        }.test();

        new TestIterator<Iterator<Long>>(set.iterator(), 13, null,
                new long[]{Long.MIN_VALUE, Long.MIN_VALUE + 1, 10, 11, 12, 13, 14, 15, 16, 17,
                        TEST_EXPECT_ILLEGAL_STATE_EXCEPTION, 19, TEST_CONCURRENT_MODIFICATION_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return testObj.hasNext();
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                Long testValue = testObj.next();
                if (testValue == 18L) {
                    testObj.remove();
                    testObj.remove(); // throws ise
                }
                else if (testValue == 19L) {
                    set.removeAll(30, 39);
                }
                return testValue;
            }
        }.test();
        check(set, 23, "[-9223372036854775808..-9223372036854775807,10..17,19,50..59,9223372036854775806..9223372036854775807]");

        new TestIterator<Iterator<Long>>(set.iterator(), 24, null,
                new long[]{Long.MIN_VALUE, Long.MIN_VALUE + 1, 10, 11, 12, 13, 14, 15, 16, 17, 19, 50, 51, 52, 53,
                        54, 55, 56, 57, 58, 59, Long.MAX_VALUE - 1, Long.MAX_VALUE,
                        TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return testObj.hasNext();
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                Long testValue = testObj.next();
                if (testValue == 19L) {
                    set.removeAll(30, 39);
                }
                return testValue;
            }
        }.test();
        check(set, 23, "[-9223372036854775808..-9223372036854775807,10..17,19,50..59,9223372036854775806..9223372036854775807]");

        new TestIterator<Iterator<Long>>(set.iterator(), 24, null,
                new long[]{Long.MIN_VALUE, Long.MIN_VALUE + 1, 10, 11, 12, 13, 14, 15, 16, 17, 19, 50, 51, 52, 53,
                        54, 55, 56, 57, 58, 59, Long.MAX_VALUE - 1, Long.MAX_VALUE,
                        TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return testObj.hasNext();
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                Long testValue = testObj.next();
                testObj.remove();
                return testValue;
            }
        }.test();
        check(set, 0, "[]");
    }

    public void testNextFunctions() {
        final TestSet set = getStandardSet1();

        new TestIterator<TestSet>(set, 13, new long[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60},
                new long[]{10, 10, 10, 15, 30, 30, 30, 35, 50, 50, 50, 55, TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return seq < 12;
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                return testObj.getNext(value);
            }
        }.test();

        set.add(Long.MAX_VALUE);
        new TestIterator<TestSet>(set, 13, new long[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, Long.MAX_VALUE},
                new long[]{0, 5, 20, 20, 20, 25, 40, 40, 40, 45, 60, 60, TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION})
        {
            protected boolean hasNext(int seq) {
                return seq < 12;
            }

            protected Long next(long value) throws NoSuchElementException, IllegalStateException {
                return testObj.getNextNotIncluded(value);
            }
        }.test();
    }

    public void testUtilityFunctions() {
        TestSet set = getStandardSet1();
        assertEquals("Same sets did not match", set, set);
        assertEquals("Identical empty sets did not match", new TestSet(), new TestSet());
        assertEquals("Identical sets did not match", set, getStandardSet1());
        assertEquals("Identical sets did not match", getStandardSet1(), set);
        assertEquals("Cloned sets did not match", set.clone(), set);
        assertEquals("Cloned sets did not match", set, set.clone());
        assertFalse("Non-identical sets indicated a match", getStandardSet2().equals(set));
        assertFalse("Non-identical sets indicated a match", set.equals(getStandardSet2()));
        assertFalse("Non-identical sets indicated a match", getStandardSet3().equals(set));
        assertFalse("Non-identical sets indicated a match", set.equals(getStandardSet3()));
        assertFalse("Non-identical sets indicated a match", new TestSet().equals(set));
        assertFalse("Non-identical sets indicated a match", set.equals(new TestSet()));
        assertFalse("Set matched NULL (WTF?)", set.equals(null));
        assertFalse("Set matched a garbage string (WTF?)", set.equals("Peanut butter and jelly sandwich"));

        assertEquals("Expected hashCode() was incorrect", 31, new TestSet().hashCode());
        assertEquals("Expected hashCode() was incorrect", 75490016, getStandardSet1().hashCode());
        assertEquals("Expected hashCode() was incorrect", 656426784, getStandardSet2().hashCode());
        assertEquals("Expected hashCode() was incorrect", 1237363552, getStandardSet3().hashCode());
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public void testParseFailures() {
        testParse(null, NullPointerException.class);
        testParse("", 0);
        testParse("foo", 0);
        testParse("[ ]", 1);
        testParse("[foo]", 1);
        testParse("[1,3..5,bar]", 8);
        testParse("[-0]", 1);
        testParse("[0123]", 1);
        testParse("[-9223372036854775809]", NumberFormatException.class);
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    private void testParse(String s, int position) {
        ParseException parseException = testParse(s, ParseException.class);
        assertEquals("Expected parse error position is not as expected", position, parseException.getErrorOffset());
    }

    private <F extends Throwable> F testParse(String s, Class<F> fail) {
        try {
            RangeBasedLongSet.parse(s);
            assertTrue("Expected " + fail + ", but was not thrown: \"" + s + "\"", false);
        }
        catch (Throwable t) {
            if (fail.isAssignableFrom(t.getClass())) {
                return fail.cast(t);
            }
            throw new RuntimeException(t);
        }
        return null;
    }

    public void testPerformance() {
//    final int numCycles = 100000; // use for a real performance test!
        final int numCycles = 1000;

        RangeBasedLongSet set = new RangeBasedLongSet();
        for (int i = 0; i < numCycles; i += 10) {
            set.add(i);
        }
        for (int i = 0; i < numCycles; i += 10) {
            set.add(i + 5);
        }
        for (int i = 0; i < numCycles; i += 10) {
            set.addAll(i, i + 5);
        }
        for (int i = 0; i < numCycles; i += 10) {
            set.remove(i);
        }
        for (int i = 0; i < numCycles; i += 10) {
            set.remove(i + 3);
        }
        set.removeAll(0, numCycles);
    }

    public void testInvalidRanges() {
        RangeBasedLongSet set = new RangeBasedLongSet();
        try {
            set.addAll(100, 0);
            assertTrue("A range operation involving a mis-ordered from/to pair was " + "permitted", false);
        }
        catch (IllegalArgumentException e) {
            // This is the expected result from this test case.
        }
    }

    private abstract static class TestIterator<TO> {

        protected final TO testObj;
        private final int iterations;
        private final long[] testData;
        private final long[] expectedResults;

        TestIterator(TO testObj, int iterations, long[] testData, long[] expectedResults) {
            this.testObj = testObj;
            this.iterations = iterations;
            this.testData = testData;
            this.expectedResults = expectedResults;
        }

        protected boolean hasNext(int seq) {
            return true;
        }

        protected abstract Long next(long value) throws NoSuchElementException, IllegalStateException;

        final void test() {
            for (int i = 0; i < iterations; i++) {
                long value = (testData != null) ? testData[i] : 0;
                long result;
                long expected = expectedResults[i];
                boolean hasNext;
                try {
                    hasNext = hasNext(i);
                    if (expected < 3000000000L) {
                        assertTrue("hasNext() did not report another value was available " +
                                   "when one is expected [" + i + "]",
                                   hasNext);
                    }
                    else if (expected == TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION) {
                        assertFalse("hasNext() reported another value was available " +
                                    "when expecting no more elements",
                                    hasNext);
                    }
                    Long testResult = next(value);
                    if (expected == TEST_EXPECT_NULL) {
                        assertNull("Null was expected [" + i + "]", testResult);
                    }
                    else if (expected == TEST_EXPECT_ILLEGAL_STATE_EXCEPTION) {
                        assertFalse("Expected an IllegalStateException to be thrown " + "[" + i + "]", true);
                    }
                    else if (expected == TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION) {
                        assertFalse("Expected a NoSuchElementException to be thrown " + "[" + i + "]", true);
                    }
                    else if (expected == TEST_CONCURRENT_MODIFICATION_EXCEPTION) {
                        assertFalse("Expected a ConcurrentModificationException to be " + "thrown [" + i + "]", true);
                    }
                    else {
                        result = testResult;
                        assertEquals("Value was not the expected value [" + i + "]", expected, result);
                    }
                }
                catch (NoSuchElementException nsee) {
                    if (expected != TEST_EXPECT_NO_SUCH_ELEMENT_EXCEPTION) {
                        throw nsee;
                    }
                }
                catch (IllegalStateException ise) {
                    if (expected != TEST_EXPECT_ILLEGAL_STATE_EXCEPTION) {
                        throw ise;
                    }
                }
                catch (ConcurrentModificationException ccme) {
                    if (expected != TEST_CONCURRENT_MODIFICATION_EXCEPTION) {
                        throw ccme;
                    }
                }
            }
        }
    }

    // Test subclass that validates the collection after every operation that
    // possibly modifies it.
    private static class TestSet extends RangeBasedLongSet {

        public TestSet() {
            super();
            assertOptimized();
        }

        public TestSet(RangeBasedLongSet longSet) {
            super(longSet);
            assertOptimized();
        }

        public boolean add(long value) {
            boolean changed = super.add(value);
            assertOptimized();
            return changed;
        }

        public boolean remove(long value) {
            boolean changed = super.remove(value);
            assertOptimized();
            return changed;
        }

        public boolean addAll(long from, long to) {
            boolean changed = super.addAll(from, to);
            assertOptimized();
            return changed;
        }

        public boolean addAll(RangeBasedLongSet values) {
            boolean changed = super.addAll(values);
            assertOptimized();
            return changed;
        }

        public boolean retainAll(long from, long to) {
            boolean changed = super.retainAll(from, to);
            assertOptimized();
            return changed;
        }

        public boolean retainAll(RangeBasedLongSet values) {
            boolean changed = super.retainAll(values);
            assertOptimized();
            return changed;
        }

        public boolean removeAll(long from, long to) {
            boolean changed = super.removeAll(from, to);
            assertOptimized();
            return changed;
        }

        public boolean removeAll(RangeBasedLongSet values) {
            boolean changed = super.removeAll(values);
            assertOptimized();
            return changed;
        }

        public Iterator<Long> iterator(long firstValue) {
            final Iterator<Long> iterator = super.iterator(firstValue);
            return new Iterator<Long>() {
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public Long next() {
                    return iterator.next();
                }

                public void remove() {
                    iterator.remove();
                    assertOptimized();
                }
            };
        }

        public void invert() {
            super.invert();
            assertOptimized();
        }

        public void clear() {
            super.clear();
        }

        public void assertOptimized() {
            assertNotNull("allRanges should not be null", ranges);
            assertTrue("allRanges should never be empty", ranges.size() > 0);
            Range leftRange = ranges.get(0);
            assertEquals("The first range should always begin with MIN_VALUE",
                         Long.MIN_VALUE,
                         leftRange.getBegin());
            Range rightRange;
            long size = leftRange.size();
            for (int i = 1; i < ranges.size(); i++) {
                rightRange = ranges.get(i);
                assertEquals("Ranges should always start immediately after " +
                             "the preceeding range [" + i + "]",
                             leftRange.getEnd() + 1, rightRange.getBegin());
                assertTrue("Consecutive ranges should not have the same " +
                           "inclusion [" + i + "]",
                           leftRange.isIncluded() != rightRange.isIncluded());
                assertTrue("Ranges should always end on or after they begin " +
                           "[" + i + "]",
                           rightRange.getBegin() <= rightRange.getEnd());
                size += rightRange.size();
                leftRange = rightRange;
            }
            assertEquals("The last range should always end with MAX_VALUE",
                         Long.MAX_VALUE,
                         leftRange.getEnd());
            assertEquals("This set does not indicate the correct size",
                         size,
                         this.size);
        }
    }

    private static void assertEquals(String why, Object o1, Object o2) {
        assertTrue(why, o1 == null ? o2 == null : o1.equals(o2));
    }

    private static void assertEquals(String why, int i1, int i2) {
        assertTrue(why, i1 == i2);
    }

    private static void assertTrue(String why, boolean value) {
        if (!value) {
            throw new AssertionError(why);
        }
    }

    private static void assertFalse(String why, boolean value) {
        assertTrue(why, !value);
    }

    private static void assertNotNull(String why, Object o) {
        assertTrue(why, o != null);
    }

    private static void assertNull(String why, Object o) {
        assertTrue(why, o == null);
    }

    public static void main(String[] args) throws Exception {
        RangeBasedIntegerSetTest test = new RangeBasedIntegerSetTest();
        for (Method m : test.getClass().getDeclaredMethods()) {
            if (Void.TYPE.equals(m.getReturnType()) && m.getParameterTypes().length == 0) {
                int mod = m.getModifiers();
                if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
                    m.invoke(test);
                }
            }
        }
    }
}
