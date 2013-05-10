package com.jivesoftware.jcalibrary.structures;

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

    /**
     * We need to hide this constructor so we force instances to have an ID
     */
    private JiveInstance() {
        super();
    }

    public JiveInstance(long customerInstallationId) {
        this.customerInstallationId = customerInstallationId;
    }

    /*************************** PROPERTIES ********************************************************/
    protected long customerInstallationId;

    public long getCustomerInstallationId() {
        return customerInstallationId;
    }

    /*************************** GRAPHIC** ********************************************************/
    private Transformation transformation = new Transformation();

    @Override
    protected void projectImpl(long nowMS, GeometryBuffer buffer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MouseTarget<JiveInstance> getMouseTarget() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
