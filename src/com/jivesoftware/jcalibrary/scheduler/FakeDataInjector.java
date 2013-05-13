package com.jivesoftware.jcalibrary.scheduler;

import com.jivesoftware.jcalibrary.JiveInstancesRegistry;
import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.NodeDetails;

import java.util.Random;

/**
 * User: ed
 * Date: 5/12/13
 * Time: 9:41 PM
 */
public class FakeDataInjector implements Runnable {

    private Random r = new Random();

    @Override
    public void run() {
        JiveInstancesRegistry registry = JiveInstancesRegistry.getInstance();
        for (JiveInstance instance : registry.getAllJiveInstances()) {
            for (NodeDetails nodeDetails : instance.getAllNodeDetails().values()) {
                nodeDetails.setActiveConnections(Math.abs((int)Math.round(r.nextGaussian() * 25)));
                nodeDetails.setActiveSessions(Math.abs((int)Math.round(r.nextGaussian() * 25)));
                nodeDetails.setLoadAverage(Math.abs((float)(r.nextGaussian() * 2)));
            }
        }
        System.out.println("Fake data has been populated");
    }
}
