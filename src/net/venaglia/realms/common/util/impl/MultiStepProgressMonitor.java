package net.venaglia.realms.common.util.impl;

import net.venaglia.realms.common.util.ProgressListener;
import net.venaglia.realms.common.util.ProgressMonitor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 12/6/12
 * Time: 8:22 AM
 */
public class MultiStepProgressMonitor implements ProgressMonitor {

    private final int numberOfSteps;
    private final AtomicInteger currentStep = new AtomicInteger(1);

    public MultiStepProgressMonitor(int numberOfSteps) {
        if (numberOfSteps <= 0) {
            throw new IllegalArgumentException();
        }
        this.numberOfSteps = numberOfSteps;
    }

    public int getNumberOfSteps() {
        return numberOfSteps;
    }

    public int getCurrentStepNumber() {
        return currentStep.get();
    }

    public void setCurrentStep(int currentStep) {
        if (currentStep <= 0 || currentStep > numberOfSteps) {
            throw new IllegalArgumentException();
        }
        this.currentStep.set(currentStep);
    }

    public double getProgress() {
        return Math.min((currentStep.doubleValue() - 1.0) / numberOfSteps, 1.0);
    }

    public ProgressListener getProgressListener() {
        return new ProgressListener() {
            int step = getCurrentStepNumber();
            public void nextStep() {
                int now = currentStep.get();
                if (now > numberOfSteps) return;
                while (!currentStep.compareAndSet(now, now + 1)) {
                    now = currentStep.get();
                    if (now > numberOfSteps) return;
                }
            }
        };
    }
}
