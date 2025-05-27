package com.example.snapnews.models;

public class FilterChip {
    private String name;
    private String category;
    private String country;
    private boolean isSelected;

    public FilterChip(String name, String category, String country) {
        this.name = name;
        this.category = category;
        this.country = country;
        this.isSelected = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}