package net.venaglia.realms.common.util.work;

/**
 * User: ed
 * Date: 9/3/14
 * Time: 10:23 PM
 */
public interface ProgressExportingRunnable extends Runnable {


    interface ProgressExporter {

        /**
         * @param soFar steps completed
         * @param total steps total
         */
        void exportProgress(long soFar, long total);

        /**
         * @param percentage incremental completion, from 0 to 1
         */
        void exportProgress(double percentage);

        /**
         * @throws IllegalStateException if the previous call is not {@link ProgressExportingRunnable.ProgressExporter#exportProgress(long, long)}
         */
        void oneMore();
    }

    void setProgressExporter(ProgressExporter progressExporter);
}
