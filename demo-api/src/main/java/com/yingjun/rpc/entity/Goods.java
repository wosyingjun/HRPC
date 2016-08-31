package com.yingjun.rpc.entity;

import java.io.Serializable;

/**
 * @author yingjun
 */
public class Goods implements Serializable{

    private String title;
    private float price;

    public Goods(String title, float price) {
        this.title = title;
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Goods{" +
                "title='" + title + '\'' +
                ", price=" + price +
                '}';
    }
}
