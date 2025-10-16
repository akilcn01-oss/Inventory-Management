package com.inventory.controller;

import com.inventory.model.Product;
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
 * Controller for the product form (add/edit product)
 * Handles product creation and editing with validation
 */
public class ProductFormController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductFormController.class);
    
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private TextField priceField;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label titleLabel;
    @FXML private ProgressIndicator loadingIndicator;
    
    private MainController mainController;
    private ApiService apiService;
    private Product currentProduct;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initializing ProductFormController");
        
        setupForm();
        setupValidation();
        setupButtons();
        
        loadingIndicator.setVisible(false);
        
        logger.debug("ProductFormController initialized");
    }
    
    /**
     * Setup form controls
     */
    private void setupForm() {
        // Setup quantity spinner
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 99999, 0));
        quantitySpinner.setEditable(true);
        
        // Setup category combo box with common categories
        categoryComboBox.getItems().addAll(
            "Electronics",
            "Clothing",
            "Books",
            "Home & Garden",
            "Sports",
            "Food & Beverages",
            "Health & Beauty",
            "Automotive"
        );
        categoryComboBox.setEditable(true);
        
        // Setup price field to accept only numbers and decimal point
        priceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                priceField.setText(oldValue);
            }
        });
        
        // Limit description length
        descriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 1000) {
                descriptionArea.setText(oldValue);
            }
        });
    }
    
    /**
     * Setup real-time validation
     */
    private void setupValidation() {
        // Add listeners for real-time validation
        nameField.textProperty().addListener((observable, oldValue, newValue) -> validateForm());
        categoryComboBox.valueProperty().addListener((observable, oldValue, newValue) -> validateForm());
        quantitySpinner.valueProperty().addListener((observable, oldValue, newValue) -> validateForm());
        priceField.textProperty().addListener((observable, oldValue, newValue) -> validateForm());
        
        // Initial validation
        validateForm();
    }
    
    /**
     * Setup button actions
     */
    private void setupButtons() {
        saveButton.setOnAction(e -> saveProduct());
        cancelButton.setOnAction(e -> closeForm());
    }
    
    /**
     * Validate form and enable/disable save button
     */
    private void validateForm() {
        try {
            boolean isValid = isFormValid();
            saveButton.setDisable(!isValid);
            
            // Update field styles based on validation
            updateFieldStyle(nameField, !nameField.getText().trim().isEmpty());
            
            String categoryValue = categoryComboBox.getValue();
            updateFieldStyle(categoryComboBox, categoryValue != null && !categoryValue.trim().isEmpty());
            
            updateFieldStyle(priceField, isPriceValid());
        } catch (Exception e) {
            logger.error("Error during form validation", e);
            saveButton.setDisable(true);
        }
    }
    
    /**
     * Check if form is valid
     * @return true if form is valid
     */
    private boolean isFormValid() {
        return !nameField.getText().trim().isEmpty() &&
               categoryComboBox.getValue() != null && !categoryComboBox.getValue().trim().isEmpty() &&
               quantitySpinner.getValue() != null && quantitySpinner.getValue() >= 0 &&
               isPriceValid();
    }
    
    /**
     * Check if price is valid
     * @return true if price is valid
     */
    private boolean isPriceValid() {
        try {
            String priceText = priceField.getText().trim();
            if (priceText.isEmpty()) return false;
            double price = Double.parseDouble(priceText);
            return price > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Update field style based on validation
     * @param control Control to update
     * @param isValid Whether the field is valid
     */
    private void updateFieldStyle(Control control, boolean isValid) {
        if (isValid) {
            control.getStyleClass().remove("error-field");
        } else {
            if (!control.getStyleClass().contains("error-field")) {
                control.getStyleClass().add("error-field");
            }
        }
    }
    
    /**
     * Set product for editing
     * @param product Product to edit (null for new product)
     */
    public void setProduct(Product product) {
        this.currentProduct = product;
        this.isEditMode = product != null;
        
        if (isEditMode) {
            titleLabel.setText("Edit Product");
            populateForm(product);
        } else {
            titleLabel.setText("Add Product");
            clearForm();
        }
        
        logger.debug("Product form set to {} mode", isEditMode ? "edit" : "add");
    }
    
    /**
     * Populate form with product data
     * @param product Product data
     */
    private void populateForm(Product product) {
        nameField.setText(product.getName());
        categoryComboBox.setValue(product.getCategory());
        quantitySpinner.getValueFactory().setValue(product.getQuantity());
        priceField.setText(String.valueOf(product.getPrice()));
        descriptionArea.setText(product.getDescription());
    }
    
    /**
     * Clear form fields
     */
    private void clearForm() {
        nameField.clear();
        categoryComboBox.setValue(null);
        quantitySpinner.getValueFactory().setValue(0);
        priceField.clear();
        descriptionArea.clear();
    }
    
    /**
     * Save product (create or update)
     */
    @FXML
    private void saveProduct() {
        if (!isFormValid()) {
            AlertUtil.showValidationError("Please correct the form errors before saving.");
            return;
        }
        
        try {
            Product product = createProductFromForm();
            
            // Validate product
            String validationError = product.getValidationError();
            if (validationError != null) {
                AlertUtil.showValidationError(validationError);
                return;
            }
            
            // Show loading
            setFormEnabled(false);
            loadingIndicator.setVisible(true);
            
            // Save product asynchronously
            Task<Product> saveTask = createSaveTask(product);
            
            saveTask.setOnSucceeded(e -> {
                try {
                    Product savedProduct = saveTask.getValue();
                    logger.info("Product saved successfully: {}", savedProduct.getName());
                    
                    // Notify main controller
                    if (mainController != null) {
                        mainController.onProductSaved(savedProduct, !isEditMode);
                    }
                    
                    // Close form
                    closeForm();
                } catch (Exception ex) {
                    logger.error("Error processing saved product", ex);
                    AlertUtil.showError("Save Error", "Product was saved but there was an error updating the UI", ex.getMessage());
                    closeForm();
                }
            });
            
            saveTask.setOnFailed(e -> {
                Throwable exception = saveTask.getException();
                logger.error("Error saving product", exception);
                
                String errorMessage = exception.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "An unexpected error occurred while saving the product.";
                }
                
                AlertUtil.showError(
                    "Save Failed",
                    "Failed to " + (isEditMode ? "update" : "create") + " product",
                    errorMessage
                );
                
                // Re-enable form
                setFormEnabled(true);
                loadingIndicator.setVisible(false);
            });
            
            Thread saveThread = new Thread(saveTask);
            saveThread.setDaemon(true);
            saveThread.start();
            
        } catch (Exception e) {
            logger.error("Error preparing product for save", e);
            AlertUtil.showError("Form Error", e);
        }
    }
    
    /**
     * Create product object from form data
     * @return Product object
     */
    private Product createProductFromForm() {
        Product product = isEditMode ? currentProduct.copy() : new Product();
        
        product.setName(nameField.getText().trim());
        product.setCategory(categoryComboBox.getValue().trim());
        product.setQuantity(quantitySpinner.getValue());
        product.setPrice(Double.parseDouble(priceField.getText().trim()));
        product.setDescription(descriptionArea.getText().trim());
        
        return product;
    }
    
    /**
     * Create save task based on edit mode
     * @param product Product to save
     * @return Save task
     */
    private Task<Product> createSaveTask(Product product) {
        if (isEditMode) {
            return new Task<Product>() {
                @Override
                protected Product call() throws Exception {
                    return apiService.updateProduct(currentProduct.getId(), product);
                }
            };
        } else {
            return new Task<Product>() {
                @Override
                protected Product call() throws Exception {
                    return apiService.createProduct(product);
                }
            };
        }
    }
    
    /**
     * Enable/disable form controls
     * @param enabled Whether to enable controls
     */
    private void setFormEnabled(boolean enabled) {
        nameField.setDisable(!enabled);
        categoryComboBox.setDisable(!enabled);
        quantitySpinner.setDisable(!enabled);
        priceField.setDisable(!enabled);
        descriptionArea.setDisable(!enabled);
        saveButton.setDisable(!enabled);
        cancelButton.setDisable(!enabled);
    }
    
    /**
     * Close the form
     */
    @FXML
    private void closeForm() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Handle form reset
     */
    @FXML
    private void resetForm() {
        if (isEditMode && currentProduct != null) {
            populateForm(currentProduct);
        } else {
            clearForm();
        }
        validateForm();
    }
    
    /**
     * Set the main controller reference
     * @param mainController Main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.apiService = mainController.getApiService();
        
        // Load categories from API
        loadCategories();
    }
    
    /**
     * Load categories from API
     */
    private void loadCategories() {
        Task<java.util.List<String>> categoriesTask = new Task<java.util.List<String>>() {
            @Override
            protected java.util.List<String> call() throws Exception {
                return apiService.getCategories();
            }
        };
        
        categoriesTask.setOnSucceeded(e -> {
            java.util.List<String> categories = categoriesTask.getValue();
            
            // Add categories that aren't already in the combo box
            for (String category : categories) {
                if (!categoryComboBox.getItems().contains(category)) {
                    categoryComboBox.getItems().add(category);
                }
            }
            
            // Sort categories
            categoryComboBox.getItems().sort(String::compareToIgnoreCase);
            
            logger.debug("Loaded {} categories from API", categories.size());
        });
        
        categoriesTask.setOnFailed(e -> {
            logger.warn("Failed to load categories from API", categoriesTask.getException());
            // Continue with default categories
        });
        
        Thread categoriesThread = new Thread(categoriesTask);
        categoriesThread.setDaemon(true);
        categoriesThread.start();
    }
    
    /**
     * Get current product being edited
     * @return Current product or null
     */
    public Product getCurrentProduct() {
        return currentProduct;
    }
    
    /**
     * Check if form is in edit mode
     * @return true if editing existing product
     */
    public boolean isEditMode() {
        return isEditMode;
    }
}
