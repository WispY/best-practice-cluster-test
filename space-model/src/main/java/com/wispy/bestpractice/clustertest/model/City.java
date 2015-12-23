package com.wispy.bestpractice.clustertest.model;

import java.io.Serializable;

/**
 * @author Leonid_Poliakov
 */
public class City implements Serializable {
    private String name;
    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}