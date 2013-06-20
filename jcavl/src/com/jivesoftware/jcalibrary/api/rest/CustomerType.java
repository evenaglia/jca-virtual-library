package com.jivesoftware.jcalibrary.api.rest;

import java.util.HashMap;
import java.util.Map;

public class CustomerType {

    private static Map<String, CustomerType> codeToTypeMap = new HashMap<String, CustomerType>();

    public static final CustomerType STANDARD = new CustomerType("standard");
    public static final CustomerType SANDBOX = new CustomerType("sandbox");

    private String code;

    public CustomerType(){}

    private CustomerType(String code) {
        this.code = code;
        codeToTypeMap.put(code, this);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public static CustomerType getTypeFromCode(String code) {
        return codeToTypeMap.get(code);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomerType that = (CustomerType) o;

        if (code != null ? !code.equals(that.code) : that.code != null) return false;

        return true;
    }

    public int hashCode() {
        return (code != null ? code.hashCode() : 0);
    }

    public String toString() {
        return "[CustomerType:" + code + "]";
    }
}
