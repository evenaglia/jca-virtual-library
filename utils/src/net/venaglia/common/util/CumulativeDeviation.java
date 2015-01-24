package net.venaglia.common.util;

/**
 * Statistical utility class for continuously tracking the deviation and
 * average of an input set.
 */
public class CumulativeDeviation {

    private double sumValues = 0.0;
    private double sumSquares = 0.0;
    private int count = 0;

    /**
     * Adds another sample value to the statistical accumulator.
     * @param value The next sampled value
     */
    public void add(double value) {
        sumValues += value;
        sumSquares += value * value;
        count += 1;
    }

    /**
     * Removes a value from the statistical accumulator. This value is expected
     * to be a part of the accumulated set, but there is no check made to
     * ensure this.
     *
     * Should only be used by subclasses that track values in the statistical
     * set.
     * @param value The sample value to be removed
     */
    private void remove(double value) {
        sumValues -= value;
        sumSquares -= value * value;
        count -= 1;
    }

    /**
     * Computes the standard deviation of the input set so far. If the set is
     * empty, NaN is returned. If the set has only one sample, zero is returned.
     * @return The standard deviation of the input set.
     * @link http://en.wikipedia.org/wiki/Standard_deviation
     */
    public double deviation() {
        switch (count) {
            case 0:
                return Double.NaN;
            case 1:
                return 0.0;
            default:
                double count = this.count;
                double avg = sumValues / count;
                return Math.sqrt((sumSquares - 2.0 * sumValues * avg + avg * avg * count) / count);
        }
    }

    /**
     * Computes the statistical average of the input set so far. If the set is
     * empty, NaN is returned.
     * @return The statistical average of the input set.
     * @link http://en.wikipedia.org/wiki/Arithmetic_mean
     */
    public double average() {
        switch (count) {
            case 0:
                return Double.NaN;
            case 1:
                return sumValues;
            default:
                return sumValues / count;
        }
    }

    /**
     * @return The count of samples in the current accumulator
     */
    public int count() {
        return count;
    }

    /**
     * Resets this accumulator, removing all samples
     */
    public void clear() {
        sumValues = 0.0;
        sumSquares = 0.0;
        count = 0;
    }

    public static CumulativeDeviation forRecentSamples(final int sampleCount) {
        return new CumulativeDeviation() {

            private final double[] buffer = new double[sampleCount];
            private int index = 0;

            @Override
            public void add(double value) {
                super.add(value);
                double oldValue = buffer[index];
                buffer[index++] = value;
                if (index >= sampleCount) {
                    index = 0;
                }
                if (count() > sampleCount) {
                    super.remove(oldValue);
                }
            }

            @Override
            public void clear() {
                super.clear();
                index = 0;
            }
        };
    }
}
