package com.pos.system.dto.product;

public class ProductRequest {
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String tipo;
    private BigDecimal costoProduccion;
    private Integer categoryId;
    private Boolean active;

    // Getters y Setters
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getCostoProduccion() { return costoProduccion; }
    public void setCostoProduccion(BigDecimal costoProduccion) { this.costoProduccion = costoProduccion; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
