package com.yxy.reactive.model;

import java.io.Serializable;

/**
 * Created by Nuclear on 2020/10/3
 */
public class Person implements Serializable {
    private String name;
    private String sex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    @Override
    public String toString() {
        return "{"
                + "\"name\":\""
                + name + '\"'
                + ",\"sex\":\""
                + sex + '\"'
                + "}";
    }
}
