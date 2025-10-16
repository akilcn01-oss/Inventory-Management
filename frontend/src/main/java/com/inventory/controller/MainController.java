package com.inventory.controller;

import com.inventory.config.AppConfig;
import com.inventory.model.DashboardStats;
import com.inventory.model.Product;
import com.inventory.service.ApiService;
import com.inventory.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Main controller for the inventory management application
 * Handles navigation between different views and manages the main UI
 */
public class MainController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox sidebarVBox;
    @FXML private Button dashboardButton;
    @FXML private Button productsButton;
    @FXML private Button addProductButton;
    @FXML private Button documentsButton;
    @FXML private Button settingsButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    
    private ApiService apiService;
    private AppConfig appConfig;
    private ObservableList<Product> products;
    private DashboardStats currentStats;
    
    // Controllers for different views
    private DashboardController dashboardController;
    private ProductListController productListController;
    private ProductFormController productFormController;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController");
        
        try {
            // Initialize services
            apiService = ApiService.getInstance();
            appConfig = AppConfig.getInstance();
            products = FXCollections.observableArrayList();
            
            // Setup UI
            setupSidebar();
            setupStatusBar();
            
            // Load dashboard by default
            showDashboard();
            
            // Test API connection
            testApiConnection();
            
            logger.info("MainController initialized successfully");
            
        } catch (Exception e) {
            logger.error("Error initializing MainController", e);
            AlertUtil.showError("Initialization Error", e);
        }
    }
    
    /**
     * Setup sidebar navigation
     */
    private void setupSidebar() {
        // Style active button
        dashboardButton.getStyleClass().add("sidebar-button-active");
        
        // Setup button actions
        dashboardButton.setOnAction(e -> showDashboard());
        productsButton.setOnAction(e -> showProducts());
        addProductButton.setOnAction(e -> showAddProduct());
        documentsButton.setOnAction(e -> showDocuments());
        settingsButton.setOnAction(e -> showSettings());
    }
    
    /**
     * Setup status bar
     */
    private void setupStatusBar() {
        loadingIndicator.setVisible(false);
        updateStatus("Ready");
    }
    
    /**
     * Test API connection and update status
     */
    private void testApiConnection() {
        Task<Boolean> connectionTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return apiService.testConnection();
            }
        };
        
        connectionTask.setOnSucceeded(e -> {
            boolean connected = connectionTask.getValue();
            if (connected) {
                updateStatus("Connected to server");
                logger.info("API connection successful");
            } else {
                updateStatus("Server connection failed");
                logger.warn("API connection failed");
                AlertUtil.showConnectionError();
            }
        });
        
        connectionTask.setOnFailed(e -> {
            updateStatus("Connection error");
            logger.error("API connection test failed", connectionTask.getException());
            AlertUtil.showConnectionError();
        });
        
        Thread connectionThread = new Thread(connectionTask);
        connectionThread.setDaemon(true);
        connectionThread.start();
    }
    
    /**
     * Show dashboard view
     */
    @FXML
    public void showDashboard() {
        try {
            logger.debug("Showing dashboard");
            updateActiveButton(dashboardButton);
            
            if (dashboardController == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                Parent dashboardView = loader.load();
                dashboardController = loader.getController();
                dashboardController.setMainController(this);
            }
            
            mainBorderPane.setCenter(dashboardController.getView());
            refreshDashboard();
            
        } catch (IOException e) {
            logger.error("Error loading dashboard view", e);
            AlertUtil.showLoadingError("load dashboard");
        }
    }
    
    /**
     * Show products list view
     */
    @FXML
    public void showProducts() {
        try {
            logger.debug("Showing products list");
            updateActiveButton(productsButton);
            
            if (productListController == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product-list.fxml"));
                Parent productListView = loader.load();
                productListController = loader.getController();
                productListController.setMainController(this);
            }
            
            mainBorderPane.setCenter(productListController.getView());
            refreshProducts();
            
        } catch (IOException e) {
            logger.error("Error loading products view", e);
            AlertUtil.showLoadingError("load products");
        }
    }
    
    /**
     * Show add product form
     */
    @FXML
    public void showAddProduct() {
        try {
            logger.debug("Showing add product form");
            showProductForm(null);
            
        } catch (Exception e) {
            logger.error("Error showing add product form", e);
            AlertUtil.showError("Form Error", e);
        }
    }
    
    /**
     * Show documents view
     */
    @FXML
    public void showDocuments() {
        try {
            logger.debug("Showing documents view");
            updateActiveButton(documentsButton);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/documents.fxml"));
            Parent documentsView = loader.load();
            DocumentsController documentsController = loader.getController();
            documentsController.setMainController(this);
            
            mainBorderPane.setCenter(documentsView);
            
        } catch (IOException e) {
            logger.error("Error loading documents view", e);
            AlertUtil.showLoadingError("load documents");
        }
    }
    
    /**
     * Show product form for adding or editing
     * @param product Product to edit (null for new product)
     */
    public void showProductForm(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product-form.fxml"));
            Parent formView = loader.load();
            ProductFormController controller = loader.getController();
            
            controller.setMainController(this);
            if (product != null) {
                controller.setProduct(product);
            }
            
            // Show in modal dialog
            Stage formStage = new Stage();
            formStage.setTitle(product == null ? "Add Product" : "Edit Product");
            formStage.initModality(Modality.APPLICATION_MODAL);
            formStage.setScene(new Scene(formView));
            formStage.setResizable(false);
            
            // Center on parent window
            Stage parentStage = (Stage) mainBorderPane.getScene().getWindow();
            formStage.initOwner(parentStage);
            
            formStage.showAndWait();
            
        } catch (IOException e) {
            logger.error("Error loading product form", e);
            AlertUtil.showLoadingError("load product form");
        }
    }
    
    /**
     * Show settings dialog
     */
    @FXML
    public void showSettings() {
        try {
            logger.debug("Showing settings");
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent settingsView = loader.load();
            SettingsController controller = loader.getController();
            controller.setMainController(this);
            
            // Show in modal dialog
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setScene(new Scene(settingsView));
            settingsStage.setResizable(false);
            
            // Center on parent window
            Stage parentStage = (Stage) mainBorderPane.getScene().getWindow();
            settingsStage.initOwner(parentStage);
            
            settingsStage.showAndWait();
            
        } catch (IOException e) {
            logger.error("Error loading settings view", e);
            AlertUtil.showLoadingError("load settings");
        }
    }
    
    /**
     * Refresh dashboard data
     */
    public void refreshDashboard() {
        if (dashboardController == null) return;
        
        showLoading(true);
        updateStatus("Loading dashboard data...");
        
        Task<DashboardStats> dashboardTask = new Task<DashboardStats>() {
            @Override
            protected DashboardStats call() throws Exception {
                return apiService.getDashboardStats();
            }
        };
        
        dashboardTask.setOnSucceeded(e -> {
            currentStats = dashboardTask.getValue();
            dashboardController.updateStats(currentStats);
            updateStatus("Dashboard updated");
            showLoading(false);
            logger.debug("Dashboard data refreshed");
        });
        
        dashboardTask.setOnFailed(e -> {
            logger.error("Error refreshing dashboard", dashboardTask.getException());
            AlertUtil.showLoadingError("refresh dashboard");
            updateStatus("Dashboard refresh failed");
            showLoading(false);
        });
        
        Thread dashboardThread = new Thread(dashboardTask);
        dashboardThread.setDaemon(true);
        dashboardThread.start();
    }
    
    /**
     * Refresh products data
     */
    public void refreshProducts() {
        if (productListController == null) return;
        
        showLoading(true);
        updateStatus("Loading products...");
        
        Task<List<Product>> productsTask = new Task<List<Product>>() {
            @Override
            protected List<Product> call() throws Exception {
                return apiService.getAllProducts();
            }
        };
        
        productsTask.setOnSucceeded(e -> {
            List<Product> productList = productsTask.getValue();
            products.setAll(productList);
            productListController.updateProducts(products);
            updateStatus(String.format("Loaded %d products", productList.size()));
            showLoading(false);
            logger.debug("Products data refreshed: {} items", productList.size());
        });
        
        productsTask.setOnFailed(e -> {
            logger.error("Error refreshing products", productsTask.getException());
            AlertUtil.showLoadingError("refresh products");
            updateStatus("Products refresh failed");
            showLoading(false);
        });
        
        Thread productsThread = new Thread(productsTask);
        productsThread.setDaemon(true);
        productsThread.start();
    }
    
    /**
     * Handle product saved event
     * @param product Saved product
     * @param isNew Whether this is a new product
     */
    public void onProductSaved(Product product, boolean isNew) {
        logger.info("Product saved: {} (new: {})", product.getName(), isNew);
        
        if (isNew) {
            products.add(product);
            AlertUtil.showSuccess("Product '" + product.getName() + "' added successfully");
        } else {
            // Update existing product in list
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).getId() == product.getId()) {
                    products.set(i, product);
                    break;
                }
            }
            AlertUtil.showSuccess("Product '" + product.getName() + "' updated successfully");
        }
        
        // Refresh current view
        if (productListController != null) {
            productListController.updateProducts(products);
        }
        
        // Refresh dashboard if it's loaded
        if (dashboardController != null) {
            refreshDashboard();
        }
    }
    
    /**
     * Handle product deleted event
     * @param product Deleted product
     */
    public void onProductDeleted(Product product) {
        logger.info("Product deleted: {}", product.getName());
        
        products.removeIf(p -> p.getId() == product.getId());
        AlertUtil.showSuccess("Product '" + product.getName() + "' deleted successfully");
        
        // Refresh current view
        if (productListController != null) {
            productListController.updateProducts(products);
        }
        
        // Refresh dashboard if it's loaded
        if (dashboardController != null) {
            refreshDashboard();
        }
    }
    
    /**
     * Update active button styling
     * @param activeButton Button to mark as active
     */
    private void updateActiveButton(Button activeButton) {
        // Remove active class from all buttons
        dashboardButton.getStyleClass().remove("sidebar-button-active");
        productsButton.getStyleClass().remove("sidebar-button-active");
        addProductButton.getStyleClass().remove("sidebar-button-active");
        settingsButton.getStyleClass().remove("sidebar-button-active");
        
        // Add active class to selected button
        activeButton.getStyleClass().add("sidebar-button-active");
    }
    
    /**
     * Update status bar message
     * @param message Status message
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            logger.debug("Status updated: {}", message);
        });
    }
    
    /**
     * Show/hide loading indicator
     * @param show Whether to show loading indicator
     */
    private void showLoading(boolean show) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(show);
        });
    }
    
    // Getters for other controllers
    
    public ApiService getApiService() {
        return apiService;
    }
    
    public AppConfig getAppConfig() {
        return appConfig;
    }
    
    public ObservableList<Product> getProducts() {
        return products;
    }
    
    public DashboardStats getCurrentStats() {
        return currentStats;
    }
}
