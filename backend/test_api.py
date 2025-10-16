"""
Test cases for the Inventory Management API
Comprehensive tests for all endpoints and functionality
"""

import pytest
import json
from fastapi.testclient import TestClient
from unittest.mock import Mock, patch
import sys
import os

# Add backend directory to path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from main import app
from models import ProductCreate, ProductUpdate

# Create test client
client = TestClient(app)

# Test data
sample_product = {
    "name": "Test Product",
    "category": "Test Category",
    "quantity": 10,
    "price": 29.99,
    "description": "A test product for unit testing"
}

sample_product_update = {
    "name": "Updated Test Product",
    "quantity": 15,
    "price": 39.99
}


class TestHealthEndpoints:
    """Test health and root endpoints"""
    
    def test_root_endpoint(self):
        """Test root endpoint returns correct information"""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert data["message"] == "Inventory Management API"
        assert data["version"] == "1.0.0"
        assert "endpoints" in data
    
    @patch('main.db_manager.execute_query')
    def test_health_check_healthy(self, mock_execute):
        """Test health check when database is healthy"""
        mock_execute.return_value = [(1,)]
        
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["database"] == "connected"
    
    @patch('main.db_manager.execute_query')
    def test_health_check_unhealthy(self, mock_execute):
        """Test health check when database is unhealthy"""
        mock_execute.side_effect = Exception("Database connection failed")
        
        response = client.get("/health")
        assert response.status_code == 503
        data = response.json()
        assert data["status"] == "unhealthy"
        assert data["database"] == "disconnected"


class TestProductEndpoints:
    """Test product CRUD endpoints"""
    
    @patch('main.db_manager.execute_query')
    def test_get_products_success(self, mock_execute):
        """Test getting products successfully"""
        # Mock database response
        mock_execute.return_value = [
            (1, "Test Product", "Electronics", 10, 29.99, "Test description", 
             "2023-01-01 10:00:00", "2023-01-01 10:00:00")
        ]
        
        response = client.get("/products")
        assert response.status_code == 200
        data = response.json()
        assert len(data) == 1
        assert data[0]["name"] == "Test Product"
        assert data[0]["category"] == "Electronics"
    
    @patch('main.db_manager.execute_query')
    def test_get_products_with_filters(self, mock_execute):
        """Test getting products with filters"""
        mock_execute.return_value = []
        
        response = client.get("/products?category=Electronics&search=phone&low_stock=true")
        assert response.status_code == 200
        
        # Verify the query was called with correct parameters
        mock_execute.assert_called_once()
        call_args = mock_execute.call_args
        query = call_args[0][0]
        params = call_args[0][1]
        
        assert "category = %s" in query
        assert "name LIKE %s OR description LIKE %s" in query
        assert "quantity < 10" in query
        assert "Electronics" in params
        assert "%phone%" in params
    
    @patch('main.db_manager.execute_query')
    def test_get_product_by_id_success(self, mock_execute):
        """Test getting a specific product by ID"""
        mock_execute.return_value = [
            (1, "Test Product", "Electronics", 10, 29.99, "Test description", 
             "2023-01-01 10:00:00", "2023-01-01 10:00:00")
        ]
        
        response = client.get("/products/1")
        assert response.status_code == 200
        data = response.json()
        assert data["id"] == 1
        assert data["name"] == "Test Product"
    
    @patch('main.db_manager.execute_query')
    def test_get_product_by_id_not_found(self, mock_execute):
        """Test getting a non-existent product"""
        mock_execute.return_value = []
        
        response = client.get("/products/999")
        assert response.status_code == 404
        data = response.json()
        assert "not found" in data["detail"]
    
    @patch('main.db_manager.execute_insert')
    @patch('main.db_manager.execute_query')
    def test_create_product_success(self, mock_query, mock_insert):
        """Test creating a new product successfully"""
        mock_insert.return_value = 1
        mock_query.return_value = [
            (1, "Test Product", "Test Category", 10, 29.99, "A test product", 
             "2023-01-01 10:00:00", "2023-01-01 10:00:00")
        ]
        
        response = client.post("/products", json=sample_product)
        assert response.status_code == 200
        data = response.json()
        assert data["name"] == sample_product["name"]
        assert data["category"] == sample_product["category"]
    
    def test_create_product_invalid_data(self):
        """Test creating a product with invalid data"""
        invalid_product = {
            "name": "",  # Empty name
            "category": "Test Category",
            "quantity": -1,  # Negative quantity
            "price": 0,  # Zero price
        }
        
        response = client.post("/products", json=invalid_product)
        assert response.status_code == 422  # Validation error
    
    @patch('main.db_manager.execute_update')
    @patch('main.db_manager.execute_query')
    def test_update_product_success(self, mock_query, mock_update):
        """Test updating a product successfully"""
        # Mock existing product
        mock_query.side_effect = [
            [(1, "Test Product", "Test Category", 10, 29.99, "Test description", 
              "2023-01-01 10:00:00", "2023-01-01 10:00:00")],
            [(1, "Updated Test Product", "Test Category", 15, 39.99, "Test description", 
              "2023-01-01 10:00:00", "2023-01-01 11:00:00")]
        ]
        mock_update.return_value = 1
        
        response = client.put("/products/1", json=sample_product_update)
        assert response.status_code == 200
        data = response.json()
        assert data["name"] == sample_product_update["name"]
        assert data["quantity"] == sample_product_update["quantity"]
    
    @patch('main.db_manager.execute_query')
    def test_update_product_not_found(self, mock_query):
        """Test updating a non-existent product"""
        mock_query.return_value = []
        
        response = client.put("/products/999", json=sample_product_update)
        assert response.status_code == 404
    
    @patch('main.db_manager.execute_update')
    @patch('main.db_manager.execute_query')
    def test_delete_product_success(self, mock_query, mock_update):
        """Test deleting a product successfully"""
        mock_query.return_value = [
            (1, "Test Product", "Test Category", 10, 29.99, "Test description", 
             "2023-01-01 10:00:00", "2023-01-01 10:00:00")
        ]
        mock_update.return_value = 1
        
        response = client.delete("/products/1")
        assert response.status_code == 200
        data = response.json()
        assert "deleted successfully" in data["message"]
    
    @patch('main.db_manager.execute_query')
    def test_delete_product_not_found(self, mock_query):
        """Test deleting a non-existent product"""
        mock_query.return_value = []
        
        response = client.delete("/products/999")
        assert response.status_code == 404


class TestDashboardEndpoints:
    """Test dashboard and statistics endpoints"""
    
    @patch('main.db_manager.execute_query')
    def test_get_dashboard_stats_success(self, mock_execute):
        """Test getting dashboard statistics successfully"""
        # Mock multiple query results
        mock_execute.side_effect = [
            [(25,)],  # total products
            [(5,)],   # total categories
            [(3,)],   # low stock count
            [(15000.50,)],  # total value
            [(2,)],   # recent products
            [("Electronics", 10), ("Clothing", 8), ("Books", 5)]  # top categories
        ]
        
        response = client.get("/dashboard/stats")
        assert response.status_code == 200
        data = response.json()
        
        assert data["total_products"] == 25
        assert data["total_categories"] == 5
        assert data["low_stock_count"] == 3
        assert data["total_inventory_value"] == 15000.50
        assert data["recent_products"] == 2
        assert len(data["top_categories"]) == 3
        assert data["top_categories"][0]["name"] == "Electronics"
    
    @patch('main.db_manager.execute_query')
    def test_get_categories_success(self, mock_execute):
        """Test getting product categories successfully"""
        mock_execute.return_value = [
            ("Electronics",),
            ("Clothing",),
            ("Books",),
            ("Home & Garden",)
        ]
        
        response = client.get("/categories")
        assert response.status_code == 200
        data = response.json()
        
        assert "categories" in data
        assert len(data["categories"]) == 4
        assert "Electronics" in data["categories"]
        assert "Clothing" in data["categories"]


class TestErrorHandling:
    """Test error handling scenarios"""
    
    @patch('main.db_manager.execute_query')
    def test_database_error_handling(self, mock_execute):
        """Test handling of database errors"""
        mock_execute.side_effect = Exception("Database connection failed")
        
        response = client.get("/products")
        assert response.status_code == 500
        data = response.json()
        assert "Failed to fetch products" in data["detail"]
    
    def test_validation_error_handling(self):
        """Test handling of validation errors"""
        invalid_data = {
            "name": "A" * 300,  # Too long
            "category": "",     # Empty
            "quantity": "invalid",  # Not a number
            "price": -10        # Negative
        }
        
        response = client.post("/products", json=invalid_data)
        assert response.status_code == 422


class TestPagination:
    """Test pagination functionality"""
    
    @patch('main.db_manager.execute_query')
    def test_pagination_parameters(self, mock_execute):
        """Test pagination with skip and limit parameters"""
        mock_execute.return_value = []
        
        response = client.get("/products?skip=10&limit=5")
        assert response.status_code == 200
        
        # Verify pagination parameters were used in query
        call_args = mock_execute.call_args
        params = call_args[0][1]
        assert 5 in params  # limit
        assert 10 in params  # skip
    
    def test_pagination_validation(self):
        """Test pagination parameter validation"""
        # Test negative skip
        response = client.get("/products?skip=-1")
        assert response.status_code == 422
        
        # Test zero limit
        response = client.get("/products?limit=0")
        assert response.status_code == 422
        
        # Test excessive limit
        response = client.get("/products?limit=2000")
        assert response.status_code == 422


# Integration test data
integration_test_products = [
    {
        "name": "Integration Test Product 1",
        "category": "Test Category",
        "quantity": 5,
        "price": 19.99,
        "description": "First integration test product"
    },
    {
        "name": "Integration Test Product 2",
        "category": "Test Category",
        "quantity": 15,
        "price": 39.99,
        "description": "Second integration test product"
    }
]


def run_integration_tests():
    """
    Run integration tests against a real database
    Note: This requires a test database to be set up
    """
    print("Running integration tests...")
    
    # Test creating products
    created_products = []
    for product_data in integration_test_products:
        response = client.post("/products", json=product_data)
        if response.status_code == 200:
            created_products.append(response.json())
            print(f"✓ Created product: {product_data['name']}")
        else:
            print(f"✗ Failed to create product: {product_data['name']}")
    
    # Test retrieving products
    response = client.get("/products")
    if response.status_code == 200:
        products = response.json()
        print(f"✓ Retrieved {len(products)} products")
    else:
        print("✗ Failed to retrieve products")
    
    # Test dashboard stats
    response = client.get("/dashboard/stats")
    if response.status_code == 200:
        stats = response.json()
        print(f"✓ Dashboard stats: {stats['total_products']} products")
    else:
        print("✗ Failed to get dashboard stats")
    
    # Clean up created products
    for product in created_products:
        response = client.delete(f"/products/{product['id']}")
        if response.status_code == 200:
            print(f"✓ Cleaned up product: {product['name']}")
        else:
            print(f"✗ Failed to clean up product: {product['name']}")
    
    print("Integration tests completed!")


if __name__ == "__main__":
    # Run unit tests
    pytest.main([__file__, "-v"])
    
    # Uncomment to run integration tests
    # run_integration_tests()
