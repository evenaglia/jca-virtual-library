package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.api.rest.CustomerInfo;
import com.jivesoftware.jcalibrary.api.rest.CustomerInstallation;
import com.jivesoftware.jcalibrary.api.rest.InstallationPageViewBean;
import com.jivesoftware.jcalibrary.objects.Objects;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 5:22 PM
 */
public class JiveInstance implements Projectable {

    public enum Grouping {
        Production(Color.GREEN),
        UAT(new Color(1.0f, 0.9f, 0.0f)),
        Test(Color.BLUE),
        Thunder(Color.CYAN),
        Other(Color.MAGENTA);

        private static Grouping[] byRack = {
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Thunder,
                Production,
                Production,
                Production,
                Production,
                Production,
                Production,
                Production,
                UAT,
                UAT,
                UAT,
                UAT,
                UAT,
                UAT,
                UAT,
        };

        public final Color color;

        private Grouping(Color color) {
            this.color = color;
        }

        public static Grouping byRack(int rackNum) {
            return byRack[rackNum];
        }
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
    private long pageViews = -1;

    private Map<String,NodeDetails> nodeDetails = new ConcurrentHashMap<String,NodeDetails>();

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

    public void setCustomerInstallationId(long customerInstallationId) {
        this.customerInstallationId = customerInstallationId;
    }

    public long getPageViews() {
        return pageViews;
    }

    public void setPageViews(long pageViews) {
        this.pageViews = pageViews;
    }

    public NodeDetails getNodeDetails(String nodeId) {
        NodeDetails details = nodeDetails.get(nodeId);
        if (details == null) {
            details = new NodeDetails();
            nodeDetails.put(nodeId, details);
        }
        return details;
    }

    public Grouping getGrouping() {
        switch (installation.getInstallationType()) {
            case UAT:
            case UAT_CLONE:
            case UAT_MIGRATED:
            case JCLOUD_UAT:
            case JCLOUD_UAT_CLONE:
            case JCLOUD_UAT_MIGRATED:
                return Grouping.UAT;
            case PRODUCTION:
            case PRODUCTION_CLONE:
            case PRODUCTION_MIGRATED:
            case JCLOUD_PRODUCTION:
            case JCLOUD_PRODUCTION_CLONE:
            case JCLOUD_PRODUCTION_MIGRATED:
                return Grouping.Production;
            case JCLOUD_TEST:
            case JCLOUD_TEST_CLONE:
            case JCLOUD_TEST_MIGRATED:
                return Grouping.Test;
            case THUNDER:
            case THUNDER_CLONE:
            case THUNDER_MIGRATED:
                return Grouping.Thunder;
            default:
                return Grouping.Other;
        }
    }

    public void importFrom(JiveInstance that) {
        // todo: update fields
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
    public boolean isStatic() {
        return false;
    }

    @Override
    public void project(long nowMS, GeometryBuffer buffer) {
        if (slotTransformation != null) {
            buffer.pushTransform();
            buffer.identity();
            slotTransformation.apply(nowMS, buffer);
            Objects.ORIGIN.project(nowMS, buffer);
//            Objects.CUBE.project(nowMS, buffer);
            // todo: render the box, and all the components inside it
            buffer.popTransform();
        }
    }

}
