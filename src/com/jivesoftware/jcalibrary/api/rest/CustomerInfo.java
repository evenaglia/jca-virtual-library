package com.jivesoftware.jcalibrary.api.rest;

import java.util.Date;

public class CustomerInfo {

    private long customerId;
    private String name;
    private String accountKey;
    private String jiveContactEmail;
    private CustomerType customerType;
    private Date deployDate;
    private String domain;

    /**
     * Since this can be large, currently isn't load for grid views.
     */
    private String notes;

    public CustomerInfo() {}

    public CustomerInfo(long customerId, String name, String accountKey, String jiveContactEmail, CustomerType customerType, String notes, String domain) {
        this.customerId = customerId;
        this.name = name;
        this.accountKey = accountKey;
        this.jiveContactEmail = jiveContactEmail;
        this.customerType = customerType;
        this.notes = notes;
        this.domain = domain;
    }

    public CustomerInfo(long customerId, String name, String accountKey, String jiveContactEmail, CustomerType customerType, Date deployDate, String notes, String domain) {
        this.customerId = customerId;
        this.name = name;
        this.accountKey = accountKey;
        this.jiveContactEmail = jiveContactEmail;
        this.customerType = customerType;
        this.deployDate = deployDate;
        this.notes = notes;
        this.domain = domain;
    }

    public Date getDeployDate() {
        return deployDate;
    }

    public long getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public String getJiveContactEmail() {
        return jiveContactEmail;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public String getNotes() {
        return notes;
    }

    public String getDomain() {
        return domain;
    }

    public String toString() {
        return "[CustomerInfo:" + customerId + "," + name + "," + accountKey + "," + jiveContactEmail + "," + customerType + "," +  domain + "]";
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomerInfo that = (CustomerInfo) o;

        if (customerId != that.customerId) return false;
        if (accountKey != null ? !accountKey.equals(that.accountKey) : that.accountKey != null) return false;
        if (customerType != null ? !customerType.equals(that.customerType) : that.customerType != null) return false;
        if (deployDate != null ? !deployDate.equals(that.deployDate) : that.deployDate != null) return false;
        if (jiveContactEmail != null ? !jiveContactEmail.equals(that.jiveContactEmail) : that.jiveContactEmail != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (domain != null ? !domain.equals(that.domain) : that.domain != null) return false;
        if (notes != null ? !notes.equals(that.notes) : that.notes != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (customerId ^ (customerId >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (accountKey != null ? accountKey.hashCode() : 0);
        result = 31 * result + (jiveContactEmail != null ? jiveContactEmail.hashCode() : 0);
        result = 31 * result + (customerType != null ? customerType.hashCode() : 0);
        result = 31 * result + (deployDate != null ? deployDate.hashCode() : 0);
        result = 31 * result + (notes != null ? notes.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        return result;
    }
}
