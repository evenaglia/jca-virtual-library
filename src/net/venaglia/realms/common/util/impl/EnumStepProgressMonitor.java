package net.venaglia.realms.common.util.impl;

import net.venaglia.realms.common.util.ProgressListener;
import net.venaglia.realms.common.util.ProgressMonitor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 12/6/12
 * Time: 5:54 PM
 */
public class EnumStepProgressMonitor<E extends Enum> implements ProgressMonitor {

    private final E[] names;
    private final AtomicInteger currentStep = new AtomicInteger(1);

    public EnumStepProgressMonitor(Class<E> type) {
        this.names = type.getEnumConstants();
    }

    public int getNumberOfSteps() {
        return names.length;
    }

    public int getCurrentStepNumber() {
        return currentStep.get();
    }

    public void setCurrentStep(E currentStep) {
        if (currentStep == null) {
            throw new NullPointerException();
        }
        this.currentStep.set(currentStep.ordinal() + 1);
    }

    public String getCurrentStepName() {
        return names[currentStep.get() - 1].name();
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
