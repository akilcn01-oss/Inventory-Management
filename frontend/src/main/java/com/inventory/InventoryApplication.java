package com.inventory;

import com.inventory.config.AppConfig;
import com.inventory.service.ApiService;
import com.inventory.util.AlertUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main JavaFX Application class for Inventory Management System
 * Handles application startup, configuration, and primary stage setup
 */
public class InventoryApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryApplication.class);
    private static final String APP_TITLE = "Inventory Management System";
    private static final String MAIN_FXML = "/fxml/main.fxml";
    private static final String APP_ICON = "/images/inventory-icon.png";
    
    private Stage primaryStage;
    private ApiService apiService;
    
    @Override
    public void init() throws Exception {
        super.init();
        logger.info("Initializing Inventory Management Application...");
        
        try {
            // Initialize configuration
            AppConfig.getInstance().loadConfiguration();
            logger.info("Configuration loaded successfully");
            
            // Initialize API service
            apiService = ApiService.getInstance();
            logger.info("API service initialized");
            
            // Test API connection
            if (!apiService.testConnection()) {
                logger.warn("API connection test failed - application will continue with limited functionality");
            } else {
                logger.info("API connection test successful");
            }
            
        } catch (Exception e) {
            logger.error("Error during application initialization", e);
            throw e;
        }
    }
    
    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        
        try {
            logger.info("Starting JavaFX application...");
            
            // Load main FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            Scene scene = new Scene(fxmlLoader.load());
            
            // Apply CSS styling
            try {
                String cssPath = getClass().getResource("/css/styles-simple.css").toExternalForm();
                scene.getStylesheets().add(cssPath);
                logger.info("CSS stylesheet loaded successfully");
            } catch (Exception e) {
                logger.warn("Could not load CSS stylesheet: " + e.getMessage());
            }
            
            // Configure primary stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.setMaximized(true);
            
            // Set application icon (optional)
            try {
                Image icon = new Image(getClass().getResourceAsStream(APP_ICON));
                if (icon != null) {
                    primaryStage.getIcons().add(icon);
                }
            } catch (Exception e) {
                logger.debug("Application icon not available: " + e.getMessage());
            }
            
            // Handle close request
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Application close requested");
                // Allow close without confirmation for now
                // handleApplicationExit();
            });
            
            // Show the stage
            primaryStage.show();
            logger.info("Application started successfully");
            
        } catch (IOException e) {
            logger.error("Error loading main FXML", e);
            AlertUtil.showError("Startup Error", 
                "Failed to load the main application window.", 
                "Please check the application files and try again.");
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during application startup", e);
            AlertUtil.showError("Startup Error", 
                "An unexpected error occurred during startup.", 
                e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("Stopping Inventory Management Application...");
        
        try {
            // Cleanup resources
            if (apiService != null) {
                apiService.shutdown();
            }
            
            // Save any pending configuration changes
            AppConfig.getInstance().saveConfiguration();
            
            logger.info("Application stopped successfully");
            
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        } finally {
            super.stop();
        }
    }
    
    /**
     * Handle application exit with confirmation
     */
    private void handleApplicationExit() {
        try {
            boolean confirmed = AlertUtil.showConfirmation(
                "Exit Application",
                "Are you sure you want to exit?",
                "Any unsaved changes will be lost."
            );
            
            if (confirmed) {
                logger.info("User confirmed application exit");
                primaryStage.close();
            } else {
                logger.info("User cancelled application exit");
            }
            
        } catch (Exception e) {
            logger.error("Error handling application exit", e);
            primaryStage.close();
        }
    }
    
    /**
     * Get the primary stage instance
     * @return Primary stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Get the API service instance
     * @return API service
     */
    public ApiService getApiService() {
        return apiService;
    }
    
    /**
     * Main method - entry point for the application
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting Inventory Management Application with args: {}", 
                args.length > 0 ? String.join(" ", args) : "none");
            
            // Set system properties for better JavaFX performance
            System.setProperty("javafx.preloader", "com.inventory.preloader.InventoryPreloader");
            System.setProperty("prism.lcdtext", "false");
            System.setProperty("prism.text", "t2k");
            
            // Launch JavaFX application
            launch(args);
            
        } catch (Exception e) {
            logger.error("Fatal error during application startup", e);
            System.err.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }
}
