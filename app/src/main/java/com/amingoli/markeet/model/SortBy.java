package com.amingoli.markeet.model;

public class SortBy {

    public String label;
    public String column;
    public String order;

    public SortBy() {
    }

    public SortBy(String label, String column, String order) {
        this.label = label;
        this.column = column;
        this.order = order;
    }

}
