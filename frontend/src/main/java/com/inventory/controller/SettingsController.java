package com.inventory.controller;

import com.inventory.config.AppConfig;
import com.inventory.service.ApiService;
import com.inventory.util.AlertUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the settings dialog
 * Handles application configuration and preferences
 */
public class SettingsController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    
    @FXML private TabPane settingsTabPane;
    
    // API Settings
    @FXML private TextField apiBaseUrlField;
    @FXML private Spinner<Integer> apiTimeoutSpinner;
    @FXML private Button testConnectionButton;
    @FXML private Label connectionStatusLabel;
    
    // UI Settings
    @FXML private ComboBox<String> themeComboBox;
    @FXML private Spinner<Integer> itemsPerPageSpinner;
    @FXML private CheckBox enableAnimationsCheckBox;
    @FXML private CheckBox showTooltipsCheckBox;
    
    // Inventory Settings
    @FXML private Spinner<Integer> lowStockThresholdSpinner;
    @FXML private CheckBox enableLowStockAlertsCheckBox;
    @FXML private CheckBox autoRefreshCheckBox;
    @FXML private Spinner<Integer> refreshIntervalSpinner;
    
    // Buttons
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button resetButton;
    @FXML private ProgressIndicator loadingIndicator;
    
    private MainController mainController;
    private AppConfig appConfig;
    private ApiService apiService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initializing SettingsController");
        
        setupControls();
        setupButtons();
        
        loadingIndicator.setVisible(false);
        
        logger.debug("SettingsController initialized");
    }
    
    /**
     * Setup form controls
     */
    private void setupControls() {
        // API timeout spinner (5-300 seconds)
        apiTimeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5000, 300000, 30000, 5000));
        apiTimeoutSpinner.setEditable(true);
        
        // Items per page spinner (10-100)
        itemsPerPageSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 100, 20, 10));
        itemsPerPageSpinner.setEditable(true);
        
        // Low stock threshold spinner (1-100)
        lowStockThresholdSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10, 1));
        lowStockThresholdSpinner.setEditable(true);
        
        // Refresh interval spinner (30-3600 seconds)
        refreshIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 3600, 300, 30));
        refreshIntervalSpinner.setEditable(true);
        
        // Theme combo box
        themeComboBox.getItems().addAll("Light", "Dark", "System");
        
        // Enable/disable refresh interval based on auto refresh
        autoRefreshCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            refreshIntervalSpinner.setDisable(!newValue);
        });
        
        // Validate API URL format
        apiBaseUrlField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateApiUrl();
        });
    }
    
    /**
     * Setup button actions
     */
    private void setupButtons() {
        testConnectionButton.setOnAction(e -> testApiConnection());
        saveButton.setOnAction(e -> saveSettings());
        cancelButton.setOnAction(e -> closeDialog());
        resetButton.setOnAction(e -> resetToDefaults());
    }
    
    /**
     * Load current settings into form
     */
    private void loadSettings() {
        if (appConfig == null) return;
        
        // API Settings
        apiBaseUrlField.setText(appConfig.getApiBaseUrl());
        apiTimeoutSpinner.getValueFactory().setValue(appConfig.getApiTimeout());
        
        // UI Settings
        themeComboBox.setValue(capitalizeFirst(appConfig.getTheme()));
        itemsPerPageSpinner.getValueFactory().setValue(appConfig.getItemsPerPage());
        enableAnimationsCheckBox.setSelected(Boolean.parseBoolean(appConfig.getProperty("ui.animations.enabled", "true")));
        showTooltipsCheckBox.setSelected(Boolean.parseBoolean(appConfig.getProperty("ui.tooltips.enabled", "true")));
        
        // Inventory Settings
        lowStockThresholdSpinner.getValueFactory().setValue(appConfig.getLowStockThreshold());
        enableLowStockAlertsCheckBox.setSelected(Boolean.parseBoolean(appConfig.getProperty("inventory.alerts.enabled", "true")));
        autoRefreshCheckBox.setSelected(Boolean.parseBoolean(appConfig.getProperty("ui.auto.refresh", "false")));
        refreshIntervalSpinner.getValueFactory().setValue(Integer.parseInt(appConfig.getProperty("ui.refresh.interval", "300")));
        
        // Update UI state
        refreshIntervalSpinner.setDisable(!autoRefreshCheckBox.isSelected());
        validateApiUrl();
        
        logger.debug("Settings loaded into form");
    }
    
    /**
     * Validate API URL format
     */
    private void validateApiUrl() {
        String url = apiBaseUrlField.getText().trim();
        boolean isValid = url.startsWith("http://") || url.startsWith("https://");
        
        if (isValid) {
            apiBaseUrlField.getStyleClass().remove("error-field");
        } else {
            if (!apiBaseUrlField.getStyleClass().contains("error-field")) {
                apiBaseUrlField.getStyleClass().add("error-field");
            }
        }
        
        testConnectionButton.setDisable(!isValid);
    }
    
    /**
     * Test API connection
     */
    @FXML
    private void testApiConnection() {
        String apiUrl = apiBaseUrlField.getText().trim();
        if (apiUrl.isEmpty()) {
            AlertUtil.showValidationError("Please enter an API URL");
            return;
        }
        
        connectionStatusLabel.setText("Testing connection...");
        testConnectionButton.setDisable(true);
        loadingIndicator.setVisible(true);
        
        Task<Boolean> connectionTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Temporarily update API service with new URL
                String originalUrl = appConfig.getApiBaseUrl();
                appConfig.setApiBaseUrl(apiUrl);
                
                try {
                    return apiService.testConnection();
                } finally {
                    // Restore original URL
                    appConfig.setApiBaseUrl(originalUrl);
                }
            }
        };
        
        connectionTask.setOnSucceeded(e -> {
            boolean connected = connectionTask.getValue();
            if (connected) {
                connectionStatusLabel.setText("✓ Connection successful");
                connectionStatusLabel.getStyleClass().remove("error-text");
                connectionStatusLabel.getStyleClass().add("success-text");
            } else {
                connectionStatusLabel.setText("✗ Connection failed");
                connectionStatusLabel.getStyleClass().remove("success-text");
                connectionStatusLabel.getStyleClass().add("error-text");
            }
            
            testConnectionButton.setDisable(false);
            loadingIndicator.setVisible(false);
        });
        
        connectionTask.setOnFailed(e -> {
            connectionStatusLabel.setText("✗ Connection error");
            connectionStatusLabel.getStyleClass().remove("success-text");
            connectionStatusLabel.getStyleClass().add("error-text");
            
            testConnectionButton.setDisable(false);
            loadingIndicator.setVisible(false);
            
            logger.error("Connection test failed", connectionTask.getException());
        });
        
        Thread connectionThread = new Thread(connectionTask);
        connectionThread.setDaemon(true);
        connectionThread.start();
    }
    
    /**
     * Save settings
     */
    @FXML
    private void saveSettings() {
        try {
            if (!validateSettings()) {
                return;
            }
            
            // API Settings
            appConfig.setApiBaseUrl(apiBaseUrlField.getText().trim());
            appConfig.setApiTimeout(apiTimeoutSpinner.getValue());
            
            // UI Settings
            appConfig.setTheme(themeComboBox.getValue().toLowerCase());
            appConfig.setItemsPerPage(itemsPerPageSpinner.getValue());
            appConfig.setProperty("ui.animations.enabled", String.valueOf(enableAnimationsCheckBox.isSelected()));
            appConfig.setProperty("ui.tooltips.enabled", String.valueOf(showTooltipsCheckBox.isSelected()));
            
            // Inventory Settings
            appConfig.setLowStockThreshold(lowStockThresholdSpinner.getValue());
            appConfig.setProperty("inventory.alerts.enabled", String.valueOf(enableLowStockAlertsCheckBox.isSelected()));
            appConfig.setProperty("ui.auto.refresh", String.valueOf(autoRefreshCheckBox.isSelected()));
            appConfig.setProperty("ui.refresh.interval", String.valueOf(refreshIntervalSpinner.getValue()));
            
            // Save to file
            appConfig.saveConfiguration();
            
            AlertUtil.showSuccess("Settings saved successfully. Some changes may require a restart to take effect.");
            
            logger.info("Settings saved successfully");
            closeDialog();
            
        } catch (Exception e) {
            logger.error("Error saving settings", e);
            AlertUtil.showSaveError("save settings");
        }
    }
    
    /**
     * Validate settings before saving
     * @return true if settings are valid
     */
    private boolean validateSettings() {
        // Validate API URL
        String apiUrl = apiBaseUrlField.getText().trim();
        if (apiUrl.isEmpty() || (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://"))) {
            AlertUtil.showValidationError("Please enter a valid API URL (must start with http:// or https://)");
            settingsTabPane.getSelectionModel().select(0); // Switch to API tab
            apiBaseUrlField.requestFocus();
            return false;
        }
        
        // Validate numeric values
        if (apiTimeoutSpinner.getValue() < 5000) {
            AlertUtil.showValidationError("API timeout must be at least 5 seconds");
            settingsTabPane.getSelectionModel().select(0);
            apiTimeoutSpinner.requestFocus();
            return false;
        }
        
        if (itemsPerPageSpinner.getValue() < 10) {
            AlertUtil.showValidationError("Items per page must be at least 10");
            settingsTabPane.getSelectionModel().select(1);
            itemsPerPageSpinner.requestFocus();
            return false;
        }
        
        if (lowStockThresholdSpinner.getValue() < 1) {
            AlertUtil.showValidationError("Low stock threshold must be at least 1");
            settingsTabPane.getSelectionModel().select(2);
            lowStockThresholdSpinner.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Reset settings to defaults
     */
    @FXML
    private void resetToDefaults() {
        boolean confirmed = AlertUtil.showConfirmation(
            "Reset Settings",
            "Reset all settings to defaults?",
            "This will restore all settings to their default values. This action cannot be undone."
        );
        
        if (confirmed) {
            appConfig.resetToDefaults();
            loadSettings();
            AlertUtil.showSuccess("Settings reset to defaults");
            logger.info("Settings reset to defaults");
        }
    }
    
    /**
     * Close the settings dialog
     */
    @FXML
    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Set the main controller reference
     * @param mainController Main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.appConfig = mainController.getAppConfig();
        this.apiService = mainController.getApiService();
        
        // Load current settings
        loadSettings();
    }
    
    /**
     * Capitalize first letter of string
     * @param str Input string
     * @return Capitalized string
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    /**
     * Handle about button click
     */
    @FXML
    private void showAbout() {
        AlertUtil.showInfo(
            "About",
            appConfig.getAppName() + " v" + appConfig.getAppVersion(),
            "A comprehensive inventory management system built with JavaFX and Python FastAPI.\n\n" +
            "Features:\n" +
            "• Product management with CRUD operations\n" +
            "• Dashboard with statistics and charts\n" +
            "• Search and filtering capabilities\n" +
            "• Low stock alerts\n" +
            "• Modern, responsive UI\n\n" +
            "© 2024 Inventory Management System"
        );
    }
}
