package net.venaglia.realms.builder.terraform.flow;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 8:03 AM
 */
public interface FlowQueryInterface {

    boolean isStable();

    void changeSettings(double fps, double timeScale);

    int getFrameCount();

    void query(Iterable<? extends FlowQuery> queries);

    void runNext(Iterable<? extends Runnable> work);
}
