"""
Utility functions for the Inventory Management System
Includes logging setup, validation helpers, and common utilities
"""

import logging
import os
import sys
from datetime import datetime
from typing import Any, Dict, Optional
from dotenv import load_dotenv

# Load environment variables
load_dotenv()


def setup_logging() -> logging.Logger:
    """
    Setup logging configuration for the application
    
    Returns:
        Configured logger instance
    """
    # Get log level from environment
    log_level = os.getenv('LOG_LEVEL', 'INFO').upper()
    
    # Create logs directory if it doesn't exist
    log_dir = 'logs'
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)
    
    # Configure logging
    logging.basicConfig(
        level=getattr(logging, log_level),
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(f'{log_dir}/inventory_api.log'),
            logging.StreamHandler(sys.stdout)
        ]
    )
    
    # Create logger
    logger = logging.getLogger('inventory_api')
    logger.info(f"Logging initialized with level: {log_level}")
    
    return logger


def validate_product_data(data: Dict[str, Any]) -> Dict[str, str]:
    """
    Validate product data and return validation errors
    
    Args:
        data: Product data dictionary
        
    Returns:
        Dictionary of validation errors (empty if valid)
    """
    errors = {}
    
    # Validate name
    if not data.get('name') or not data['name'].strip():
        errors['name'] = 'Product name is required'
    elif len(data['name'].strip()) > 255:
        errors['name'] = 'Product name must be less than 255 characters'
    
    # Validate category
    if not data.get('category') or not data['category'].strip():
        errors['category'] = 'Category is required'
    elif len(data['category'].strip()) > 100:
        errors['category'] = 'Category must be less than 100 characters'
    
    # Validate quantity
    try:
        quantity = int(data.get('quantity', 0))
        if quantity < 0:
            errors['quantity'] = 'Quantity cannot be negative'
    except (ValueError, TypeError):
        errors['quantity'] = 'Quantity must be a valid integer'
    
    # Validate price
    try:
        price = float(data.get('price', 0))
        if price <= 0:
            errors['price'] = 'Price must be greater than 0'
    except (ValueError, TypeError):
        errors['price'] = 'Price must be a valid number'
    
    # Validate description length
    description = data.get('description', '')
    if description and len(description) > 1000:
        errors['description'] = 'Description must be less than 1000 characters'
    
    return errors


def sanitize_string(value: str) -> str:
    """
    Sanitize string input by removing dangerous characters
    
    Args:
        value: Input string
        
    Returns:
        Sanitized string
    """
    if not isinstance(value, str):
        return str(value)
    
    # Remove null bytes and control characters
    sanitized = value.replace('\x00', '').strip()
    
    # Limit length
    if len(sanitized) > 1000:
        sanitized = sanitized[:1000]
    
    return sanitized


def format_currency(amount: float) -> str:
    """
    Format amount as currency string
    
    Args:
        amount: Numeric amount
        
    Returns:
        Formatted currency string
    """
    return f"${amount:,.2f}"


def format_datetime(dt: datetime) -> str:
    """
    Format datetime for display
    
    Args:
        dt: Datetime object
        
    Returns:
        Formatted datetime string
    """
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def calculate_inventory_value(products: list) -> float:
    """
    Calculate total inventory value from product list
    
    Args:
        products: List of product dictionaries
        
    Returns:
        Total inventory value
    """
    total = 0.0
    for product in products:
        try:
            quantity = int(product.get('quantity', 0))
            price = float(product.get('price', 0))
            total += quantity * price
        except (ValueError, TypeError):
            continue
    
    return round(total, 2)


def is_low_stock(quantity: int, threshold: int = 10) -> bool:
    """
    Check if product is low stock
    
    Args:
        quantity: Product quantity
        threshold: Low stock threshold (default: 10)
        
    Returns:
        True if low stock, False otherwise
    """
    return quantity < threshold


def generate_product_code(name: str, category: str) -> str:
    """
    Generate a product code based on name and category
    
    Args:
        name: Product name
        category: Product category
        
    Returns:
        Generated product code
    """
    # Take first 3 characters of category and first 3 of name
    cat_code = category[:3].upper()
    name_code = ''.join(c for c in name if c.isalnum())[:3].upper()
    
    # Add timestamp for uniqueness
    timestamp = datetime.now().strftime("%m%d")
    
    return f"{cat_code}-{name_code}-{timestamp}"


def validate_pagination_params(page: int, limit: int) -> Dict[str, str]:
    """
    Validate pagination parameters
    
    Args:
        page: Page number
        limit: Items per page
        
    Returns:
        Dictionary of validation errors
    """
    errors = {}
    
    if page < 1:
        errors['page'] = 'Page number must be greater than 0'
    
    if limit < 1:
        errors['limit'] = 'Limit must be greater than 0'
    elif limit > 1000:
        errors['limit'] = 'Limit cannot exceed 1000'
    
    return errors


def build_search_query(base_query: str, filters: Dict[str, Any]) -> tuple:
    """
    Build dynamic search query with filters
    
    Args:
        base_query: Base SQL query
        filters: Dictionary of filter parameters
        
    Returns:
        Tuple of (query_string, parameters)
    """
    conditions = []
    params = []
    
    # Category filter
    if filters.get('category'):
        conditions.append("category = %s")
        params.append(filters['category'])
    
    # Price range filter
    if filters.get('min_price') is not None:
        conditions.append("price >= %s")
        params.append(filters['min_price'])
    
    if filters.get('max_price') is not None:
        conditions.append("price <= %s")
        params.append(filters['max_price'])
    
    # Quantity range filter
    if filters.get('min_quantity') is not None:
        conditions.append("quantity >= %s")
        params.append(filters['min_quantity'])
    
    if filters.get('max_quantity') is not None:
        conditions.append("quantity <= %s")
        params.append(filters['max_quantity'])
    
    # Search filter
    if filters.get('search'):
        conditions.append("(name LIKE %s OR description LIKE %s)")
        search_term = f"%{filters['search']}%"
        params.extend([search_term, search_term])
    
    # Low stock filter
    if filters.get('low_stock'):
        conditions.append("quantity < 10")
    
    # Build final query
    if conditions:
        query = f"{base_query} WHERE {' AND '.join(conditions)}"
    else:
        query = base_query
    
    return query, params


def create_response(success: bool, message: str, data: Any = None, error_code: str = None) -> Dict[str, Any]:
    """
    Create standardized API response
    
    Args:
        success: Success status
        message: Response message
        data: Response data
        error_code: Error code (if applicable)
        
    Returns:
        Standardized response dictionary
    """
    response = {
        'success': success,
        'message': message,
        'timestamp': datetime.now().isoformat()
    }
    
    if data is not None:
        response['data'] = data
    
    if error_code:
        response['error_code'] = error_code
    
    return response


def log_api_request(logger: logging.Logger, method: str, endpoint: str, params: Dict[str, Any] = None):
    """
    Log API request details
    
    Args:
        logger: Logger instance
        method: HTTP method
        endpoint: API endpoint
        params: Request parameters
    """
    log_message = f"{method} {endpoint}"
    if params:
        log_message += f" - Params: {params}"
    
    logger.info(log_message)


def handle_database_error(logger: logging.Logger, error: Exception, operation: str) -> Dict[str, Any]:
    """
    Handle database errors and create appropriate response
    
    Args:
        logger: Logger instance
        error: Database error
        operation: Operation being performed
        
    Returns:
        Error response dictionary
    """
    error_message = f"Database error during {operation}: {str(error)}"
    logger.error(error_message)
    
    return create_response(
        success=False,
        message=f"Failed to {operation}",
        error_code="DATABASE_ERROR"
    )


def get_config_value(key: str, default: Any = None, required: bool = False) -> Any:
    """
    Get configuration value from environment variables
    
    Args:
        key: Configuration key
        default: Default value if not found
        required: Whether the value is required
        
    Returns:
        Configuration value
        
    Raises:
        ValueError: If required value is not found
    """
    value = os.getenv(key, default)
    
    if required and value is None:
        raise ValueError(f"Required configuration value '{key}' not found")
    
    return value


def convert_to_int(value: Any, default: int = 0) -> int:
    """
    Safely convert value to integer
    
    Args:
        value: Value to convert
        default: Default value if conversion fails
        
    Returns:
        Integer value
    """
    try:
        return int(value)
    except (ValueError, TypeError):
        return default


def convert_to_float(value: Any, default: float = 0.0) -> float:
    """
    Safely convert value to float
    
    Args:
        value: Value to convert
        default: Default value if conversion fails
        
    Returns:
        Float value
    """
    try:
        return float(value)
    except (ValueError, TypeError):
        return default
