package net.venaglia.realms.builder.geoform;

import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.realms.spec.map.Globe;

import java.text.ParseException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* User: ed
* Date: 6/23/14
* Time: 7:37 AM
*/
public abstract class VertexValidator implements Runnable {

    public static final long COUNT_NOT_CHECKED = Long.MIN_VALUE + 2;

    private static final int EXPECTED_COUNT_ARRAY_LENGTH = 11;

    protected enum State {
        NEW(true), RUNNING(true), SUCCESS(false), FAIL(false);

        public final boolean pending;

        private State(boolean pending) {
            this.pending = pending;
        }
    }

    protected State state = State.NEW;
    protected VertexCounter counter;
    protected long points = 0;

    public synchronized void run() {
        assert state == State.NEW;
        state = State.RUNNING;
        init();
        try {
            validate();
            state = State.SUCCESS;
            synchronized (this) {
                this.notifyAll();
            }
        } finally {
            cleanup();
            if (state == State.RUNNING) {
                state = State.FAIL;
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }
    }

    protected synchronized void blockUntilDone() {
        while (state.pending) {
            try {
                wait(50L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    protected void init() {
        counter = new VertexCounter();
    }

    protected void validate() {
        processVertices();
        if (!validateExpected()) {
            if (GRAPH_UNEXPECTED_COUNTS.compareAndSet(false, true)) {
                try {
                    showDetailedCounts(new SectorDebugger.VertexCountAccessor() {
                        public long[] getAllVertexIds(long start, int limit) {
                            VertexCounter.CountAccessor countAccessor = counter.at(start);
                            long[] buffer = new long[limit];
                            int i = 0;
                            if (countAccessor.getCount() > 0) {
                                buffer[i++] = countAccessor.getVertexId();
                            }
                            try {
                                while (i < limit) {
                                    countAccessor.nextVertexNotZero();
                                    buffer[i++] = countAccessor.getVertexId();
                                }
                            } catch (NoSuchElementException e) {
                                // no more
                                long[] b = new long[i];
                                System.arraycopy(buffer, 0, b, 0, i);
                                buffer = b;
                            }
                            return buffer;
                        }

                        public int getCount(long vertexId) {
                            return counter.at(vertexId).getCount();
                        }

                        public long getVertexId(GeoPoint point) {
                            return Globe.INSTANCE.pointMap.getSeq(point);
                        }
                    });
                } catch (UnsupportedOperationException e) {
                    GRAPH_UNEXPECTED_COUNTS.compareAndSet(true, false);
                }
            }
        }
    }

    protected void showDetailedCounts(SectorDebugger.VertexCountAccessor vca) {
        throw new UnsupportedOperationException();
    }

    protected void cleanup() {
        counter.clear();
        counter = null;
    }

    protected abstract void processVertices();

    protected abstract long[] getExpectedCounts();

    protected String getInstanceName() {
        return null;
    }

    private static final AtomicBoolean GRAPH_UNEXPECTED_COUNTS = new AtomicBoolean();

    protected boolean validateExpected() {
        final long[] actualCounts = new long[EXPECTED_COUNT_ARRAY_LENGTH];
        System.arraycopy(counter.tallyCounts(), 0, actualCounts, 0, EXPECTED_COUNT_ARRAY_LENGTH);
        final long[] expectedCounts = getExpectedCounts();
        assert expectedCounts.length == EXPECTED_COUNT_ARRAY_LENGTH;
        boolean pass = true;
        for (int i = 0; pass && i < EXPECTED_COUNT_ARRAY_LENGTH; i++) {
            long expectedCount = expectedCounts[i];
            long actualCount = actualCounts[i];
            pass = expectedCount == COUNT_NOT_CHECKED || expectedCount == actualCount;
        }
        if (!pass) {
            recordError(new Object() {
                @Override
                public String toString() {
                    String instanceName = getInstanceName();
                    String who = instanceName == null || instanceName.length() == 0
                                 ? ""
                                 : " when checking \"" + instanceName + "\"";
                    int length = createZeroCountsArray().length;
                    String[] difference = new String[length];
                    String[] expected = new String[length];
                    String[] actual = new String[length];
                    String[] formats = new String[length];
                    for (int i = 0, l = difference.length; i < l; i++) {
                        boolean notChecked = expectedCounts[i] == COUNT_NOT_CHECKED;
                        actual[i] = String.valueOf(actualCounts[i]);
                        expected[i] = notChecked ? "*" : String.valueOf(expectedCounts[i]);
                        difference[i] = notChecked ? "-" : String.valueOf(expectedCounts[i] - actualCounts[i]);
                        formats[i] = "%" + Math.max(Math.max(expected[i].length(), difference[i].length()), actual[i].length()) + "s";
                    }
                    String format = Arrays.toString(formats);
                    return String.format("Vertex counts are not as expected%s:\n" +
                                         "\t  expected: %s\n" +
                                         "\t    actual: %s\n" +
                                         "\tdifference: %s\n",
                                         who,
                                         String.format(format, (Object[])expected),
                                         String.format(format, (Object[])actual),
                                         String.format(format, (Object[])difference));
                }
            });
        }
        return pass;
    }

    protected long totalPoints(long[] counts) {
        int l = counts.length - 1;
        long sum = 0;
        assert counts[0] == 0 && counts[l] == 0;
        for (int i = 0; i < l; i++) {
            sum += counts[i] * i;
        }
        return sum;
    }

    private long sumDistinctPoints(long[] actualCounts) {
        long sum = 0;
        for (long count : actualCounts) {
            sum += count;
        }
        return sum;
    }

    protected void recordError(Object message) {
        System.err.println(message);
    }

    protected void addAll(RangeBasedLongSet set) {
        try {
            //noinspection InfiniteLoopStatement
            for (long a = set.getNext(Long.MIN_VALUE), b = set.getNextNotIncluded(a); true; a = set.getNext(b), b = set.getNextNotIncluded(a)) {
                addAll(a, (int)(b - a));
//                points += b - a;
            }
        } catch (NoSuchElementException e) {
            // end of loop, no more elements
        }
    }

    protected void addAll(long start, int count) {
        counter.incrementRange(start, count);
//        long to = start + count - 1;
//        for (Long i = start; i <= to; i++) {
//            add(i);
//        }
    }

    protected void add(long id) {
        counter.at(id).incrementCount();
        points++;
    }

    public boolean isSuccess() {
        return state == State.SUCCESS;
    }

    public static long[] createZeroCountsArray() {
        return new long[EXPECTED_COUNT_ARRAY_LENGTH];
    }

    public static void main(String[] args) {
        String[] tests = {
//                "1",
                "[1..5]!5@1",
                "[1..5];[1..5]!5@2",
                "[1..5];[2..6];[3..7];[4..8];[5..9]!2@1,2@2,2@3,2@4,1@5",
                "[1..999];[500..599]!899@1,100@2",
                "[1..99999];[50000..59999]!89999@1,10000@2",
        };
        for (final String test : tests) {
            VertexValidator vv = new VertexValidator() {

                private long[] expectedCounts = createZeroCountsArray();

                @Override
                protected void processVertices() {
                    try {
                        if (test.matches("\\d+")) {
                            long value = Long.parseLong(test);
                            add(value);
                            expectedCounts[1] = 1;
                        } else {
                            String[] split = test.split("!");
                            String sets = split[0];
                            for (String set : sets.split(";")) {
                                addAll(RangeBasedLongSet.parse(set));
                            }
                            String[] counts = split[1].split(",");
                            for (String count : counts) {
                                String[] at = count.split("@");
                                if ("*".equals(at[1])) {
                                    Arrays.fill(expectedCounts, parseCount(at[0]));
                                } else {
                                    expectedCounts[(int)parseCount(at[1])] = parseCount(at[0]);
                                }
                            }
                        }
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                protected String getInstanceName() {
                    return "test:" + test;
                }

                private long parseCount(String s) {
                    return "*".equals(s) ? COUNT_NOT_CHECKED : Integer.parseInt(s);
                }

                @Override
                protected long[] getExpectedCounts() {
                    expectedCounts[0] = 0;
                    return expectedCounts;
                }
            };
            vv.run();
        }
    }
}
