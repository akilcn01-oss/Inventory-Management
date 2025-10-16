package com.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Application configuration manager
 * Handles loading and saving of application settings
 */
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "application.properties";
    private static final String DEFAULT_CONFIG_FILE = "/config/default.properties";
    
    private static AppConfig instance;
    private Properties properties;
    
    // Default configuration values
    private static final String DEFAULT_API_BASE_URL = "http://localhost:8000";
    private static final String DEFAULT_API_TIMEOUT = "30000";
    private static final String DEFAULT_ITEMS_PER_PAGE = "20";
    private static final String DEFAULT_LOW_STOCK_THRESHOLD = "10";
    private static final String DEFAULT_THEME = "light";
    
    private AppConfig() {
        properties = new Properties();
    }
    
    /**
     * Get singleton instance of AppConfig
     * @return AppConfig instance
     */
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    /**
     * Load configuration from file
     */
    public void loadConfiguration() {
        try {
            // First load default configuration
            loadDefaultConfiguration();
            
            // Then load user configuration if it exists
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    logger.info("User configuration loaded from: {}", configFile.getAbsolutePath());
                }
            } else {
                logger.info("No user configuration file found, using defaults");
                saveConfiguration(); // Create default config file
            }
            
            // Validate configuration
            validateConfiguration();
            
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
            throw new RuntimeException("Failed to load application configuration", e);
        }
    }
    
    /**
     * Load default configuration from resources
     */
    private void loadDefaultConfiguration() {
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
                logger.debug("Default configuration loaded");
            } else {
                // Set hardcoded defaults if resource file is not found
                setDefaultValues();
                logger.warn("Default configuration file not found, using hardcoded defaults");
            }
        } catch (IOException e) {
            logger.warn("Error loading default configuration, using hardcoded defaults", e);
            setDefaultValues();
        }
    }
    
    /**
     * Set hardcoded default values
     */
    private void setDefaultValues() {
        properties.setProperty("api.base.url", DEFAULT_API_BASE_URL);
        properties.setProperty("api.timeout", DEFAULT_API_TIMEOUT);
        properties.setProperty("ui.items.per.page", DEFAULT_ITEMS_PER_PAGE);
        properties.setProperty("inventory.low.stock.threshold", DEFAULT_LOW_STOCK_THRESHOLD);
        properties.setProperty("ui.theme", DEFAULT_THEME);
        properties.setProperty("app.version", "1.0.0");
        properties.setProperty("app.name", "Inventory Management System");
    }
    
    /**
     * Validate configuration values
     */
    private void validateConfiguration() {
        // Validate API base URL
        String apiUrl = getApiBaseUrl();
        if (!apiUrl.startsWith("http://") && !apiUrl.startsWith("https://")) {
            logger.warn("Invalid API base URL format: {}", apiUrl);
        }
        
        // Validate numeric values
        try {
            Integer.parseInt(getProperty("api.timeout", DEFAULT_API_TIMEOUT));
            Integer.parseInt(getProperty("ui.items.per.page", DEFAULT_ITEMS_PER_PAGE));
            Integer.parseInt(getProperty("inventory.low.stock.threshold", DEFAULT_LOW_STOCK_THRESHOLD));
        } catch (NumberFormatException e) {
            logger.error("Invalid numeric configuration value", e);
            throw new RuntimeException("Invalid configuration values", e);
        }
        
        logger.info("Configuration validation completed successfully");
    }
    
    /**
     * Save configuration to file
     */
    public void saveConfiguration() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Inventory Management System Configuration");
            logger.info("Configuration saved to: {}", new File(CONFIG_FILE).getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error saving configuration", e);
        }
    }
    
    /**
     * Get property value
     * @param key Property key
     * @param defaultValue Default value if key not found
     * @return Property value
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get property value
     * @param key Property key
     * @return Property value or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Set property value
     * @param key Property key
     * @param value Property value
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        logger.debug("Configuration property set: {} = {}", key, value);
    }
    
    // Convenience methods for common configuration values
    
    public String getApiBaseUrl() {
        return getProperty("api.base.url", DEFAULT_API_BASE_URL);
    }
    
    public void setApiBaseUrl(String url) {
        setProperty("api.base.url", url);
    }
    
    public int getApiTimeout() {
        return Integer.parseInt(getProperty("api.timeout", DEFAULT_API_TIMEOUT));
    }
    
    public void setApiTimeout(int timeout) {
        setProperty("api.timeout", String.valueOf(timeout));
    }
    
    public int getItemsPerPage() {
        return Integer.parseInt(getProperty("ui.items.per.page", DEFAULT_ITEMS_PER_PAGE));
    }
    
    public void setItemsPerPage(int itemsPerPage) {
        setProperty("ui.items.per.page", String.valueOf(itemsPerPage));
    }
    
    public int getLowStockThreshold() {
        return Integer.parseInt(getProperty("inventory.low.stock.threshold", DEFAULT_LOW_STOCK_THRESHOLD));
    }
    
    public void setLowStockThreshold(int threshold) {
        setProperty("inventory.low.stock.threshold", String.valueOf(threshold));
    }
    
    public String getTheme() {
        return getProperty("ui.theme", DEFAULT_THEME);
    }
    
    public void setTheme(String theme) {
        setProperty("ui.theme", theme);
    }
    
    public String getAppVersion() {
        return getProperty("app.version", "1.0.0");
    }
    
    public String getAppName() {
        return getProperty("app.name", "Inventory Management System");
    }
    
    /**
     * Get all properties as a copy
     * @return Properties copy
     */
    public Properties getAllProperties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }
    
    /**
     * Reset configuration to defaults
     */
    public void resetToDefaults() {
        logger.info("Resetting configuration to defaults");
        properties.clear();
        setDefaultValues();
        saveConfiguration();
    }
    
    /**
     * Check if configuration file exists
     * @return true if config file exists
     */
    public boolean configFileExists() {
        return new File(CONFIG_FILE).exists();
    }
}
