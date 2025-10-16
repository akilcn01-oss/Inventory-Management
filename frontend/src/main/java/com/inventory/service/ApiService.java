package com.inventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventory.config.AppConfig;
import com.inventory.model.DashboardStats;
import com.inventory.model.Product;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service class for handling API communication with the backend
 * Provides methods for all CRUD operations and dashboard data
 */
public class ApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);
    private static ApiService instance;
    
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final ExecutorService executorService;
    
    private ApiService() {
        AppConfig config = AppConfig.getInstance();
        this.baseUrl = config.getApiBaseUrl();
        
        // Configure HTTP client with timeout
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getApiTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(config.getApiTimeout()))
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        // Configure JSON mapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Thread pool for async operations
        this.executorService = Executors.newFixedThreadPool(5);
        
        logger.info("ApiService initialized with base URL: {}", baseUrl);
    }
    
    /**
     * Get singleton instance
     * @return ApiService instance
     */
    public static synchronized ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }
    
    /**
     * Test API connection
     * @return true if connection successful
     */
    public boolean testConnection() {
        try {
            HttpGet request = new HttpGet(baseUrl + "/health");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                logger.debug("Health check response: {}", statusCode);
                return statusCode == 200;
            }
        } catch (Exception e) {
            logger.error("API connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get all products with optional filters
     * @param skip Number of items to skip
     * @param limit Number of items to return
     * @param category Category filter
     * @param search Search term
     * @param lowStock Low stock filter
     * @return List of products
     */
    public List<Product> getProducts(int skip, int limit, String category, String search, Boolean lowStock) {
        try {
            StringBuilder url = new StringBuilder(baseUrl + "/products");
            url.append("?skip=").append(skip).append("&limit=").append(limit);
            
            if (category != null && !category.trim().isEmpty()) {
                url.append("&category=").append(category);
            }
            if (search != null && !search.trim().isEmpty()) {
                url.append("&search=").append(search);
            }
            if (lowStock != null) {
                url.append("&low_stock=").append(lowStock);
            }
            
            HttpGet request = new HttpGet(url.toString());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getCode() == 200) {
                    List<Product> products = objectMapper.readValue(responseBody, new TypeReference<List<Product>>() {});
                    logger.debug("Retrieved {} products", products.size());
                    return products;
                } else {
                    logger.error("Failed to get products. Status: {}, Response: {}", response.getCode(), responseBody);
                    throw new RuntimeException("Failed to retrieve products: " + responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting products", e);
            throw new RuntimeException("Failed to retrieve products", e);
        }
    }
    
    /**
     * Get all products (convenience method)
     * @return List of all products
     */
    public List<Product> getAllProducts() {
        return getProducts(0, 1000, null, null, null);
    }
    
    /**
     * Get product by ID
     * @param productId Product ID
     * @return Product or null if not found
     */
    public Product getProduct(int productId) {
        try {
            HttpGet request = new HttpGet(baseUrl + "/products/" + productId);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getCode() == 200) {
                    Product product = objectMapper.readValue(responseBody, Product.class);
                    logger.debug("Retrieved product: {}", product.getName());
                    return product;
                } else if (response.getCode() == 404) {
                    logger.debug("Product not found: {}", productId);
                    return null;
                } else {
                    logger.error("Failed to get product. Status: {}, Response: {}", response.getCode(), responseBody);
                    throw new RuntimeException("Failed to retrieve product: " + responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting product: {}", productId, e);
            throw new RuntimeException("Failed to retrieve product", e);
        }
    }
    
    /**
     * Create new product
     * @param product Product to create
     * @return Created product with ID
     */
    public Product createProduct(Product product) {
        try {
            HttpPost request = new HttpPost(baseUrl + "/products");
            String jsonBody = objectMapper.writeValueAsString(product);
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getCode() == 200) {
                    Product createdProduct = objectMapper.readValue(responseBody, Product.class);
                    logger.info("Created product: {} (ID: {})", createdProduct.getName(), createdProduct.getId());
                    return createdProduct;
                } else {
                    logger.error("Failed to create product. Status: {}, Response: {}", response.getCode(), responseBody);
                    throw new RuntimeException("Failed to create product: " + responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("Error creating product: {}", product.getName(), e);
            throw new RuntimeException("Failed to create product", e);
        }
    }
    
    /**
     * Update existing product
     * @param productId Product ID
     * @param product Updated product data
     * @return Updated product
     */
    public Product updateProduct(int productId, Product product) {
        try {
            HttpPut request = new HttpPut(baseUrl + "/products/" + productId);
            String jsonBody = objectMapper.writeValueAsString(product);
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getCode() == 200) {
                    Product updatedProduct = objectMapper.readValue(responseBody, Product.class);
                    logger.info("Updated product: {} (ID: {})", updatedProduct.getName(), updatedProduct.getId());
                    return updatedProduct;
                } else {
                    logger.error("Failed to update product. Status: {}, Response: {}", response.getCode(), responseBody);
                    throw new RuntimeException("Failed to update product: " + responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating product: {}", productId, e);
            throw new RuntimeException("Failed to update product", e);
        }
    }
    
    /**
     * Delete product
     * @param productId Product ID
     * @return true if deleted successfully
     */
    public boolean deleteProduct(int productId) {
        try {
            HttpDelete request = new HttpDelete(baseUrl + "/products/" + productId);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getCode() == 200) {
                    logger.info("Deleted product: {}", productId);
                    return true;
                } else {
                    logger.error("Failed to delete product. Status: {}, Response: {}", response.getCode(), responseBody);
                    throw new RuntimeException("Failed to delete product: " + responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("Error deleting product: {}", productId, e);
            throw new RuntimeException("Failed to delete product", e);
        }
    }
    
    /**
     * Get dashboard statistics
     * @return Dashboard statistics
     */
    public DashboardStats getDashboardStats() {
        try {
            HttpGet request = new HttpGet(baseUrl + "/dashboard/stats");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getCode() == 200) {
                    DashboardStats stats = objectMapper.readValue(responseBody, DashboardStats.class);
                    logger.debug("Retrieved dashboard stats: {} products", stats.getTotalProducts());
                    return stats;
                } else {
                    logger.error("Failed to get dashboard stats. Status: {}, Response: {}", response.getCode(), responseBody);
                    throw new RuntimeException("Failed to retrieve dashboard statistics: " + responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting dashboard stats", e);
            throw new RuntimeException("Failed to retrieve dashboard statistics", e);
        }
    }
    
    /**
     * Get all categories
     * @return List of categories
     */
    @SuppressWarnings("unchecked")
    public List<String> getCategories() {
        try {
            HttpGet request = new HttpGet(baseUrl + "/categories");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (response.getCode() == 200) {
                    Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                    List<String> categories = (List<String>) result.get("categories");
                    logger.debug("Retrieved {} categories", categories.size());
                    return categories;
                } else {
                    logger.error("Failed to get categories. Status: {}, Response: {}", response.getCode(), responseBody);
                    throw new RuntimeException("Failed to retrieve categories: " + responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting categories", e);
            throw new RuntimeException("Failed to retrieve categories", e);
        }
    }
    
    // Async methods for better UI responsiveness
    
    /**
     * Get products asynchronously
     * @param skip Number of items to skip
     * @param limit Number of items to return
     * @param category Category filter
     * @param search Search term
     * @param lowStock Low stock filter
     * @return CompletableFuture with list of products
     */
    public CompletableFuture<List<Product>> getProductsAsync(int skip, int limit, String category, String search, Boolean lowStock) {
        return CompletableFuture.supplyAsync(() -> getProducts(skip, limit, category, search, lowStock), executorService);
    }
    
    /**
     * Get dashboard statistics asynchronously
     * @return CompletableFuture with dashboard statistics
     */
    public CompletableFuture<DashboardStats> getDashboardStatsAsync() {
        return CompletableFuture.supplyAsync(this::getDashboardStats, executorService);
    }
    
    /**
     * Create product asynchronously
     * @param product Product to create
     * @return CompletableFuture with created product
     */
    public CompletableFuture<Product> createProductAsync(Product product) {
        return CompletableFuture.supplyAsync(() -> createProduct(product), executorService);
    }
    
    /**
     * Update product asynchronously
     * @param productId Product ID
     * @param product Updated product data
     * @return CompletableFuture with updated product
     */
    public CompletableFuture<Product> updateProductAsync(int productId, Product product) {
        return CompletableFuture.supplyAsync(() -> updateProduct(productId, product), executorService);
    }
    
    /**
     * Delete product asynchronously
     * @param productId Product ID
     * @return CompletableFuture with deletion result
     */
    public CompletableFuture<Boolean> deleteProductAsync(int productId) {
        return CompletableFuture.supplyAsync(() -> deleteProduct(productId), executorService);
    }
    
    /**
     * Shutdown the service and cleanup resources
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            httpClient.close();
            logger.info("ApiService shutdown completed");
        } catch (IOException e) {
            logger.error("Error during ApiService shutdown", e);
        }
    }
}
