package com.inventory.controller;

import com.inventory.model.Product;
import com.inventory.service.ApiService;
import com.inventory.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Controller for the product list view
 * Handles product display, search, filtering, and CRUD operations
 */
public class ProductListController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductListController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    @FXML private VBox productListRoot;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private CheckBox lowStockFilter;
    @FXML private Button refreshButton;
    @FXML private Button addButton;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, Integer> quantityColumn;
    @FXML private TableColumn<Product, Double> priceColumn;
    @FXML private TableColumn<Product, String> descriptionColumn;
    @FXML private TableColumn<Product, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;
    @FXML private Label statusLabel;
    @FXML private Pagination pagination;
    
    private MainController mainController;
    private ApiService apiService;
    private ObservableList<Product> allProducts;
    private FilteredList<Product> filteredProducts;
    private SortedList<Product> sortedProducts;
    
    private static final int ITEMS_PER_PAGE = 20;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("Initializing ProductListController");
        
        setupTable();
        setupFilters();
        setupPagination();
        
        allProducts = FXCollections.observableArrayList();
        filteredProducts = new FilteredList<>(allProducts);
        sortedProducts = new SortedList<>(filteredProducts);
        
        productTable.setItems(sortedProducts);
        sortedProducts.comparatorProperty().bind(productTable.comparatorProperty());
        
        logger.debug("ProductListController initialized");
    }
    
    /**
     * Setup table columns and properties
     */
    private void setupTable() {
        // Configure columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Format price column
        priceColumn.setCellFactory(column -> new TableCell<Product, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", price));
                }
            }
        });
        
        // Format quantity column with low stock warning
        quantityColumn.setCellFactory(column -> new TableCell<Product, Integer>() {
            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                if (empty || quantity == null) {
                    setText(null);
                    getStyleClass().remove("low-stock");
                } else {
                    setText(quantity.toString());
                    if (quantity < 10) {
                        getStyleClass().add("low-stock");
                    } else {
                        getStyleClass().remove("low-stock");
                    }
                }
            }
        });
        
        // Format date column
        createdAtColumn.setCellFactory(column -> new TableCell<Product, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DATE_FORMATTER));
                }
            }
        });
        
        // Setup actions column
        setupActionsColumn();
        
        // Enable row selection
        productTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // Handle double-click to edit
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editProduct(row.getItem());
                }
            });
            return row;
        });
    }
    
    /**
     * Setup actions column with edit and delete buttons
     */
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(new Callback<TableColumn<Product, Void>, TableCell<Product, Void>>() {
            @Override
            public TableCell<Product, Void> call(TableColumn<Product, Void> param) {
                return new TableCell<Product, Void>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    
                    {
                        editButton.getStyleClass().add("action-button");
                        deleteButton.getStyleClass().addAll("action-button", "delete-button");
                        
                        editButton.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            editProduct(product);
                        });
                        
                        deleteButton.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            deleteProduct(product);
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(new javafx.scene.layout.HBox(5, editButton, deleteButton));
                        }
                    }
                };
            }
        });
    }
    
    /**
     * Setup search and filter controls
     */
    private void setupFilters() {
        // Setup search field
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Setup category filter
        categoryFilter.getItems().add("All Categories");
        categoryFilter.setValue("All Categories");
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Setup low stock filter
        lowStockFilter.selectedProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Setup buttons
        refreshButton.setOnAction(e -> refreshProducts());
        addButton.setOnAction(e -> addProduct());
    }
    
    /**
     * Setup pagination
     */
    private void setupPagination() {
        pagination.setPageCount(1);
        pagination.setCurrentPageIndex(0);
        pagination.setPageFactory(this::createPage);
    }
    
    /**
     * Create page for pagination
     * @param pageIndex Page index
     * @return Page content
     */
    private javafx.scene.Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, sortedProducts.size());
        
        ObservableList<Product> pageItems = FXCollections.observableArrayList(
            sortedProducts.subList(fromIndex, toIndex)
        );
        
        productTable.setItems(pageItems);
        return productTable;
    }
    
    /**
     * Apply search and filter criteria
     */
    private void applyFilters() {
        filteredProducts.setPredicate(createFilterPredicate());
        updatePagination();
        updateStatusLabel();
    }
    
    /**
     * Create filter predicate based on current filter settings
     * @return Filter predicate
     */
    private Predicate<Product> createFilterPredicate() {
        return product -> {
            // Search filter
            String searchText = searchField.getText();
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                if (!product.getName().toLowerCase().contains(lowerCaseFilter) &&
                    !product.getDescription().toLowerCase().contains(lowerCaseFilter)) {
                    return false;
                }
            }
            
            // Category filter
            String selectedCategory = categoryFilter.getValue();
            if (selectedCategory != null && !"All Categories".equals(selectedCategory)) {
                if (!product.getCategory().equals(selectedCategory)) {
                    return false;
                }
            }
            
            // Low stock filter
            if (lowStockFilter.isSelected()) {
                if (product.getQuantity() >= 10) {
                    return false;
                }
            }
            
            return true;
        };
    }
    
    /**
     * Update pagination based on filtered results
     */
    private void updatePagination() {
        int totalItems = filteredProducts.size();
        int pageCount = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        pagination.setPageCount(Math.max(1, pageCount));
        
        if (pagination.getCurrentPageIndex() >= pageCount) {
            pagination.setCurrentPageIndex(Math.max(0, pageCount - 1));
        }
    }
    
    /**
     * Update status label with current item count
     */
    private void updateStatusLabel() {
        int totalItems = allProducts.size();
        int filteredItems = filteredProducts.size();
        
        if (totalItems == filteredItems) {
            statusLabel.setText(String.format("Showing %d products", totalItems));
        } else {
            statusLabel.setText(String.format("Showing %d of %d products", filteredItems, totalItems));
        }
    }
    
    /**
     * Update products list
     * @param products New products list
     */
    public void updateProducts(ObservableList<Product> products) {
        Platform.runLater(() -> {
            allProducts.setAll(products);
            updateCategoryFilter();
            applyFilters();
            logger.debug("Products list updated with {} items", products.size());
        });
    }
    
    /**
     * Update category filter with available categories
     */
    private void updateCategoryFilter() {
        String currentSelection = categoryFilter.getValue();
        categoryFilter.getItems().clear();
        categoryFilter.getItems().add("All Categories");
        
        allProducts.stream()
            .map(Product::getCategory)
            .distinct()
            .sorted()
            .forEach(category -> categoryFilter.getItems().add(category));
        
        // Restore selection if still valid
        if (categoryFilter.getItems().contains(currentSelection)) {
            categoryFilter.setValue(currentSelection);
        } else {
            categoryFilter.setValue("All Categories");
        }
    }
    
    /**
     * Refresh products from server
     */
    @FXML
    private void refreshProducts() {
        if (mainController != null) {
            mainController.refreshProducts();
        }
    }
    
    /**
     * Add new product
     */
    @FXML
    private void addProduct() {
        if (mainController != null) {
            mainController.showProductForm(null);
        }
    }
    
    /**
     * Edit selected product
     * @param product Product to edit
     */
    private void editProduct(Product product) {
        if (product != null && mainController != null) {
            logger.debug("Editing product: {}", product.getName());
            mainController.showProductForm(product);
        }
    }
    
    /**
     * Delete selected product
     * @param product Product to delete
     */
    private void deleteProduct(Product product) {
        if (product == null) return;
        
        boolean confirmed = AlertUtil.showDeleteConfirmation(product.getName());
        if (!confirmed) return;
        
        logger.info("Deleting product: {}", product.getName());
        
        Task<Boolean> deleteTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return apiService.deleteProduct(product.getId());
            }
        };
        
        deleteTask.setOnSucceeded(e -> {
            if (deleteTask.getValue()) {
                mainController.onProductDeleted(product);
                logger.info("Product deleted successfully: {}", product.getName());
            }
        });
        
        deleteTask.setOnFailed(e -> {
            logger.error("Error deleting product: {}", product.getName(), deleteTask.getException());
            AlertUtil.showSaveError("delete product");
        });
        
        Thread deleteThread = new Thread(deleteTask);
        deleteThread.setDaemon(true);
        deleteThread.start();
    }
    
    /**
     * Get the root view node
     * @return Root view node
     */
    public Parent getView() {
        return productListRoot;
    }
    
    /**
     * Set the main controller reference
     * @param mainController Main controller
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
        this.apiService = mainController.getApiService();
    }
    
    /**
     * Get selected product
     * @return Selected product or null
     */
    public Product getSelectedProduct() {
        return productTable.getSelectionModel().getSelectedItem();
    }
    
    /**
     * Select product by ID
     * @param productId Product ID to select
     */
    public void selectProduct(int productId) {
        for (Product product : productTable.getItems()) {
            if (product.getId() == productId) {
                productTable.getSelectionModel().select(product);
                productTable.scrollTo(product);
                break;
            }
        }
    }
    
    /**
     * Apply low stock filter
     * @param enabled Whether to enable low stock filter
     */
    public void setLowStockFilter(boolean enabled) {
        lowStockFilter.setSelected(enabled);
    }
}
