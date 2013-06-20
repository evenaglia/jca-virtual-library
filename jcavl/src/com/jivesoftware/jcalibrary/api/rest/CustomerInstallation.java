package com.jivesoftware.jcalibrary.api.rest;

/**
 * This represents the configuration information around a "CustomerInstallation".
 * The CustomerInstallationDetail class contains more detail - such as the Clearspace instances associated with an installation, and
 * which user have rights to update the installation.
 */
public class CustomerInstallation {

    private long customerInstallationId;
    private String installationName;
    private String installationDescription;
    private InstallationType installationType;
    private String installationUrl;

    // Some installation URLs can't be shown in an iframe (For instance, the UAT ones...)
    private boolean installationUrlAccessible;
    private String analyticsDbDir;
    private String systemDbDir;
    private String eaeDbDir;
    private String cloneStatus;

    /**
     * Because this can be large, not loaded for grids
     */
    private String notes;
    /**
     * Because this can be large, not loaded for grids
     */
    private String ssoNotes;

    private String supportalKey;

    /**
     * Suffix of link to contract. I.E., for "https://na3.salesforce.com/80050000000W0qK", this will be "80050000000W0qK".
     */
    private String sforceContract;
    private Long costActual;
    private Long costBilled;
    private String version;
    private boolean inMaintenanceMode;
    private boolean active;
    private boolean additionalRestart;
    private boolean hasVPN = false;
    private boolean hasLDAP = false;
    private boolean hasLogStreaming = false;
    private String installationSystemName;
    private boolean seed = false;
    private String jiveInstanceId;
    private String zenossCollector;
    private String zenossMaster;

    public CustomerInstallation(){}

    // new customer installation no id
    public CustomerInstallation(String installationName,
                                String installationDescription,
                                InstallationType installationType,
                                String installationUrl,
                                boolean installationUrlAccessible,
                                String analyticsDbDir,
                                String systemDbDir,
                                String eaeDbDir,
                                String notes,
                                String ssoNotes,
                                String supportalKey,
                                String sforceContract,
                                Long costActual,
                                Long costBilled,
                                String version,
                                boolean inMaintenanceMode,
                                boolean active,
                                boolean additionalRestart,
                                String installationSystemName,
                                boolean seed,
                                String jiveInstanceId) {
        this(installationName,
                installationDescription,
                installationType,
                installationUrl,
                installationUrlAccessible,
                analyticsDbDir,
                systemDbDir,
                eaeDbDir,
                notes,
                ssoNotes,
                supportalKey,
                sforceContract,
                costActual,
                costBilled,
                version,
                inMaintenanceMode,
                active,
                additionalRestart,
                false,
                false,
                false,
                installationSystemName,
                seed,
                jiveInstanceId);
    }

    // existing customer installation with id
    public CustomerInstallation(long customerInstallationId,
                                String installationName,
                                String installationDescription,
                                InstallationType installationType,
                                String installationUrl,
                                boolean installationUrlAccessible,
                                String analyticsDbDir,
                                String systemDbDir,
                                String eaeDbDir,
                                String notes,
                                String ssoNotes,
                                String supportalKey,
                                String sforceContract,
                                Long costActual,
                                Long costBilled,
                                String version,
                                boolean inMaintenanceMode,
                                boolean active,
                                boolean additionalRestart,
                                String cloneStatus,
                                boolean seed,
                                String jiveInstanceId) {
        this(customerInstallationId,
                installationName,
                installationDescription,
                installationType,
                installationUrl,
                installationUrlAccessible,
                analyticsDbDir,
                systemDbDir,
                eaeDbDir,
                notes,
                ssoNotes,
                supportalKey,
                sforceContract,
                costActual,
                costBilled,
                version,
                inMaintenanceMode,
                active,
                additionalRestart,
                false,
                false,
                false,
                installationName,
                cloneStatus,

                seed,
                jiveInstanceId,
                "",
                "");
    }

    // new customer installation with hasVPN and hasLDAP specified
    public CustomerInstallation(String installationName,
                                String installationDescription,
                                InstallationType installationType,
                                String installationUrl,
                                boolean installationUrlAccessible,

                                String analyticsDbDir,
                                String systemDbDir,
                                String eaeDbDir,
                                String notes,
                                String ssoNotes,
                                String supportalKey,
                                String sforceContract,
                                Long costActual,
                                Long costBilled,
                                String version,
                                boolean inMaintenanceMode,
                                boolean active,
                                boolean additionalRestart,
                                boolean hasVPN,
                                boolean hasLDAP,
                                boolean hasLogStreaming,
                                String installationSystemName,

                                boolean seed,
                                String jiveInstanceId) {
        this.installationName = installationName;
        this.installationDescription = installationDescription;
        this.installationType = installationType;
        this.installationUrl = installationUrl;
        this.installationUrlAccessible = installationUrlAccessible;

        this.analyticsDbDir = analyticsDbDir;
        this.systemDbDir = systemDbDir;
        this.eaeDbDir = eaeDbDir;
        this.notes = notes;
        this.ssoNotes = ssoNotes;
        this.supportalKey = supportalKey;
        this.sforceContract = sforceContract;
        this.costActual = costActual;
        this.costBilled = costBilled;
        this.version = version;
        this.inMaintenanceMode = inMaintenanceMode;
        this.active = active;
        this.additionalRestart = additionalRestart;
        this.hasVPN = hasVPN;
        this.hasLDAP = hasLDAP;
        this.hasLogStreaming = hasLogStreaming;
        this.installationSystemName = installationSystemName;

        this.seed = seed;
        this.jiveInstanceId = jiveInstanceId;

    }

    // update customer installation with hasVPN and hasLDAP
    public CustomerInstallation(long customerInstallationId,
                                String installationName,
                                String installationDescription,
                                InstallationType installationType,
                                String installationUrl,
                                boolean installationUrlAccessible,

                                String analyticsDbDir,
                                String systemDbDir,
                                String eaeDbDir,
                                String notes,
                                String ssoNotes,
                                String supportalKey,
                                String sforceContract,
                                Long costActual,
                                Long costBilled,
                                String version,
                                boolean inMaintenanceMode,
                                boolean active,
                                boolean additionalRestart,
                                boolean hasVPN,
                                boolean hasLDAP,
                                boolean hasLogStreaming,
                                String cloneStatus,
                                String installationSystemName,
                                boolean seed,
                                String jiveInstanceId,
                                String zenossCollector,
                                String zenossMaster) {
        this.customerInstallationId = customerInstallationId;
        this.installationName = installationName;
        this.installationDescription = installationDescription;
        this.installationType = installationType;
        this.installationUrl = installationUrl;
        this.installationUrlAccessible = installationUrlAccessible;
        this.analyticsDbDir = analyticsDbDir;
        this.systemDbDir = systemDbDir;
        this.eaeDbDir = eaeDbDir;
        this.notes = notes;
        this.ssoNotes = ssoNotes;
        this.supportalKey = supportalKey;
        this.sforceContract = sforceContract;
        this.costActual = costActual;
        this.costBilled = costBilled;
        this.version = version;
        this.inMaintenanceMode = inMaintenanceMode;
        this.active = active;
        this.additionalRestart = additionalRestart;
        this.hasVPN = hasVPN;
        this.hasLDAP = hasLDAP;
        this.hasLogStreaming = hasLogStreaming;
        this.cloneStatus = cloneStatus;
        this.installationSystemName = installationSystemName;
        this.seed = seed;
        this.jiveInstanceId = jiveInstanceId;
        this.zenossCollector = zenossCollector;
        this.zenossMaster = zenossMaster;
    }


    /**
     * Used when loading from the DB
     *
     * @return cutomer id
     */
    public long getCustomerInstallationId() {
        return customerInstallationId;
    }

    public String getInstallationName() {
        return installationName;
    }

    public String getInstallationDescription() {
        return installationDescription;
    }

    public InstallationType getInstallationType() {
        return installationType;
    }

    public String getInstallationUrl() {
        return installationUrl;
    }

    public boolean isInstallationUrlAccessible() {
        return installationUrlAccessible;
    }


    public void setInstallationName(String installationName) {
        this.installationName = installationName;
    }

    public void setInstallationDescription(String installationDescription) {
        this.installationDescription = installationDescription;
    }

    public void setInstallationType(InstallationType installationType) {
        this.installationType = installationType;
    }

    public void setInstallationUrl(String installationUrl) {
        this.installationUrl = installationUrl;
    }

    public void setInstallationUrlAccessible(boolean installationUrlAccessible) {
        this.installationUrlAccessible = installationUrlAccessible;
    }


    public String getAnalyticsDbDir() {
        return analyticsDbDir;
    }

    public void setAnalyticsDbDir(String analyticsDbDir) {
        this.analyticsDbDir = analyticsDbDir;
    }

    public String getSystemDbDir() {
        return systemDbDir;
    }

    public void setSystemDbDir(String systemDbDir) {
        this.systemDbDir = systemDbDir;
    }

    public String getEaeDbDir() {
        return eaeDbDir;
    }

    public void setEaeDbDir(String eaeDbDir) {
        this.eaeDbDir = eaeDbDir;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSsoNotes() {
        return ssoNotes;
    }

    public void setSsoNotes(String ssoNotes) {
        this.ssoNotes = ssoNotes;
    }

    public String getSupportalKey() {
        return supportalKey;
    }

    public void setSupportalKey(String supportalKey) {
        this.supportalKey = supportalKey;
    }

    public String getSforceContract() {
        return sforceContract;
    }

    public void setSforceContract(String sforceContract) {
        this.sforceContract = sforceContract;
    }

    public Long getCostActual() {
        return costActual;
    }

    public void setCostActual(Long costActual) {
        this.costActual = costActual;
    }

    public Long getCostBilled() {
        return costBilled;
    }

    public void setCostBilled(Long costBilled) {
        this.costBilled = costBilled;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isInMaintenanceMode() {
        return inMaintenanceMode;
    }

    public void setInMaintenanceMode(boolean inMaintenanceMode) {
        this.inMaintenanceMode = inMaintenanceMode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAdditionalRestart() {
        return additionalRestart;
    }

    public boolean isHasVPN() {
        return hasVPN;
    }

    public void setHasVPN(boolean hasVPN) {
        this.hasVPN = hasVPN;
    }

    public boolean isHasLDAP() {
        return hasLDAP;
    }

    public void setHasLDAP(boolean hasLDAP) {
        this.hasLDAP = hasLDAP;
    }

    public String getCloneStatus() {
        return cloneStatus;
    }

    public void setCloneStatus(String cloneStatus) {
        this.cloneStatus = cloneStatus;
    }

    public String getInstallationSystemName() {
        return installationSystemName;
    }

    public void setInstallationSystemName(String installationSystemName) {
        this.installationSystemName = installationSystemName;
    }


    public boolean isHasLogStreaming() {
        return hasLogStreaming;
    }

    public void setHasLogStreaming(boolean hasLogStreaming) {
        this.hasLogStreaming = hasLogStreaming;
    }


    public boolean isSeed() {
        return seed;
    }

    public void setSeed(boolean seed) {
        this.seed = seed;
    }

    public String getZenossCollector() {
        return zenossCollector;
    }

    public void setZenossCollector(String zenossCollector) {
        this.zenossCollector = zenossCollector;
    }

    public String getZenossMaster() {
        return zenossMaster;
    }

    public void setZenossMaster(String zenossMaster) {
        this.zenossMaster = zenossMaster;
    }

    /**
     * Return true if this is a cloud installation.
     *
     * @return
     */
    public boolean isCloud() {
        boolean cloud = false;

        if (!InstallationType.HOSTED_TYPES.contains(installationType) && !InstallationType.BARE_METAL_TYPES.contains(installationType)) {
            cloud = true;
        }
        return cloud;
    }

    public boolean isHosted() {
        boolean hosted = false;

        if (InstallationType.HOSTED_TYPES.contains(installationType)) {
            hosted = true;
        }
        return hosted;
    }

    public boolean isBareMetal() {
        boolean bareMetal = false;

        if (InstallationType.BARE_METAL_TYPES.contains(installationType)) {
            bareMetal = true;
        }
        return bareMetal;
    }


    public String getJiveInstanceId() {
        return jiveInstanceId;
    }

    public void setJiveInstanceId(String jiveInstanceId) {
        this.jiveInstanceId = jiveInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CustomerInstallation))
            return false;

        CustomerInstallation that = (CustomerInstallation) o;

        if (active != that.active)
            return false;
        if (additionalRestart != that.additionalRestart)
            return false;
        if (customerInstallationId != that.customerInstallationId)
            return false;
        if (inMaintenanceMode != that.inMaintenanceMode)
            return false;
        if (installationUrlAccessible != that.installationUrlAccessible)
            return false;
        if (analyticsDbDir != null ? !analyticsDbDir.equals(that.analyticsDbDir) : that.analyticsDbDir != null)
            return false;
        if (!cloneStatus.equals(that.cloneStatus))
            return false;
        if (costActual != null ? !costActual.equals(that.costActual) : that.costActual != null)
            return false;
        if (costBilled != null ? !costBilled.equals(that.costBilled) : that.costBilled != null)
            return false;
        if (eaeDbDir != null ? !eaeDbDir.equals(that.eaeDbDir) : that.eaeDbDir != null)
            return false;
        if (installationDescription != null ? !installationDescription.equals(that.installationDescription) : that.installationDescription != null)
            return false;

        if (installationName != null ? !installationName.equals(that.installationName) : that.installationName != null)
            return false;
        if (installationType != that.installationType)
            return false;
        if (installationUrl != null ? !installationUrl.equals(that.installationUrl) : that.installationUrl != null)
            return false;
        if (jiveInstanceId != null ? !jiveInstanceId.equals(that.jiveInstanceId) : that.jiveInstanceId != null)
            return false;
        if (notes != null ? !notes.equals(that.notes) : that.notes != null)
            return false;
        if (sforceContract != null ? !sforceContract.equals(that.sforceContract) : that.sforceContract != null)
            return false;
        if (ssoNotes != null ? !ssoNotes.equals(that.ssoNotes) : that.ssoNotes != null)
            return false;
        if (supportalKey != null ? !supportalKey.equals(that.supportalKey) : that.supportalKey != null)
            return false;
        if (systemDbDir != null ? !systemDbDir.equals(that.systemDbDir) : that.systemDbDir != null)
            return false;
        if (version != null ? !version.equals(that.version) : that.version != null)
            return false;
        if (hasVPN != that.hasVPN)
            return false;
        if (hasLDAP != that.hasLDAP)
            return false;
        if (hasLogStreaming != that.hasLogStreaming) {
            return false;
        }
        if (installationSystemName != null ? !installationSystemName.equals(that.installationSystemName) : that.installationSystemName != null)
            return false;

        if (seed != that.seed) {
            return false;
        }
        if (zenossCollector != null ? !zenossCollector.equals(that.zenossCollector) : that.zenossCollector != null)
            return false;
        if (zenossMaster != null ? !zenossMaster.equals(that.zenossMaster) : that.zenossMaster != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (customerInstallationId ^ (customerInstallationId >>> 32));
        result = 31 * result + (installationName != null ? installationName.hashCode() : 0);
        result = 31 * result + (installationDescription != null ? installationDescription.hashCode() : 0);
        result = 31 * result + (installationType != null ? installationType.hashCode() : 0);
        result = 31 * result + (installationUrl != null ? installationUrl.hashCode() : 0);
        result = 31 * result + (installationUrlAccessible ? 1 : 0);
        result = 31 * result + (analyticsDbDir != null ? analyticsDbDir.hashCode() : 0);
        result = 31 * result + (systemDbDir != null ? systemDbDir.hashCode() : 0);
        result = 31 * result + (eaeDbDir != null ? eaeDbDir.hashCode() : 0);
        result = 31 * result + (cloneStatus != null ? cloneStatus.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        result = 31 * result + (ssoNotes != null ? ssoNotes.hashCode() : 0);
        result = 31 * result + (supportalKey != null ? supportalKey.hashCode() : 0);
        result = 31 * result + (sforceContract != null ? sforceContract.hashCode() : 0);
        result = 31 * result + (costActual != null ? costActual.hashCode() : 0);
        result = 31 * result + (costBilled != null ? costBilled.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (inMaintenanceMode ? 1 : 0);
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + (additionalRestart ? 1 : 0);
        result = 31 * result + (hasVPN ? 1 : 0);
        result = 31 * result + (hasLDAP ? 1 : 0);
        result = 31 * result + (hasLogStreaming ? 1 : 0);
        result = 31 * result + (installationSystemName != null ? installationSystemName.hashCode() : 0);
        result = 31 * result + (jiveInstanceId != null ? jiveInstanceId.hashCode() : 0);
        result = 31 * result + (zenossMaster != null ? zenossMaster.hashCode() : 0);
        result = 31 * result + (zenossCollector != null ? zenossCollector.hashCode() : 0);
        return result;
    }


}
