package net.venaglia.common.util;

/**
 * User: ed
 * Date: 12/6/12
 * Time: 8:20 AM
 */
public interface ProgressMonitor {

    /**
     * @return The number of steps, or -1 if th number if steps is not known.
     */
    int getNumberOfSteps();

    /**
     * @return The current step number, or -1 if not known.
     */
    int getCurrentStepNumber();

    /**
     * @return The name if the current step, or null if there is none.
     */
    String getCurrentStepName();

    /**
     * @return current progress, represented as a value between 0 and 1, or NaN
     *     if it is not known.
     */
    double getProgress();

    /**
     * @return a ProgressListener that can be used to drive this  monitor.
     * @throws UnsupportedOperationException if this ProgressMonitor does not
     *     support ProgressListeners for updating progress.
     */
    ProgressListener getProgressListener();

}
