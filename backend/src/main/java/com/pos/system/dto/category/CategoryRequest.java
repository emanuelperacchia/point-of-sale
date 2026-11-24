package com.pos.system.dto.category;

public class CategoryRequest {
    private String name;
    private String description;
    private Boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
