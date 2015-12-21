package com.wispy.bestpractice.clustertest.model;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

/**
 * A simple space class to demonstrate partitioned processing of the data.
 *
 * @author Leonid_Poliakov
 */
@SpaceClass
public class Person {
    private String id;
    private String name;
    private Boolean processed;

    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }
}