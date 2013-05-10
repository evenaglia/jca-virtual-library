package com.jivesoftware.jcalibrary.api;

import com.jivesoftware.jcalibrary.structures.JiveInstance;

import java.util.List;

/**
 * TestJCAMain
 */
public class TestJCAMain {

    public static void main(String[] args) throws Exception{
        JCAManager jcaManager = JCAManager.INSTANCE;
        List<JiveInstance> jiveinstances = jcaManager.fetchInstallations();

    }

}
