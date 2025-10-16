package com.inventory.controller;

import com.inventory.model.Product;
import com.inventory.service.ApiService;
import com.inventory.util.AlertUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Documents view
 * Handles PDF document generation and downloads
 */
public class DocumentsController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentsController.class);
    
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label totalProductsLabel;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label lowStockCountLabel;
    @FXML private Label criticalStockLabel;
    @FXML private Label statusLabel;
    @FXML private Button downloadFullListButton;
    @FXML private Button downloadLowStockButton;
    
    private MainController mainController;
    private ApiService apiService;
    private List<Product> products;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initializing DocumentsController");
        
        loadingIndicator.setVisible(false);
        statusLabel.setText("");
        
        logger.debug("DocumentsController initialized");
    }
    
    /**
     * Set the main controller reference
     * @param mainController Main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.apiService = mainController.getApiService();
        
        // Load statistics
        loadStatistics();
    }
    
    /**
     * Load product statistics for display
     */
    private void loadStatistics() {
        Task<List<Product>> loadTask = new Task<List<Product>>() {
            @Override
            protected List<Product> call() throws Exception {
                return apiService.getProducts(0, 1000, null, null, null);
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            products = loadTask.getValue();
            updateStatistics();
        });
        
        loadTask.setOnFailed(e -> {
            logger.error("Error loading products for statistics", loadTask.getException());
            totalProductsLabel.setText("--");
            totalCategoriesLabel.setText("--");
            lowStockCountLabel.setText("--");
            criticalStockLabel.setText("--");
        });
        
        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }
    
    /**
     * Update statistics labels
     */
    private void updateStatistics() {
        if (products == null) return;
        
        int totalProducts = products.size();
        long totalCategories = products.stream()
                .map(Product::getCategory)
                .distinct()
                .count();
        long lowStockCount = products.stream()
                .filter(p -> p.getQuantity() < 10)
                .count();
        long criticalCount = products.stream()
                .filter(p -> p.getQuantity() < 5)
                .count();
        
        totalProductsLabel.setText(String.valueOf(totalProducts));
        totalCategoriesLabel.setText(String.valueOf(totalCategories));
        lowStockCountLabel.setText(String.valueOf(lowStockCount));
        criticalStockLabel.setText(String.valueOf(criticalCount));
        
        logger.debug("Statistics updated: {} products, {} low stock", totalProducts, lowStockCount);
    }
    
    /**
     * Download full product list PDF
     */
    @FXML
    private void downloadFullProductList() {
        logger.info("Downloading full product list PDF");
        
        // Show file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Product List PDF");
        fileChooser.setInitialFileName(generateFileName("product_list"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showSaveDialog(downloadFullListButton.getScene().getWindow());
        
        if (file != null) {
            downloadPDF("/documents/products/full", file, "Full Product List");
        }
    }
    
    /**
     * Download low stock report PDF
     */
    @FXML
    private void downloadLowStockReport() {
        logger.info("Downloading low stock report PDF");
        
        // Show file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Low Stock Report PDF");
        fileChooser.setInitialFileName(generateFileName("low_stock_report"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        
        File file = fileChooser.showSaveDialog(downloadLowStockButton.getScene().getWindow());
        
        if (file != null) {
            downloadPDF("/documents/products/low-stock", file, "Low Stock Report");
        }
    }
    
    /**
     * Download PDF from API endpoint
     * @param endpoint API endpoint
     * @param file File to save to
     * @param reportName Name of the report for display
     */
    private void downloadPDF(String endpoint, File file, String reportName) {
        // Show loading
        setLoading(true);
        setButtonsEnabled(false);
        updateStatus("Generating PDF...");
        
        Task<byte[]> downloadTask = new Task<byte[]>() {
            @Override
            protected byte[] call() throws Exception {
                return apiService.downloadPDF(endpoint);
            }
        };
        
        downloadTask.setOnSucceeded(e -> {
            try {
                byte[] pdfBytes = downloadTask.getValue();
                
                // Save to file
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(pdfBytes);
                }
                
                logger.info("{} PDF downloaded successfully: {}", reportName, file.getAbsolutePath());
                updateStatus("✓ PDF saved successfully: " + file.getName());
                AlertUtil.showSuccess(reportName + " downloaded successfully to:\n" + file.getAbsolutePath());
                
                // Ask if user wants to open the file
                if (AlertUtil.showConfirmation("Open PDF", 
                        "PDF saved successfully. Do you want to open it now?",
                        "The PDF has been saved to your computer.")) {
                    openFile(file);
                }
                
            } catch (Exception ex) {
                logger.error("Error saving PDF file", ex);
                updateStatus("✗ Error saving PDF file");
                AlertUtil.showError("Save Error", "Failed to save PDF file", ex.getMessage());
            } finally {
                setLoading(false);
                setButtonsEnabled(true);
            }
        });
        
        downloadTask.setOnFailed(e -> {
            Throwable exception = downloadTask.getException();
            logger.error("Error downloading PDF", exception);
            updateStatus("✗ Error generating PDF");
            
            String errorMessage = exception.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "An unexpected error occurred while generating the PDF.";
            }
            
            AlertUtil.showError("Download Failed", "Failed to generate " + reportName, errorMessage);
            
            setLoading(false);
            setButtonsEnabled(true);
        });
        
        Thread downloadThread = new Thread(downloadTask);
        downloadThread.setDaemon(true);
        downloadThread.start();
    }
    
    /**
     * Generate filename with timestamp
     * @param prefix Filename prefix
     * @return Generated filename
     */
    private String generateFileName(String prefix) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return prefix + "_" + timestamp + ".pdf";
    }
    
    /**
     * Open file with default application
     * @param file File to open
     */
    private void openFile(File file) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (file.exists()) {
                    desktop.open(file);
                }
            }
        } catch (Exception e) {
            logger.error("Error opening PDF file", e);
            AlertUtil.showError("Open Error", "Could not open PDF file", e.getMessage());
        }
    }
    
    /**
     * Set loading indicator visibility
     * @param loading Whether loading is in progress
     */
    private void setLoading(boolean loading) {
        Platform.runLater(() -> loadingIndicator.setVisible(loading));
    }
    
    /**
     * Enable/disable download buttons
     * @param enabled Whether buttons should be enabled
     */
    private void setButtonsEnabled(boolean enabled) {
        Platform.runLater(() -> {
            downloadFullListButton.setDisable(!enabled);
            downloadLowStockButton.setDisable(!enabled);
        });
    }
    
    /**
     * Update status label
     * @param message Status message
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
    
    /**
     * Refresh statistics
     */
    public void refresh() {
        logger.debug("Refreshing documents view");
        loadStatistics();
        updateStatus("");
    }
}
