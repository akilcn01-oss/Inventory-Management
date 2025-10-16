package com.inventory.controller;

import com.inventory.model.DashboardStats;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the dashboard view
 * Displays inventory statistics and charts
 */
public class DashboardController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @FXML private VBox dashboardRoot;
    @FXML private Label totalProductsLabel;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label lowStockCountLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label recentProductsLabel;
    @FXML private Label lowStockPercentageLabel;
    @FXML private PieChart categoryPieChart;
    @FXML private BarChart<String, Number> stockLevelChart;
    
    private MainController mainController;
    private DashboardStats currentStats;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initializing DashboardController");
        
        setupCharts();
        setupInitialData();
        
        logger.debug("DashboardController initialized");
    }
    
    /**
     * Setup charts with initial configuration
     */
    private void setupCharts() {
        // Configure pie chart
        categoryPieChart.setTitle("Products by Category");
        categoryPieChart.setLegendVisible(true);
        categoryPieChart.setLabelsVisible(true);
        
        // Configure bar chart
        stockLevelChart.setTitle("Stock Levels");
        stockLevelChart.getXAxis().setLabel("Categories");
        stockLevelChart.getYAxis().setLabel("Quantity");
        stockLevelChart.setLegendVisible(false);
    }
    
    /**
     * Setup initial data display
     */
    private void setupInitialData() {
        totalProductsLabel.setText("0");
        totalCategoriesLabel.setText("0");
        lowStockCountLabel.setText("0");
        totalValueLabel.setText("$0.00");
        recentProductsLabel.setText("0");
        lowStockPercentageLabel.setText("0.0%");
    }
    
    /**
     * Update dashboard with new statistics
     * @param stats Dashboard statistics
     */
    public void updateStats(DashboardStats stats) {
        if (stats == null) {
            logger.warn("Received null dashboard stats");
            return;
        }
        
        this.currentStats = stats;
        
        Platform.runLater(() -> {
            try {
                updateLabels(stats);
                updateCharts(stats);
                logger.debug("Dashboard updated with new stats");
            } catch (Exception e) {
                logger.error("Error updating dashboard", e);
            }
        });
    }
    
    /**
     * Update statistic labels
     * @param stats Dashboard statistics
     */
    private void updateLabels(DashboardStats stats) {
        totalProductsLabel.setText(String.valueOf(stats.getTotalProducts()));
        totalCategoriesLabel.setText(String.valueOf(stats.getTotalCategories()));
        lowStockCountLabel.setText(String.valueOf(stats.getLowStockCount()));
        totalValueLabel.setText(stats.getFormattedTotalValue());
        recentProductsLabel.setText(String.valueOf(stats.getRecentProducts()));
        lowStockPercentageLabel.setText(stats.getFormattedLowStockPercentage());
        
        // Apply warning style for low stock
        if (stats.hasLowStockAlerts()) {
            lowStockCountLabel.getStyleClass().add("warning-text");
            lowStockPercentageLabel.getStyleClass().add("warning-text");
        } else {
            lowStockCountLabel.getStyleClass().remove("warning-text");
            lowStockPercentageLabel.getStyleClass().remove("warning-text");
        }
    }
    
    /**
     * Update charts with new data
     * @param stats Dashboard statistics
     */
    private void updateCharts(DashboardStats stats) {
        updateCategoryPieChart(stats);
        updateStockLevelChart(stats);
    }
    
    /**
     * Update category pie chart
     * @param stats Dashboard statistics
     */
    private void updateCategoryPieChart(DashboardStats stats) {
        categoryPieChart.getData().clear();
        
        if (stats.getTopCategories() != null && !stats.getTopCategories().isEmpty()) {
            for (DashboardStats.CategoryStats category : stats.getTopCategories()) {
                PieChart.Data slice = new PieChart.Data(
                    category.getName() + " (" + category.getCount() + ")", 
                    category.getCount()
                );
                categoryPieChart.getData().add(slice);
            }
        } else {
            // Show placeholder when no data
            PieChart.Data placeholder = new PieChart.Data("No Data", 1);
            categoryPieChart.getData().add(placeholder);
        }
    }
    
    /**
     * Update stock level bar chart
     * @param stats Dashboard statistics
     */
    private void updateStockLevelChart(DashboardStats stats) {
        stockLevelChart.getData().clear();
        
        if (stats.getTopCategories() != null && !stats.getTopCategories().isEmpty()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Stock Levels");
            
            for (DashboardStats.CategoryStats category : stats.getTopCategories()) {
                series.getData().add(new XYChart.Data<>(category.getName(), category.getCount()));
            }
            
            stockLevelChart.getData().add(series);
        }
    }
    
    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh() {
        logger.debug("Dashboard refresh requested");
        if (mainController != null) {
            mainController.refreshDashboard();
        }
    }
    
    /**
     * Handle view products button click
     */
    @FXML
    private void handleViewProducts() {
        logger.debug("View products requested from dashboard");
        if (mainController != null) {
            mainController.showProducts();
        }
    }
    
    /**
     * Handle add product button click
     */
    @FXML
    private void handleAddProduct() {
        logger.debug("Add product requested from dashboard");
        if (mainController != null) {
            mainController.showAddProduct();
        }
    }
    
    /**
     * Handle low stock alert click
     */
    @FXML
    private void handleLowStockAlert() {
        logger.debug("Low stock alert clicked");
        if (mainController != null && currentStats != null && currentStats.hasLowStockAlerts()) {
            // Navigate to products view with low stock filter
            mainController.showProducts();
            // TODO: Apply low stock filter in products view
        }
    }
    
    /**
     * Get the root view node
     * @return Root view node
     */
    public Parent getView() {
        return dashboardRoot;
    }
    
    /**
     * Set the main controller reference
     * @param mainController Main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    /**
     * Get current dashboard statistics
     * @return Current statistics
     */
    public DashboardStats getCurrentStats() {
        return currentStats;
    }
    
    /**
     * Check if dashboard has data
     * @return true if dashboard has statistics
     */
    public boolean hasData() {
        return currentStats != null;
    }
    
    /**
     * Clear dashboard data
     */
    public void clearData() {
        currentStats = null;
        Platform.runLater(() -> {
            setupInitialData();
            categoryPieChart.getData().clear();
            stockLevelChart.getData().clear();
        });
    }
}
