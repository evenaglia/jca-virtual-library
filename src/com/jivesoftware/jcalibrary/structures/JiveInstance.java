package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.api.rest.CustomerInfo;
import com.jivesoftware.jcalibrary.api.rest.CustomerInstallation;
import com.jivesoftware.jcalibrary.objects.Objects;
import com.jivesoftware.jcalibrary.objects.VisualObjects;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 5:22 PM
 */
public class JiveInstance implements Projectable {

    public enum Grouping {
        Production(Color.GREEN),
        UAT(Color.ORANGE),
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
    private boolean selected = false;
    private AtomicBoolean dirty = new AtomicBoolean();

    private Map<String,NodeDetails> nodeDetails = new ConcurrentHashMap<String,NodeDetails>();

    public CustomerInfo getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerInfo customer) {
        if (!eq(this.customer, customer)) {
            this.customer = customer;
            dirty.set(true);
        }
    }

    public CustomerInstallation getInstallation() {
        return installation;
    }

    public void setInstallation(CustomerInstallation installation) {
        if (!eq(this.installation, installation)) {
            this.installation = installation;
            dirty.set(true);
        }
    }

    public long getCustomerInstallationId() {
        return customerInstallationId;
    }

    public void setCustomerInstallationId(long customerInstallationId) {
        if (this.customerInstallationId != customerInstallationId) {
            this.customerInstallationId = customerInstallationId;
            dirty.set(true);
        }
    }

    public long getPageViews() {
        return pageViews;
    }

    public void setPageViews(long pageViews) {
        if (this.pageViews != pageViews) {
            this.pageViews = pageViews;
            dirty.set(true);
        }
    }

    public NodeDetails getNodeDetails(String nodeId) {
        NodeDetails details = nodeDetails.get(nodeId);
        if (details == null) {
            details = new NodeDetails(dirty);
            nodeDetails.put(nodeId, details);
            dirty.set(true);
        }
        return details;
    }

    public Map<String,NodeDetails> getAllNodeDetails() {
        return nodeDetails;
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
        this.customer = that.customer;
        this.installation = that.installation;
        this.pageViews = that.pageViews;
        this.visualObjects.clear();
        dirty.set(true);
    }


    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean checkAndClearDirty() {
        boolean dirty = this.dirty.getAndSet(false);
        if (dirty) {
            VisualObjects.OVERALL_STATE.clear(this);
        }
        return dirty;
    }

    private <T> boolean eq(T a, T b) {
        return a == b || !(a == null || b == null) && a.equals(b);
    }

    /**
     * ************************ GRAPHIC** *******************************************************
     */
    private VisualObjects visualObjects = new VisualObjects();

    public VisualObjects getVisualObjects() {
        return visualObjects;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public void project(long nowMS, GeometryBuffer buffer) {
//        if (customerInstallationId == 13989) { // Apple
//            "".toString();
//        }
        Objects.JIVE_INSTANCE.project(nowMS, buffer);
    }

    @Override
    public String toString() {
        if (customer == null) {
            return "JiveInstance[" + customerInstallationId + "]=???";
        } else {
            return "JiveInstance[" + customerInstallationId + "]='" + customer.getName() + "'";
        }
    }
}
