package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.api.rest.CustomerInfo;
import com.jivesoftware.jcalibrary.api.rest.CustomerInstallation;
import com.jivesoftware.jcalibrary.objects.AbstractLibraryElement;
import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.view.MouseTarget;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 5:22 PM
 */
public class JiveInstance extends AbstractLibraryElement<JiveInstance> {

    public enum Grouping {
        Standard,
        UAT,
        Production,
        Test,
        Thunder
    }

    /**
     * We need to hide this constructor so we force instances to have an ID
     */
    private JiveInstance() {
        super();
    }

    public JiveInstance(long customerInstallationId) {
        this.customerInstallationId = customerInstallationId;
    }

    /**
     * ************************ PROPERTIES *******************************************************
     */
    protected long customerInstallationId;
    private CustomerInfo customer;
    private CustomerInstallation installation;

    public CustomerInfo getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerInfo customer) {
        this.customer = customer;
    }

    public CustomerInstallation getInstallation() {
        return installation;
    }

    public void setInstallation(CustomerInstallation installation) {
        this.installation = installation;
    }

    public long getCustomerInstallationId() {
        return customerInstallationId;
    }

    /**
     * ************************ GRAPHIC** *******************************************************
     */
    private SlotTransformation slotTransformation = null;

    public SlotTransformation getSlotTransformation() {
        return slotTransformation;
    }

    public void setSlotTransformation(SlotTransformation slotTransformation) {
        this.slotTransformation = slotTransformation;
    }

    @Override
    protected void projectImpl(long nowMS, GeometryBuffer buffer) {
        if (slotTransformation != null) {
            buffer.pushTransform();
            slotTransformation.apply(nowMS, buffer);
            // todo: render the box, and all the components inside it
            buffer.popTransform();
        }
    }

    @Override
    public MouseTarget<JiveInstance> getMouseTarget() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
