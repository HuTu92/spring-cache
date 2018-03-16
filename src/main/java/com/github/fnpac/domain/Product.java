package com.github.fnpac.domain;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by 刘春龙 on 2018/3/5.
 */
public class Product implements Serializable {

    private long id;
    private String name;
    private BigDecimal price;
    private String category;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
