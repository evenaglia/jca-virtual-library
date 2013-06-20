package net.venaglia.common.util.impl;

import net.venaglia.common.util.ProgressMonitor;
import net.venaglia.common.util.ProgressListener;
import net.venaglia.common.util.ProgressMonitor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 12/6/12
 * Time: 5:54 PM
 */
public class NamedStepProgressMonitor implements ProgressMonitor {

    private final String[] names;
    private final AtomicInteger currentStep = new AtomicInteger(1);

    public NamedStepProgressMonitor(String... names) {
        this.names = names;
    }

    public int getNumberOfSteps() {
        return names.length;
    }

    public int getCurrentStepNumber() {
        return currentStep.get();
    }

    public void setCurrentStep(int currentStep) {
        if (currentStep <= 0 || currentStep > names.length) {
            throw new IllegalArgumentException();
        }
        this.currentStep.set(currentStep);
    }

    public String getCurrentStepName() {
        return names[currentStep.get() - 1];
    }

    public double getProgress() {
        return Math.min((currentStep.doubleValue() - 1.0) / names.length, 1.0);
    }

    public ProgressListener getProgressListener() {
        return new ProgressListener() {
            public void nextStep() {
                int now = currentStep.get();
                if (now > names.length) return;
                while (!currentStep.compareAndSet(now, now + 1)) {
                    now = currentStep.get();
                    if (now > names.length) return;
                }
            }
        };
    }
}
