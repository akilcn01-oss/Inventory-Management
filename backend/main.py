"""
Inventory Management System - FastAPI Backend
Main application entry point with API routes and configuration
"""

import logging
import os
from contextlib import asynccontextmanager
from typing import List, Optional

import uvicorn
from fastapi import FastAPI, HTTPException, Depends, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse
from dotenv import load_dotenv

from models import Product, ProductCreate, ProductUpdate, DashboardStats
from database import DatabaseManager
from utils import setup_logging
from pdf_generator import PDFGenerator

# Load environment variables
load_dotenv()

# Setup logging
logger = setup_logging()

# Database manager instance
db_manager = DatabaseManager()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager"""
    logger.info("Starting Inventory Management API...")
    
    # Initialize database connection
    try:
        db_manager.connect()
        logger.info("Database connection established")
    except Exception as e:
        logger.error(f"Failed to connect to database: {e}")
        raise
    
    yield
    
    # Cleanup
    logger.info("Shutting down Inventory Management API...")
    db_manager.disconnect()


# Create FastAPI application
app = FastAPI(
    title="Inventory Management API",
    description="A comprehensive inventory management system with CRUD operations",
    version="1.0.0",
    lifespan=lifespan
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify exact origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def get_db():
    """Dependency to get database manager"""
    return db_manager


@app.get("/", tags=["Root"])
async def root():
    """Root endpoint with API information"""
    return {
        "message": "Inventory Management API",
        "version": "1.0.0",
        "status": "active",
        "endpoints": {
            "products": "/products",
            "dashboard": "/dashboard/stats",
            "docs": "/docs"
        }
    }


@app.get("/health", tags=["Health"])
async def health_check():
    """Health check endpoint"""
    try:
        # Test database connection
        db_manager.execute_query("SELECT 1")
        return {"status": "healthy", "database": "connected"}
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return JSONResponse(
            status_code=503,
            content={"status": "unhealthy", "database": "disconnected", "error": str(e)}
        )


# Product Management Endpoints

@app.get("/products", response_model=List[Product], tags=["Products"])
async def get_products(
    skip: int = Query(0, ge=0, description="Number of products to skip"),
    limit: int = Query(100, ge=1, le=1000, description="Number of products to return"),
    category: Optional[str] = Query(None, description="Filter by category"),
    search: Optional[str] = Query(None, description="Search in product name or description"),
    low_stock: Optional[bool] = Query(None, description="Filter products with low stock (quantity < 10)"),
    db: DatabaseManager = Depends(get_db)
):
    """Get all products with optional filtering and pagination"""
    try:
        logger.info(f"Fetching products: skip={skip}, limit={limit}, category={category}, search={search}")
        
        # Build query with filters
        query = "SELECT * FROM products WHERE 1=1"
        params = []
        
        if category:
            query += " AND category = %s"
            params.append(category)
        
        if search:
            query += " AND (name LIKE %s OR description LIKE %s)"
            search_param = f"%{search}%"
            params.extend([search_param, search_param])
        
        if low_stock:
            query += " AND quantity < 10"
        
        query += " ORDER BY created_at DESC LIMIT %s OFFSET %s"
        params.extend([limit, skip])
        
        results = db.execute_query(query, params)
        
        products = []
        for row in results:
            product = Product(
                id=row[0],
                name=row[1],
                category=row[2],
                quantity=row[3],
                price=float(row[4]),
                description=row[5],
                created_at=row[6],
                updated_at=row[7]
            )
            products.append(product)
        
        logger.info(f"Retrieved {len(products)} products")
        return products
        
    except Exception as e:
        logger.error(f"Error fetching products: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch products: {str(e)}")


@app.get("/products/{product_id}", response_model=Product, tags=["Products"])
async def get_product(product_id: int, db: DatabaseManager = Depends(get_db)):
    """Get a specific product by ID"""
    try:
        logger.info(f"Fetching product with ID: {product_id}")
        
        query = "SELECT * FROM products WHERE id = %s"
        results = db.execute_query(query, [product_id])
        
        if not results:
            raise HTTPException(status_code=404, detail=f"Product with ID {product_id} not found")
        
        row = results[0]
        product = Product(
            id=row[0],
            name=row[1],
            category=row[2],
            quantity=row[3],
            price=float(row[4]),
            description=row[5],
            created_at=row[6],
            updated_at=row[7]
        )
        
        logger.info(f"Retrieved product: {product.name}")
        return product
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error fetching product {product_id}: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch product: {str(e)}")


@app.post("/products", response_model=Product, tags=["Products"])
async def create_product(product: ProductCreate, db: DatabaseManager = Depends(get_db)):
    """Create a new product"""
    try:
        logger.info(f"Creating new product: {product.name}")
        
        # Validate input
        if product.price <= 0:
            raise HTTPException(status_code=400, detail="Price must be greater than 0")
        
        if product.quantity < 0:
            raise HTTPException(status_code=400, detail="Quantity cannot be negative")
        
        # Insert product
        query = """
        INSERT INTO products (name, category, quantity, price, description)
        VALUES (%s, %s, %s, %s, %s)
        """
        params = [product.name, product.category, product.quantity, product.price, product.description]
        
        product_id = db.execute_insert(query, params)
        
        # Fetch the created product
        created_product = await get_product(product_id, db)
        
        logger.info(f"Created product with ID: {product_id}")
        return created_product
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error creating product: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to create product: {str(e)}")


@app.put("/products/{product_id}", response_model=Product, tags=["Products"])
async def update_product(
    product_id: int, 
    product_update: ProductUpdate, 
    db: DatabaseManager = Depends(get_db)
):
    """Update an existing product"""
    try:
        logger.info(f"Updating product with ID: {product_id}")
        
        # Check if product exists
        existing_product = await get_product(product_id, db)
        
        # Build update query dynamically
        update_fields = []
        params = []
        
        if product_update.name is not None:
            update_fields.append("name = %s")
            params.append(product_update.name)
        
        if product_update.category is not None:
            update_fields.append("category = %s")
            params.append(product_update.category)
        
        if product_update.quantity is not None:
            if product_update.quantity < 0:
                raise HTTPException(status_code=400, detail="Quantity cannot be negative")
            update_fields.append("quantity = %s")
            params.append(product_update.quantity)
        
        if product_update.price is not None:
            if product_update.price <= 0:
                raise HTTPException(status_code=400, detail="Price must be greater than 0")
            update_fields.append("price = %s")
            params.append(product_update.price)
        
        if product_update.description is not None:
            update_fields.append("description = %s")
            params.append(product_update.description)
        
        if not update_fields:
            raise HTTPException(status_code=400, detail="No fields to update")
        
        # Execute update
        query = f"UPDATE products SET {', '.join(update_fields)} WHERE id = %s"
        params.append(product_id)
        
        db.execute_update(query, params)
        
        # Return updated product
        updated_product = await get_product(product_id, db)
        
        logger.info(f"Updated product: {updated_product.name}")
        return updated_product
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error updating product {product_id}: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to update product: {str(e)}")


@app.delete("/products/{product_id}", tags=["Products"])
async def delete_product(product_id: int, db: DatabaseManager = Depends(get_db)):
    """Delete a product"""
    try:
        logger.info(f"Deleting product with ID: {product_id}")
        
        # Check if product exists
        existing_product = await get_product(product_id, db)
        
        # Delete product
        query = "DELETE FROM products WHERE id = %s"
        rows_affected = db.execute_update(query, [product_id])
        
        if rows_affected == 0:
            raise HTTPException(status_code=404, detail=f"Product with ID {product_id} not found")
        
        logger.info(f"Deleted product: {existing_product.name}")
        return {"message": f"Product '{existing_product.name}' deleted successfully"}
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error deleting product {product_id}: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to delete product: {str(e)}")


# Dashboard Endpoints

@app.get("/dashboard/stats", response_model=DashboardStats, tags=["Dashboard"])
async def get_dashboard_stats(db: DatabaseManager = Depends(get_db)):
    """Get dashboard statistics"""
    try:
        logger.info("Fetching dashboard statistics")
        
        # Total products
        total_query = "SELECT COUNT(*) FROM products"
        total_result = db.execute_query(total_query)
        total_products = total_result[0][0] if total_result else 0
        
        # Total categories
        categories_query = "SELECT COUNT(DISTINCT category) FROM products"
        categories_result = db.execute_query(categories_query)
        total_categories = categories_result[0][0] if categories_result else 0
        
        # Low stock products (quantity < 10)
        low_stock_query = "SELECT COUNT(*) FROM products WHERE quantity < 10"
        low_stock_result = db.execute_query(low_stock_query)
        low_stock_count = low_stock_result[0][0] if low_stock_result else 0
        
        # Total inventory value
        value_query = "SELECT SUM(quantity * price) FROM products"
        value_result = db.execute_query(value_query)
        total_value = float(value_result[0][0]) if value_result and value_result[0][0] else 0.0
        
        # Recent products (last 7 days)
        recent_query = "SELECT COUNT(*) FROM products WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)"
        recent_result = db.execute_query(recent_query)
        recent_products = recent_result[0][0] if recent_result else 0
        
        # Top categories
        top_categories_query = """
        SELECT category, COUNT(*) as count 
        FROM products 
        GROUP BY category 
        ORDER BY count DESC 
        LIMIT 5
        """
        top_categories_result = db.execute_query(top_categories_query)
        top_categories = [{"name": row[0], "count": row[1]} for row in top_categories_result]
        
        stats = DashboardStats(
            total_products=total_products,
            total_categories=total_categories,
            low_stock_count=low_stock_count,
            total_inventory_value=total_value,
            recent_products=recent_products,
            top_categories=top_categories
        )
        
        logger.info("Dashboard statistics retrieved successfully")
        return stats
        
    except Exception as e:
        logger.error(f"Error fetching dashboard stats: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch dashboard statistics: {str(e)}")


@app.get("/categories", tags=["Categories"])
async def get_categories(db: DatabaseManager = Depends(get_db)):
    """Get all unique product categories"""
    try:
        logger.info("Fetching product categories")
        
        query = "SELECT DISTINCT category FROM products ORDER BY category"
        results = db.execute_query(query)
        
        categories = [row[0] for row in results]
        
        logger.info(f"Retrieved {len(categories)} categories")
        return {"categories": categories}
        
    except Exception as e:
        logger.error(f"Error fetching categories: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch categories: {str(e)}")


# Document/PDF Generation Endpoints

@app.get("/documents/products/full", tags=["Documents"])
async def download_full_product_list(db: DatabaseManager = Depends(get_db)):
    """Download complete product list as PDF"""
    try:
        logger.info("Generating full product list PDF")
        
        # Get all products
        query = "SELECT * FROM products ORDER BY name"
        results = db.execute_query(query)
        
        products = []
        for row in results:
            product = Product(
                id=row[0],
                name=row[1],
                category=row[2],
                quantity=row[3],
                price=float(row[4]),
                description=row[5],
                created_at=row[6],
                updated_at=row[7]
            )
            products.append(product)
        
        # Generate PDF
        pdf_generator = PDFGenerator()
        pdf_bytes = pdf_generator.generate_full_product_list(products)
        
        # Return as downloadable file
        from datetime import datetime
        filename = f"product_list_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pdf"
        
        logger.info(f"Generated full product list PDF with {len(products)} products")
        
        return StreamingResponse(
            iter([pdf_bytes]),
            media_type="application/pdf",
            headers={
                "Content-Disposition": f"attachment; filename={filename}",
                "Content-Length": str(len(pdf_bytes))
            }
        )
        
    except Exception as e:
        logger.error(f"Error generating full product list PDF: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to generate PDF: {str(e)}")


@app.get("/documents/products/low-stock", tags=["Documents"])
async def download_low_stock_report(db: DatabaseManager = Depends(get_db)):
    """Download low stock products report as PDF"""
    try:
        logger.info("Generating low stock report PDF")
        
        # Get all products (filter will be done in PDF generator)
        query = "SELECT * FROM products ORDER BY quantity ASC, name"
        results = db.execute_query(query)
        
        products = []
        for row in results:
            product = Product(
                id=row[0],
                name=row[1],
                category=row[2],
                quantity=row[3],
                price=float(row[4]),
                description=row[5],
                created_at=row[6],
                updated_at=row[7]
            )
            products.append(product)
        
        # Generate PDF
        pdf_generator = PDFGenerator()
        pdf_bytes = pdf_generator.generate_low_stock_report(products)
        
        # Return as downloadable file
        from datetime import datetime
        filename = f"low_stock_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.pdf"
        
        low_stock_count = sum(1 for p in products if p.quantity < 10)
        logger.info(f"Generated low stock report PDF with {low_stock_count} low stock items")
        
        return StreamingResponse(
            iter([pdf_bytes]),
            media_type="application/pdf",
            headers={
                "Content-Disposition": f"attachment; filename={filename}",
                "Content-Length": str(len(pdf_bytes))
            }
        )
        
    except Exception as e:
        logger.error(f"Error generating low stock report PDF: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to generate PDF: {str(e)}")


if __name__ == "__main__":
    # Get configuration from environment
    host = os.getenv("BACKEND_HOST", "localhost")
    port = int(os.getenv("BACKEND_PORT", 8000))
    
    logger.info(f"Starting server on {host}:{port}")
    
    uvicorn.run(
        "main:app",
        host=host,
        port=port,
        reload=True,
        log_level="info"
    )
