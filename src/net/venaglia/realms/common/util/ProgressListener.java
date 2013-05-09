package net.venaglia.realms.common.util;

/**
 * User: ed
 * Date: 12/10/12
 * Time: 6:12 PM
 *
 * Interface allowing for safe updating of progress. Implementations of ProgressListener must be thread safe.
*/
public interface ProgressListener {

    void nextStep();
}
