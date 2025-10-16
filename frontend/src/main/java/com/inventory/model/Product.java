package com.inventory.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Product model class with JavaFX properties for UI binding
 * Represents a product in the inventory system
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    
    // JavaFX properties for UI binding
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final DoubleProperty price = new SimpleDoubleProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>();
    
    // Default constructor
    public Product() {}
    
    // Constructor with basic fields
    public Product(String name, String category, int quantity, double price, String description) {
        setName(name);
        setCategory(category);
        setQuantity(quantity);
        setPrice(price);
        setDescription(description);
    }
    
    // Constructor with all fields
    public Product(int id, String name, String category, int quantity, double price, 
                   String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        setId(id);
        setName(name);
        setCategory(category);
        setQuantity(quantity);
        setPrice(price);
        setDescription(description);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
    }
    
    // ID property methods
    public int getId() {
        return id.get();
    }
    
    @JsonProperty("id")
    public void setId(int id) {
        this.id.set(id);
    }
    
    public IntegerProperty idProperty() {
        return id;
    }
    
    // Name property methods
    public String getName() {
        return name.get();
    }
    
    @JsonProperty("name")
    public void setName(String name) {
        this.name.set(name);
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    // Category property methods
    public String getCategory() {
        return category.get();
    }
    
    @JsonProperty("category")
    public void setCategory(String category) {
        this.category.set(category);
    }
    
    public StringProperty categoryProperty() {
        return category;
    }
    
    // Quantity property methods
    public int getQuantity() {
        return quantity.get();
    }
    
    @JsonProperty("quantity")
    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }
    
    public IntegerProperty quantityProperty() {
        return quantity;
    }
    
    // Price property methods
    public double getPrice() {
        return price.get();
    }
    
    @JsonProperty("price")
    public void setPrice(double price) {
        this.price.set(price);
    }
    
    public DoubleProperty priceProperty() {
        return price;
    }
    
    // Description property methods
    public String getDescription() {
        return description.get();
    }
    
    @JsonProperty("description")
    public void setDescription(String description) {
        this.description.set(description);
    }
    
    public StringProperty descriptionProperty() {
        return description;
    }
    
    // Created at property methods
    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }
    
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }
    
    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }
    
    // Updated at property methods
    public LocalDateTime getUpdatedAt() {
        return updatedAt.get();
    }
    
    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt.set(updatedAt);
    }
    
    public ObjectProperty<LocalDateTime> updatedAtProperty() {
        return updatedAt;
    }
    
    // Utility methods
    
    /**
     * Check if product is low stock based on threshold
     * @param threshold Low stock threshold
     * @return true if quantity is below threshold
     */
    public boolean isLowStock(int threshold) {
        return getQuantity() < threshold;
    }
    
    /**
     * Get total value of this product (quantity * price)
     * @return Total value
     */
    public double getTotalValue() {
        return getQuantity() * getPrice();
    }
    
    /**
     * Get formatted price string
     * @return Formatted price
     */
    public String getFormattedPrice() {
        return String.format("$%.2f", getPrice());
    }
    
    /**
     * Get formatted total value string
     * @return Formatted total value
     */
    public String getFormattedTotalValue() {
        return String.format("$%.2f", getTotalValue());
    }
    
    /**
     * Create a copy of this product
     * @return Product copy
     */
    public Product copy() {
        return new Product(getId(), getName(), getCategory(), getQuantity(), 
                          getPrice(), getDescription(), getCreatedAt(), getUpdatedAt());
    }
    
    /**
     * Validate product data
     * @return true if valid
     */
    public boolean isValid() {
        return getName() != null && !getName().trim().isEmpty() &&
               getCategory() != null && !getCategory().trim().isEmpty() &&
               getQuantity() >= 0 &&
               getPrice() > 0;
    }
    
    /**
     * Get validation error message
     * @return Error message or null if valid
     */
    public String getValidationError() {
        if (getName() == null || getName().trim().isEmpty()) {
            return "Product name is required";
        }
        if (getCategory() == null || getCategory().trim().isEmpty()) {
            return "Category is required";
        }
        if (getQuantity() < 0) {
            return "Quantity cannot be negative";
        }
        if (getPrice() <= 0) {
            return "Price must be greater than 0";
        }
        return null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return getId() == product.getId();
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    
    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', category='%s', quantity=%d, price=%.2f}", 
                           getId(), getName(), getCategory(), getQuantity(), getPrice());
    }
}
