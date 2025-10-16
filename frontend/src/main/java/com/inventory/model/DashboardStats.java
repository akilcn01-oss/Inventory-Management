package com.inventory.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.*;

import java.util.List;

/**
 * Dashboard statistics model with JavaFX properties
 * Contains summary information for the dashboard view
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardStats {
    
    private final IntegerProperty totalProducts = new SimpleIntegerProperty();
    private final IntegerProperty totalCategories = new SimpleIntegerProperty();
    private final IntegerProperty lowStockCount = new SimpleIntegerProperty();
    private final DoubleProperty totalInventoryValue = new SimpleDoubleProperty();
    private final IntegerProperty recentProducts = new SimpleIntegerProperty();
    private final ListProperty<CategoryStats> topCategories = new SimpleListProperty<>();
    
    // Default constructor
    public DashboardStats() {}
    
    // Constructor with all fields
    public DashboardStats(int totalProducts, int totalCategories, int lowStockCount,
                         double totalInventoryValue, int recentProducts, List<CategoryStats> topCategories) {
        setTotalProducts(totalProducts);
        setTotalCategories(totalCategories);
        setLowStockCount(lowStockCount);
        setTotalInventoryValue(totalInventoryValue);
        setRecentProducts(recentProducts);
        setTopCategories(topCategories);
    }
    
    // Total products property methods
    public int getTotalProducts() {
        return totalProducts.get();
    }
    
    @JsonProperty("total_products")
    public void setTotalProducts(int totalProducts) {
        this.totalProducts.set(totalProducts);
    }
    
    public IntegerProperty totalProductsProperty() {
        return totalProducts;
    }
    
    // Total categories property methods
    public int getTotalCategories() {
        return totalCategories.get();
    }
    
    @JsonProperty("total_categories")
    public void setTotalCategories(int totalCategories) {
        this.totalCategories.set(totalCategories);
    }
    
    public IntegerProperty totalCategoriesProperty() {
        return totalCategories;
    }
    
    // Low stock count property methods
    public int getLowStockCount() {
        return lowStockCount.get();
    }
    
    @JsonProperty("low_stock_count")
    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount.set(lowStockCount);
    }
    
    public IntegerProperty lowStockCountProperty() {
        return lowStockCount;
    }
    
    // Total inventory value property methods
    public double getTotalInventoryValue() {
        return totalInventoryValue.get();
    }
    
    @JsonProperty("total_inventory_value")
    public void setTotalInventoryValue(double totalInventoryValue) {
        this.totalInventoryValue.set(totalInventoryValue);
    }
    
    public DoubleProperty totalInventoryValueProperty() {
        return totalInventoryValue;
    }
    
    // Recent products property methods
    public int getRecentProducts() {
        return recentProducts.get();
    }
    
    @JsonProperty("recent_products")
    public void setRecentProducts(int recentProducts) {
        this.recentProducts.set(recentProducts);
    }
    
    public IntegerProperty recentProductsProperty() {
        return recentProducts;
    }
    
    // Top categories property methods
    public List<CategoryStats> getTopCategories() {
        return topCategories.get();
    }
    
    @JsonProperty("top_categories")
    public void setTopCategories(List<CategoryStats> topCategories) {
        this.topCategories.set(javafx.collections.FXCollections.observableArrayList(topCategories));
    }
    
    public ListProperty<CategoryStats> topCategoriesProperty() {
        return topCategories;
    }
    
    // Utility methods
    
    /**
     * Get formatted total inventory value
     * @return Formatted value string
     */
    public String getFormattedTotalValue() {
        return String.format("$%,.2f", getTotalInventoryValue());
    }
    
    /**
     * Get low stock percentage
     * @return Percentage of products with low stock
     */
    public double getLowStockPercentage() {
        if (getTotalProducts() == 0) return 0.0;
        return (double) getLowStockCount() / getTotalProducts() * 100;
    }
    
    /**
     * Get formatted low stock percentage
     * @return Formatted percentage string
     */
    public String getFormattedLowStockPercentage() {
        return String.format("%.1f%%", getLowStockPercentage());
    }
    
    /**
     * Check if there are any low stock alerts
     * @return true if there are low stock items
     */
    public boolean hasLowStockAlerts() {
        return getLowStockCount() > 0;
    }
    
    /**
     * Get average products per category
     * @return Average products per category
     */
    public double getAverageProductsPerCategory() {
        if (getTotalCategories() == 0) return 0.0;
        return (double) getTotalProducts() / getTotalCategories();
    }
    
    /**
     * Get formatted average products per category
     * @return Formatted average string
     */
    public String getFormattedAverageProductsPerCategory() {
        return String.format("%.1f", getAverageProductsPerCategory());
    }
    
    @Override
    public String toString() {
        return String.format("DashboardStats{totalProducts=%d, totalCategories=%d, lowStockCount=%d, " +
                           "totalInventoryValue=%.2f, recentProducts=%d, topCategories=%d}",
                           getTotalProducts(), getTotalCategories(), getLowStockCount(),
                           getTotalInventoryValue(), getRecentProducts(), 
                           getTopCategories() != null ? getTopCategories().size() : 0);
    }
    
    /**
     * Category statistics inner class
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryStats {
        private final StringProperty name = new SimpleStringProperty();
        private final IntegerProperty count = new SimpleIntegerProperty();
        
        public CategoryStats() {}
        
        public CategoryStats(String name, int count) {
            setName(name);
            setCount(count);
        }
        
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
        
        public int getCount() {
            return count.get();
        }
        
        @JsonProperty("count")
        public void setCount(int count) {
            this.count.set(count);
        }
        
        public IntegerProperty countProperty() {
            return count;
        }
        
        @Override
        public String toString() {
            return String.format("CategoryStats{name='%s', count=%d}", getName(), getCount());
        }
    }
}
