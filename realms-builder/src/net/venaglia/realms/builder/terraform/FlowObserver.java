package net.venaglia.realms.builder.terraform;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 8:01 AM
 */
public interface FlowObserver {

    /**
     * Called at the end of every frame
     * @param queryInterface The access to obtain data for the globe
     */
    void frame(FlowQueryInterface queryInterface);
}

