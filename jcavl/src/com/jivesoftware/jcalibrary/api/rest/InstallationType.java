package com.jivesoftware.jcalibrary.api.rest;

import java.util.Arrays;
import java.util.List;

public enum InstallationType {

    // Needs to be kept in sync with contents of DB Table jiveInstallationType

    STANDARD("standard", "Normal CS Installation", "/images/installation-standard-16x16.png", false, false, false, false, false),
    //SANDBOX("sandbox", "Sandbox CS Installation", "/images/installation-uat-16x16.png", false, false, false),
    UAT("uat", "UAT/Staging Installation", "/images/installation-uat-16x16.png", false, true, false, false, false),
    UAT_CLONE("uat_clone", "UAT/Staging Installation", "/images/installation-uat-16x16.png", false, true, false, true, false),
    UAT_MIGRATED("uat_migrated", "UAT/Staging Installation", "/images/installation-uat-16x16.png", false, true, false, false, true),
    PRODUCTION("production", "Production Installation", "/images/installation-production-16x16.png", false, false, false, false, false),
    PRODUCTION_CLONE("production_clone", "Production Installation", "/images/installation-production-16x16.png", false, false, false, true, false),
    PRODUCTION_MIGRATED("production_migrated", "Production Installation", "/images/installation-production-16x16.png", false, false, false, false, true),
    JCLOUD_TEST("jcloudtest", "Hosting Test Installation  (jCloud)", "/images/installation-test-16x16.png", true, false, true, false, false),
    JCLOUD_TEST_CLONE("jcloudtest_clone", "Hosting Test Installation  (jCloud)", "/images/installation-test-16x16.png", true, false, true, true, false),
    JCLOUD_TEST_MIGRATED("jcloudtest_migrated", "Hosting Test Installation  (jCloud)", "/images/installation-test-16x16.png", true, false, true, false, true),
    JCLOUD_UAT("jclouduat", "UAT/Staging Installation  (jCloud)", "/images/installation-uat-16x16.png", true, true, false, false, false),
    JCLOUD_UAT_CLONE("jclouduat_clone", "UAT/Staging Installation  (jCloud)", "/images/installation-uat-16x16.png", true, true, false, true, false),
    JCLOUD_UAT_MIGRATED("jclouduat_migrated", "UAT/Staging Installation  (jCloud)", "/images/installation-uat-16x16.png", true, true, false, false, true),
    JCLOUD_PRODUCTION("jcloudproduction", "Production Installation (jCloud)", "/images/installation-production-16x16.png", true, false, false, false, false),
    JCLOUD_PRODUCTION_CLONE("jcloudproduction_clone", "Production Installation (jCloud)", "/images/installation-production-16x16.png", true, false, false, true, false),
    JCLOUD_PRODUCTION_MIGRATED("jcloudproduction_migrated", "Production Installation (jCloud)", "/images/installation-production-16x16.png", true, false, false, false, true),
    THUNDER("thunder", "Thunder", "/images/thunder-16x16.png", true, false, false, false, false),
    THUNDER_CLONE("thunder_clone", "Thunder", "/images/thunder-16x16.png", true, false, false, true, false),
    THUNDER_MIGRATED("thunder_clone", "Thunder", "/images/thunder-16x16.png", true, false, false, false, true);

    public static final List<InstallationType> HOSTED_VIRTUAL_TYPES = Arrays.asList(InstallationType.JCLOUD_PRODUCTION,
            InstallationType.JCLOUD_PRODUCTION_CLONE,
            InstallationType.JCLOUD_PRODUCTION_MIGRATED,
            InstallationType.JCLOUD_UAT,
            InstallationType.JCLOUD_UAT_CLONE,
            InstallationType.JCLOUD_UAT_MIGRATED,
            InstallationType.JCLOUD_TEST,
            InstallationType.JCLOUD_TEST_CLONE,
            InstallationType.JCLOUD_TEST_MIGRATED);

    public static final List<InstallationType> ALL_VIRTUAL_TYPES = Arrays.asList(
            InstallationType.JCLOUD_PRODUCTION, InstallationType.JCLOUD_PRODUCTION_CLONE, InstallationType.JCLOUD_PRODUCTION_MIGRATED,
            InstallationType.JCLOUD_UAT,InstallationType.JCLOUD_UAT_CLONE, InstallationType.JCLOUD_UAT_MIGRATED,
            InstallationType.JCLOUD_TEST, InstallationType.JCLOUD_TEST_CLONE, InstallationType.JCLOUD_TEST_MIGRATED,
            InstallationType.THUNDER);

    public static final List<InstallationType> BARE_METAL_TYPES = Arrays.asList(
            InstallationType.UAT,
            InstallationType.PRODUCTION
    );

    public static final List<InstallationType> HOSTED_TYPES = Arrays.asList(
            InstallationType.STANDARD,
            InstallationType.UAT_CLONE,
            InstallationType.UAT_MIGRATED,
            InstallationType.PRODUCTION_CLONE,
            InstallationType.PRODUCTION_MIGRATED,
            InstallationType.JCLOUD_TEST,
            InstallationType.JCLOUD_TEST_CLONE,
            InstallationType.JCLOUD_TEST_MIGRATED,
            InstallationType.JCLOUD_UAT,
            InstallationType.JCLOUD_UAT_CLONE,
            InstallationType.JCLOUD_UAT_MIGRATED,
            InstallationType.JCLOUD_PRODUCTION,
            InstallationType.JCLOUD_PRODUCTION_CLONE,
            InstallationType.JCLOUD_PRODUCTION_MIGRATED
    );

    private String typeCode;
    private String description;
    private String icon;
    private boolean jcloud;
    private boolean uat;
    private boolean test;
    private boolean clone;
    private boolean migrated;

    InstallationType(String typeCode, String description, String icon, boolean jcloud, boolean uat, boolean test, boolean clone, boolean migrated) {
        this.typeCode = typeCode;
        this.description = description;
        this.icon = icon;
        this.jcloud = jcloud;
        this.uat = uat;
        this.test = test;
        this.clone = clone;
        this.migrated = migrated;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isJcloud() {
        return jcloud;
    }

    public boolean isUat() {
        return uat;
    }

    public boolean isTest() {
        return test;
    }

    public boolean isClone() {
        return clone;
    }

    public boolean isMigrated() {
        return migrated;
    }

    public boolean canBeCloned() {
        return !(this.isClone() || this.isMigrated());
    }

    public boolean isProd() {
        return this == JCLOUD_PRODUCTION || this == JCLOUD_PRODUCTION_CLONE || this == JCLOUD_PRODUCTION_MIGRATED || this == PRODUCTION;
    }

    public static InstallationType getFromTypeCode(String typeCode) {
        InstallationType retVal = STANDARD;

        for ( InstallationType ic : InstallationType.values()) {
            if ( ic.typeCode.equalsIgnoreCase(typeCode)) {
                retVal = ic;
            }
        }
        return retVal;
    }
}
