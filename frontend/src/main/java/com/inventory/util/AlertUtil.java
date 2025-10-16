package com.inventory.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Utility class for showing various types of alerts and dialogs
 * Provides consistent styling and behavior across the application
 */
public class AlertUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertUtil.class);
    
    /**
     * Show information alert
     * @param title Alert title
     * @param header Alert header
     * @param content Alert content
     */
    public static void showInfo(String title, String header, String content) {
        logger.debug("Showing info alert: {}", title);
        Alert alert = createAlert(Alert.AlertType.INFORMATION, title, header, content);
        alert.showAndWait();
    }
    
    /**
     * Show warning alert
     * @param title Alert title
     * @param header Alert header
     * @param content Alert content
     */
    public static void showWarning(String title, String header, String content) {
        logger.debug("Showing warning alert: {}", title);
        Alert alert = createAlert(Alert.AlertType.WARNING, title, header, content);
        alert.showAndWait();
    }
    
    /**
     * Show error alert
     * @param title Alert title
     * @param header Alert header
     * @param content Alert content
     */
    public static void showError(String title, String header, String content) {
        logger.debug("Showing error alert: {}", title);
        Alert alert = createAlert(Alert.AlertType.ERROR, title, header, content);
        alert.showAndWait();
    }
    
    /**
     * Show confirmation dialog
     * @param title Dialog title
     * @param header Dialog header
     * @param content Dialog content
     * @return true if user clicked OK, false otherwise
     */
    public static boolean showConfirmation(String title, String header, String content) {
        logger.debug("Showing confirmation dialog: {}", title);
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, header, content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Show text input dialog
     * @param title Dialog title
     * @param header Dialog header
     * @param content Dialog content
     * @param defaultValue Default input value
     * @return User input or null if cancelled
     */
    public static String showTextInput(String title, String header, String content, String defaultValue) {
        logger.debug("Showing text input dialog: {}", title);
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        
        // Apply styling
        applyDialogStyling(dialog.getDialogPane().getScene().getWindow());
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    /**
     * Show success notification
     * @param message Success message
     */
    public static void showSuccess(String message) {
        showInfo("Success", "Operation Completed", message);
    }
    
    /**
     * Show error notification with exception details
     * @param title Error title
     * @param exception Exception that occurred
     */
    public static void showError(String title, Exception exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
        showError(title, "An error occurred", message);
        logger.error("Error shown to user: {}", title, exception);
    }
    
    /**
     * Show delete confirmation dialog
     * @param itemName Name of item to delete
     * @return true if user confirmed deletion
     */
    public static boolean showDeleteConfirmation(String itemName) {
        return showConfirmation(
            "Confirm Deletion",
            "Delete " + itemName + "?",
            "This action cannot be undone. Are you sure you want to delete this item?"
        );
    }
    
    /**
     * Show validation error alert
     * @param errors Validation error messages
     */
    public static void showValidationError(String errors) {
        showError("Validation Error", "Please correct the following errors:", errors);
    }
    
    /**
     * Show connection error alert
     */
    public static void showConnectionError() {
        showError(
            "Connection Error",
            "Unable to connect to server",
            "Please check your network connection and ensure the server is running."
        );
    }
    
    /**
     * Show loading error alert
     * @param operation Operation that failed
     */
    public static void showLoadingError(String operation) {
        showError(
            "Loading Error",
            "Failed to " + operation,
            "An error occurred while loading data. Please try again."
        );
    }
    
    /**
     * Show save error alert
     * @param operation Operation that failed
     */
    public static void showSaveError(String operation) {
        showError(
            "Save Error",
            "Failed to " + operation,
            "An error occurred while saving data. Please try again."
        );
    }
    
    /**
     * Create a styled alert
     * @param alertType Type of alert
     * @param title Alert title
     * @param header Alert header
     * @param content Alert content
     * @return Configured alert
     */
    private static Alert createAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // Apply styling
        applyDialogStyling(alert.getDialogPane().getScene().getWindow());
        
        return alert;
    }
    
    /**
     * Apply consistent styling to dialogs
     * @param window Dialog window
     */
    private static void applyDialogStyling(javafx.stage.Window window) {
        if (window instanceof Stage) {
            Stage stage = (Stage) window;
            
            // Set icon if available
            try {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(
                    AlertUtil.class.getResourceAsStream("/images/inventory-icon.png")
                );
                if (icon != null && !icon.isError()) {
                    stage.getIcons().add(icon);
                }
            } catch (Exception e) {
                // Icon not found, continue without it
                logger.debug("Dialog icon not available");
            }
            
            // Apply CSS if available
            try {
                String cssUrl = AlertUtil.class.getResource("/css/dialogs.css").toExternalForm();
                if (cssUrl != null) {
                    stage.getScene().getStylesheets().add(cssUrl);
                }
            } catch (Exception e) {
                // CSS not found, continue without it
                logger.debug("Dialog CSS not available");
            }
        }
    }
    
    /**
     * Show custom alert with custom buttons
     * @param alertType Type of alert
     * @param title Alert title
     * @param header Alert header
     * @param content Alert content
     * @param buttons Custom buttons
     * @return Selected button type
     */
    public static Optional<ButtonType> showCustomAlert(Alert.AlertType alertType, String title, 
                                                      String header, String content, ButtonType... buttons) {
        logger.debug("Showing custom alert: {}", title);
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        // Clear default buttons and add custom ones
        alert.getButtonTypes().clear();
        alert.getButtonTypes().addAll(buttons);
        
        // Apply styling
        applyDialogStyling(alert.getDialogPane().getScene().getWindow());
        
        return alert.showAndWait();
    }
    
    /**
     * Show progress dialog (placeholder for future implementation)
     * @param title Dialog title
     * @param message Progress message
     */
    public static void showProgress(String title, String message) {
        // TODO: Implement progress dialog
        logger.debug("Progress dialog requested: {} - {}", title, message);
    }
}
