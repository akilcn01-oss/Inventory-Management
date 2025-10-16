"""
Pydantic models for the Inventory Management System
Defines data structures for API requests and responses
"""

from datetime import datetime
from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field, validator


class ProductBase(BaseModel):
    """Base product model with common fields"""
    name: str = Field(..., min_length=1, max_length=255, description="Product name")
    category: str = Field(..., min_length=1, max_length=100, description="Product category")
    quantity: int = Field(..., ge=0, description="Product quantity in stock")
    price: float = Field(..., gt=0, description="Product price")
    description: Optional[str] = Field(None, max_length=1000, description="Product description")

    @validator('name')
    def validate_name(cls, v):
        if not v.strip():
            raise ValueError('Product name cannot be empty')
        return v.strip()

    @validator('category')
    def validate_category(cls, v):
        if not v.strip():
            raise ValueError('Category cannot be empty')
        return v.strip()

    @validator('price')
    def validate_price(cls, v):
        if v <= 0:
            raise ValueError('Price must be greater than 0')
        return round(v, 2)

    @validator('quantity')
    def validate_quantity(cls, v):
        if v < 0:
            raise ValueError('Quantity cannot be negative')
        return v


class ProductCreate(ProductBase):
    """Model for creating a new product"""
    pass


class ProductUpdate(BaseModel):
    """Model for updating an existing product (all fields optional)"""
    name: Optional[str] = Field(None, min_length=1, max_length=255)
    category: Optional[str] = Field(None, min_length=1, max_length=100)
    quantity: Optional[int] = Field(None, ge=0)
    price: Optional[float] = Field(None, gt=0)
    description: Optional[str] = Field(None, max_length=1000)

    @validator('name')
    def validate_name(cls, v):
        if v is not None and not v.strip():
            raise ValueError('Product name cannot be empty')
        return v.strip() if v else v

    @validator('category')
    def validate_category(cls, v):
        if v is not None and not v.strip():
            raise ValueError('Category cannot be empty')
        return v.strip() if v else v

    @validator('price')
    def validate_price(cls, v):
        if v is not None and v <= 0:
            raise ValueError('Price must be greater than 0')
        return round(v, 2) if v is not None else v

    @validator('quantity')
    def validate_quantity(cls, v):
        if v is not None and v < 0:
            raise ValueError('Quantity cannot be negative')
        return v


class Product(ProductBase):
    """Complete product model with database fields"""
    id: int = Field(..., description="Product ID")
    created_at: datetime = Field(..., description="Creation timestamp")
    updated_at: datetime = Field(..., description="Last update timestamp")

    class Config:
        from_attributes = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }


class ProductResponse(BaseModel):
    """Response model for product operations"""
    success: bool
    message: str
    data: Optional[Product] = None


class ProductListResponse(BaseModel):
    """Response model for product list operations"""
    success: bool
    message: str
    data: List[Product]
    total: int
    page: int
    limit: int


class CategoryStats(BaseModel):
    """Model for category statistics"""
    name: str
    count: int


class DashboardStats(BaseModel):
    """Model for dashboard statistics"""
    total_products: int = Field(..., description="Total number of products")
    total_categories: int = Field(..., description="Total number of categories")
    low_stock_count: int = Field(..., description="Number of products with low stock")
    total_inventory_value: float = Field(..., description="Total value of inventory")
    recent_products: int = Field(..., description="Products added in last 7 days")
    top_categories: List[CategoryStats] = Field(..., description="Top 5 categories by product count")


class ErrorResponse(BaseModel):
    """Standard error response model"""
    success: bool = False
    message: str
    error_code: Optional[str] = None
    details: Optional[Dict[str, Any]] = None


class SuccessResponse(BaseModel):
    """Standard success response model"""
    success: bool = True
    message: str
    data: Optional[Dict[str, Any]] = None


# Search and filter models
class ProductFilter(BaseModel):
    """Model for product filtering parameters"""
    category: Optional[str] = None
    min_price: Optional[float] = Field(None, ge=0)
    max_price: Optional[float] = Field(None, ge=0)
    min_quantity: Optional[int] = Field(None, ge=0)
    max_quantity: Optional[int] = Field(None, ge=0)
    search: Optional[str] = None
    low_stock: Optional[bool] = None

    @validator('max_price')
    def validate_price_range(cls, v, values):
        if v is not None and 'min_price' in values and values['min_price'] is not None:
            if v < values['min_price']:
                raise ValueError('max_price must be greater than or equal to min_price')
        return v

    @validator('max_quantity')
    def validate_quantity_range(cls, v, values):
        if v is not None and 'min_quantity' in values and values['min_quantity'] is not None:
            if v < values['min_quantity']:
                raise ValueError('max_quantity must be greater than or equal to min_quantity')
        return v


class PaginationParams(BaseModel):
    """Model for pagination parameters"""
    page: int = Field(1, ge=1, description="Page number")
    limit: int = Field(10, ge=1, le=100, description="Items per page")
    
    @property
    def skip(self) -> int:
        """Calculate skip value for database queries"""
        return (self.page - 1) * self.limit


# Audit and logging models
class AuditLog(BaseModel):
    """Model for audit log entries"""
    id: int
    product_id: Optional[int]
    action: str
    old_values: Optional[Dict[str, Any]]
    new_values: Optional[Dict[str, Any]]
    changed_by: Optional[str]
    changed_at: datetime

    class Config:
        from_attributes = True
        json_encoders = {
            datetime: lambda v: v.isoformat()
        }
