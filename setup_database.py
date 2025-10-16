"""
Database Setup Script for Inventory Management System
Automatically creates the database, tables, and loads sample data
"""

import mysql.connector
from mysql.connector import Error
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

def read_sql_file(file_path):
    """Read SQL file and return its contents"""
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            return file.read()
    except Exception as e:
        print(f"[ERROR] Error reading file {file_path}: {e}")
        return None

def execute_sql_script(cursor, sql_script):
    """Execute SQL script with multiple statements"""
    # Split by semicolon and execute each statement
    statements = sql_script.split(';')
    
    for statement in statements:
        statement = statement.strip()
        if statement:  # Skip empty statements
            try:
                cursor.execute(statement)
                print(f"[OK] Executed: {statement[:50]}..." if len(statement) > 50 else f"[OK] Executed: {statement}")
            except Error as e:
                # Skip errors for statements that might already exist
                if "already exists" not in str(e).lower():
                    print(f"[WARNING] {e}")

def setup_database():
    """Main function to set up the database"""
    print("=" * 60)
    print("INVENTORY MANAGEMENT SYSTEM - DATABASE SETUP")
    print("=" * 60)
    print()
    
    # Get database configuration from environment
    db_host = os.getenv('DATABASE_HOST', 'localhost')
    db_port = int(os.getenv('DATABASE_PORT', 3306))
    db_user = os.getenv('DATABASE_USER', 'root')
    db_password = os.getenv('DATABASE_PASSWORD', '')
    db_name = os.getenv('DATABASE_NAME', 'inventory_db')
    
    print("Configuration:")
    print(f"   Host: {db_host}")
    print(f"   Port: {db_port}")
    print(f"   User: {db_user}")
    print(f"   Database: {db_name}")
    print()
    
    connection = None
    
    try:
        # Step 1: Connect to MySQL server (without specifying database)
        print("[Step 1] Connecting to MySQL server...")
        connection = mysql.connector.connect(
            host=db_host,
            port=db_port,
            user=db_user,
            password=db_password
        )
        
        if connection.is_connected():
            print("[SUCCESS] Connected to MySQL server successfully!")
            cursor = connection.cursor()
            
            # Step 2: Create database
            print(f"\n[Step 2] Creating database '{db_name}'...")
            cursor.execute(f"CREATE DATABASE IF NOT EXISTS {db_name}")
            print(f"[SUCCESS] Database '{db_name}' created successfully!")
            
            # Step 3: Use the database
            print(f"\n[Step 3] Switching to database '{db_name}'...")
            cursor.execute(f"USE {db_name}")
            print(f"[SUCCESS] Now using database '{db_name}'")
            
            # Step 4: Create tables from schema.sql
            print("\n[Step 4] Creating tables from schema.sql...")
            schema_file = os.path.join('database', 'schema.sql')
            
            if os.path.exists(schema_file):
                schema_sql = read_sql_file(schema_file)
                if schema_sql:
                    # Remove CREATE DATABASE and USE statements from schema
                    schema_sql = '\n'.join([
                        line for line in schema_sql.split('\n')
                        if not line.strip().upper().startswith(('CREATE DATABASE', 'USE '))
                    ])
                    execute_sql_script(cursor, schema_sql)
                    connection.commit()
                    print("[SUCCESS] Tables created successfully!")
                else:
                    print("[ERROR] Failed to read schema file")
                    return False
            else:
                print(f"[ERROR] Schema file not found: {schema_file}")
                return False
            
            # Step 5: Load sample data
            print("\n[Step 5] Loading sample data from sample_data.sql...")
            sample_data_file = os.path.join('database', 'sample_data.sql')
            
            if os.path.exists(sample_data_file):
                sample_data_sql = read_sql_file(sample_data_file)
                if sample_data_sql:
                    # Remove USE statements from sample data
                    sample_data_sql = '\n'.join([
                        line for line in sample_data_sql.split('\n')
                        if not line.strip().upper().startswith('USE ')
                    ])
                    execute_sql_script(cursor, sample_data_sql)
                    connection.commit()
                    print("[SUCCESS] Sample data loaded successfully!")
                else:
                    print("[ERROR] Failed to read sample data file")
                    return False
            else:
                print(f"[ERROR] Sample data file not found: {sample_data_file}")
                return False
            
            # Step 6: Verify setup
            print("\n[Step 6] Verifying database setup...")
            
            # Check tables
            cursor.execute("SHOW TABLES")
            tables = cursor.fetchall()
            print(f"[SUCCESS] Found {len(tables)} tables:")
            for table in tables:
                print(f"   - {table[0]}")
            
            # Check product count
            cursor.execute("SELECT COUNT(*) FROM products")
            product_count = cursor.fetchone()[0]
            print(f"\n[SUCCESS] Found {product_count} products in the database")
            
            # Check categories
            cursor.execute("SELECT COUNT(DISTINCT category) FROM products")
            category_count = cursor.fetchone()[0]
            print(f"[SUCCESS] Found {category_count} unique categories")
            
            cursor.close()
            
            print("\n" + "=" * 60)
            print("DATABASE SETUP COMPLETED SUCCESSFULLY!")
            print("=" * 60)
            print("\n[SUCCESS] Your database is ready to use!")
            print(f"[SUCCESS] Database: {db_name}")
            print(f"[SUCCESS] Products: {product_count}")
            print(f"[SUCCESS] Categories: {category_count}")
            print("\n[NEXT] You can now start the backend server with: python backend/main.py")
            print("=" * 60)
            
            return True
            
    except Error as e:
        print(f"\n[ERROR] {e}")
        print("\nTroubleshooting tips:")
        print("   1. Make sure MySQL server is running")
        print("   2. Check your database credentials in the .env file")
        print("   3. Verify you have permission to create databases")
        print("   4. Try connecting to MySQL manually: mysql -u root -p")
        return False
        
    finally:
        if connection and connection.is_connected():
            connection.close()
            print("\nDatabase connection closed")

if __name__ == "__main__":
    try:
        success = setup_database()
        if success:
            print("\n[SUCCESS] Setup completed successfully!")
            exit(0)
        else:
            print("\n[ERROR] Setup failed. Please check the errors above.")
            exit(1)
    except KeyboardInterrupt:
        print("\n\n[WARNING] Setup interrupted by user")
        exit(1)
    except Exception as e:
        print(f"\n[ERROR] Unexpected error: {e}")
        exit(1)
