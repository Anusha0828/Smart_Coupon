package com.couponloop.model;

import java.sql.Timestamp;

public class Category {
    private int categoryId;
    private String name;
    private String slug;
    private String description;
    private String iconClass;
    private boolean active;
    private int couponCount;
    private Timestamp createdAt;

    public Category() {}

    public Category(int categoryId, String name, String slug, String description, String iconClass, boolean active) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.iconClass = iconClass;
        this.active = active;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconClass() { return iconClass; }
    public void setIconClass(String iconClass) { this.iconClass = iconClass; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getCouponCount() { return couponCount; }
    public void setCouponCount(int couponCount) { this.couponCount = couponCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
