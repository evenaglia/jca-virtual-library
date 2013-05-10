package com.jivesoftware.jcalibrary.structures;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The registry keeps track of all JiveInstances that exists in the system and helps with keeping track of
 * which ones are still not used by any slot, find them by UUID, or filter them by different criteria.
 */
public class JiveInstancesRegistry {
    // Create and keep track of single instance of this class
    private static JiveInstancesRegistry instance = new JiveInstancesRegistry();


    protected Map<Long, JiveInstance> instances = new ConcurrentHashMap<Long, JiveInstance>();
    protected Set<JiveInstance> availableInstances = new CopyOnWriteArraySet<JiveInstance>();

    private JiveInstancesRegistry() {
        super();
    }

    public JiveInstancesRegistry getInstance() {
        return instance;
    }

    /**
     * Returns an existing JiveInstance with the specified UUID of null if none was found.
     *
     * @param customerInstallationId unique identifier of the JiveInstance.
     * @return existing JiveInstance with the specified UUID of null if none was found.
     */
    public JiveInstance getJiveInstance(long customerInstallationId) {
        return instances.get(customerInstallationId);
    }

    /**
     * Adds a new instance to the registry. If this instance was already in the registry
     * then this is a no op.
     *
     * @param jiveInstance
     */
    public void addJiveInstance(JiveInstance jiveInstance) {
        JiveInstance existing = instances.put(jiveInstance.getCustomerInstallationId(), jiveInstance);
        if (existing == null) {
            // This is a real new instance
            availableInstances.add(jiveInstance);
        }
    }

    /**
     * Returns the {@link JiveInstance}s that have not been added to any {@link ServerSlot}.
     * <i>Note: Any attempt to modify the returned collection will result in an error</i>
     *
     * @return JiveInstances that have not been added to any ServerSlot.
     */
    public Set<JiveInstance> getUnslottedJiveInstances() {
        return Collections.unmodifiableSet(availableInstances);
    }

    /**
     * Sets the specified {@link JiveInstance} into the requested {@link ServerSlot}. If the slot
     * already had a JiveInstance then this instance will be removed from the slot and returned to
     * the list of unslotted instances.
     *
     * @param jiveInstance the JiveInstance to add to the slot.
     * @param serverSlot the slot to add the new JiveInstance to.
     */
    public void addJiveInstanceTo(JiveInstance jiveInstance, ServerSlot serverSlot) {
        JiveInstance existingSlottedInstance = serverSlot.getJiveInstance();
        if (existingSlottedInstance == jiveInstance) {
            // Do nothing since the jiveInstance is already in this slot
            return;
        }
        if (existingSlottedInstance != null) {
            // We need to return this existing slotted JiveInstance to the pool of available
            availableInstances.add(existingSlottedInstance);
        }
        // Mark that the newly slotted instance is no longer unslotted
        availableInstances.remove(jiveInstance);
        // Put ths new JiveInstance in the slot
        serverSlot.setJiveInstance(jiveInstance);
    }

}
