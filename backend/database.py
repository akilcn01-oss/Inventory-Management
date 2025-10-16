"""
Database manager for MySQL operations
Handles connection, queries, and transactions for the inventory system
"""

import logging
import os
from typing import List, Tuple, Any, Optional
import mysql.connector
from mysql.connector import Error, pooling
from contextlib import contextmanager
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

logger = logging.getLogger(__name__)


class DatabaseManager:
    """
    Database manager class for handling MySQL operations
    Provides connection pooling, query execution, and transaction management
    """
    
    def __init__(self):
        """Initialize database manager with configuration"""
        self.config = {
            'host': os.getenv('DATABASE_HOST', 'localhost'),
            'port': int(os.getenv('DATABASE_PORT', 3306)),
            'user': os.getenv('DATABASE_USER', 'root'),
            'password': os.getenv('DATABASE_PASSWORD', ''),
            'database': os.getenv('DATABASE_NAME', 'inventory_db'),
            'charset': 'utf8mb4',
            'collation': 'utf8mb4_unicode_ci',
            'autocommit': True,
            'raise_on_warnings': True
        }
        
        # Connection pool configuration
        self.pool_config = {
            **self.config,
            'pool_name': 'inventory_pool',
            'pool_size': 10,
            'pool_reset_session': True
        }
        
        self.connection_pool = None
        self.connection = None
    
    def connect(self):
        """Establish database connection with connection pooling"""
        try:
            logger.info("Establishing database connection...")
            
            # Create connection pool
            self.connection_pool = pooling.MySQLConnectionPool(**self.pool_config)
            
            # Test connection
            test_connection = self.connection_pool.get_connection()
            if test_connection.is_connected():
                logger.info(f"Successfully connected to MySQL database: {self.config['database']}")
                test_connection.close()
            
        except Error as e:
            logger.error(f"Error connecting to MySQL database: {e}")
            raise Exception(f"Database connection failed: {e}")
    
    def disconnect(self):
        """Close database connection"""
        try:
            if self.connection and self.connection.is_connected():
                self.connection.close()
                logger.info("Database connection closed")
        except Error as e:
            logger.error(f"Error closing database connection: {e}")
    
    @contextmanager
    def get_connection(self):
        """Context manager for database connections"""
        connection = None
        try:
            connection = self.connection_pool.get_connection()
            yield connection
        except Error as e:
            logger.error(f"Database connection error: {e}")
            raise
        finally:
            if connection and connection.is_connected():
                connection.close()
    
    def execute_query(self, query: str, params: Optional[List[Any]] = None) -> List[Tuple]:
        """
        Execute a SELECT query and return results
        
        Args:
            query: SQL query string
            params: Query parameters
            
        Returns:
            List of tuples containing query results
        """
        try:
            with self.get_connection() as connection:
                cursor = connection.cursor()
                
                if params:
                    cursor.execute(query, params)
                else:
                    cursor.execute(query)
                
                results = cursor.fetchall()
                cursor.close()
                
                logger.debug(f"Query executed successfully: {query[:100]}...")
                return results
                
        except Error as e:
            logger.error(f"Error executing query: {e}")
            logger.error(f"Query: {query}")
            logger.error(f"Params: {params}")
            raise Exception(f"Query execution failed: {e}")
    
    def execute_insert(self, query: str, params: Optional[List[Any]] = None) -> int:
        """
        Execute an INSERT query and return the last inserted ID
        
        Args:
            query: SQL INSERT query string
            params: Query parameters
            
        Returns:
            Last inserted row ID
        """
        try:
            with self.get_connection() as connection:
                cursor = connection.cursor()
                
                if params:
                    cursor.execute(query, params)
                else:
                    cursor.execute(query)
                
                last_id = cursor.lastrowid
                connection.commit()
                cursor.close()
                
                logger.debug(f"Insert executed successfully, ID: {last_id}")
                return last_id
                
        except Error as e:
            logger.error(f"Error executing insert: {e}")
            logger.error(f"Query: {query}")
            logger.error(f"Params: {params}")
            raise Exception(f"Insert execution failed: {e}")
    
    def execute_update(self, query: str, params: Optional[List[Any]] = None) -> int:
        """
        Execute an UPDATE or DELETE query and return affected rows count
        
        Args:
            query: SQL UPDATE/DELETE query string
            params: Query parameters
            
        Returns:
            Number of affected rows
        """
        try:
            with self.get_connection() as connection:
                cursor = connection.cursor()
                
                if params:
                    cursor.execute(query, params)
                else:
                    cursor.execute(query)
                
                affected_rows = cursor.rowcount
                connection.commit()
                cursor.close()
                
                logger.debug(f"Update executed successfully, affected rows: {affected_rows}")
                return affected_rows
                
        except Error as e:
            logger.error(f"Error executing update: {e}")
            logger.error(f"Query: {query}")
            logger.error(f"Params: {params}")
            raise Exception(f"Update execution failed: {e}")
    
    def execute_transaction(self, queries: List[Tuple[str, Optional[List[Any]]]]) -> bool:
        """
        Execute multiple queries in a transaction
        
        Args:
            queries: List of tuples containing (query, params)
            
        Returns:
            True if transaction successful, False otherwise
        """
        try:
            with self.get_connection() as connection:
                connection.autocommit = False
                cursor = connection.cursor()
                
                try:
                    for query, params in queries:
                        if params:
                            cursor.execute(query, params)
                        else:
                            cursor.execute(query)
                    
                    connection.commit()
                    cursor.close()
                    
                    logger.debug(f"Transaction executed successfully with {len(queries)} queries")
                    return True
                    
                except Error as e:
                    connection.rollback()
                    cursor.close()
                    logger.error(f"Transaction failed, rolled back: {e}")
                    raise
                    
        except Error as e:
            logger.error(f"Error executing transaction: {e}")
            raise Exception(f"Transaction execution failed: {e}")
    
    def test_connection(self) -> bool:
        """
        Test database connection
        
        Returns:
            True if connection is successful, False otherwise
        """
        try:
            with self.get_connection() as connection:
                cursor = connection.cursor()
                cursor.execute("SELECT 1")
                result = cursor.fetchone()
                cursor.close()
                
                return result is not None
                
        except Error as e:
            logger.error(f"Connection test failed: {e}")
            return False
    
    def get_table_info(self, table_name: str) -> List[Tuple]:
        """
        Get information about a table structure
        
        Args:
            table_name: Name of the table
            
        Returns:
            List of tuples containing column information
        """
        try:
            query = f"DESCRIBE {table_name}"
            return self.execute_query(query)
            
        except Exception as e:
            logger.error(f"Error getting table info for {table_name}: {e}")
            raise
    
    def get_database_stats(self) -> dict:
        """
        Get database statistics
        
        Returns:
            Dictionary containing database statistics
        """
        try:
            stats = {}
            
            # Get table count
            tables_query = "SHOW TABLES"
            tables = self.execute_query(tables_query)
            stats['table_count'] = len(tables)
            
            # Get products table stats
            products_count_query = "SELECT COUNT(*) FROM products"
            products_result = self.execute_query(products_count_query)
            stats['products_count'] = products_result[0][0] if products_result else 0
            
            # Get database size
            size_query = """
            SELECT 
                ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'DB Size in MB'
            FROM information_schema.tables 
            WHERE table_schema = %s
            """
            size_result = self.execute_query(size_query, [self.config['database']])
            stats['database_size_mb'] = float(size_result[0][0]) if size_result and size_result[0][0] else 0.0
            
            return stats
            
        except Exception as e:
            logger.error(f"Error getting database stats: {e}")
            raise
    
    def create_backup_query(self, table_name: str) -> str:
        """
        Generate a backup query for a table
        
        Args:
            table_name: Name of the table to backup
            
        Returns:
            SQL query string for creating backup
        """
        return f"CREATE TABLE {table_name}_backup AS SELECT * FROM {table_name}"
    
    def validate_schema(self) -> bool:
        """
        Validate that required tables exist
        
        Returns:
            True if schema is valid, False otherwise
        """
        try:
            required_tables = ['products', 'categories', 'product_audit']
            
            tables_query = "SHOW TABLES"
            existing_tables = self.execute_query(tables_query)
            existing_table_names = [table[0] for table in existing_tables]
            
            for table in required_tables:
                if table not in existing_table_names:
                    logger.error(f"Required table '{table}' not found in database")
                    return False
            
            logger.info("Database schema validation successful")
            return True
            
        except Exception as e:
            logger.error(f"Schema validation failed: {e}")
            return False
