package com.jivesoftware.jcalibrary.api.rest;

import java.util.Date;
import java.util.List;

public class InstallationPageViewBean {

    private Date statTime;
    // Has PV # for each hour for each webapp in installation.
    private List<Long> pageViews;

    // Has PV # for each hour for each webapp in installation.
    private List<Long> botViews;

    // Has PV # for each hour for each webapp in installation.
    private List<Long> dataTxs;

    public InstallationPageViewBean(){}

    public InstallationPageViewBean(Date statTime, List<Long> pageViews, List<Long> botViews, List<Long> dataTxs) {
        this.statTime = statTime;
        this.pageViews = pageViews;
        this.botViews = botViews;
        this.dataTxs = dataTxs;
    }

    public Date getStatTime() {
        return statTime;
    }

    public List<Long> getPageViews() {
        return pageViews;
    }

    public List<Long> getBotViews() {
        return botViews;
    }

    public List<Long> getDataTxs() {
        return dataTxs;
    }

    public Long getTotalPageViewsForDay() {
        return 0L;
        //TODO: calculate
    }
}