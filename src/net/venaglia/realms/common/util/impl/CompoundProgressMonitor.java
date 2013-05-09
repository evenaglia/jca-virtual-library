package net.venaglia.realms.common.util.impl;

import net.venaglia.realms.common.util.ProgressListener;
import net.venaglia.realms.common.util.ProgressMonitor;

/**
 * User: ed
 * Date: 12/6/12
 * Time: 6:01 PM
 */
public class CompoundProgressMonitor implements ProgressMonitor {

    public final ProgressMonitor[] delegates;

    private final int numberOfSteps;

    private double[] relativeWieghts = null;
    private double sumWeights;

    public CompoundProgressMonitor(ProgressMonitor... delegates) {
        this.delegates = delegates;
        int sum = 0;
        for (ProgressMonitor pm : delegates) {
            sum += pm.getNumberOfSteps();
        }
        numberOfSteps = sum;
        sumWeights = delegates.length;
    }

    public void setRelativeWieghts(double... relativeWieghts) {
        if (relativeWieghts.length != delegates.length) {
            throw new IllegalArgumentException();
        }
        double sum = 0.0;
        for (double d : relativeWieghts) {
            if (d < 0.0) {
                throw new IllegalArgumentException();
            }
            sum += d;
        }
        if (sum == 0.0) {
            throw new IllegalArgumentException();
        }
        this.relativeWieghts = relativeWieghts;
        this.sumWeights = sum;
    }

    public int getNumberOfSteps() {
        return numberOfSteps;
    }

    public int getCurrentStepNumber() {
        int sum = 0;
        for (ProgressMonitor pm : delegates) {
            sum += Math.max(pm.getCurrentStepNumber() - 1, 0);
        }
        return sum;
    }

    public double getProgress() {
        double sum = 0.0;
        if (relativeWieghts != null) {
            for (int i = 0, l = delegates.length; i < l; i++) {
                sum += delegates[i].getProgress() * relativeWieghts[i];
            }
        } else {
            for (ProgressMonitor pm : delegates) {
                sum += pm.getProgress();
            }
        }
        return sum / sumWeights;
    }

    public ProgressListener getProgressListener() {
        throw new UnsupportedOperationException();
    }
}
