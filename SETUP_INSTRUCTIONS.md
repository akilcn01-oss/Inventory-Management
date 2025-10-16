# Inventory Management System - Setup Instructions

This guide will walk you through setting up the complete Inventory Management System with JavaFX frontend, Python FastAPI backend, and MySQL database.

## 📋 Prerequisites

Before you begin, ensure you have the following installed on your system:

### Required Software
- **Java 11 or higher** - [Download from Oracle](https://www.oracle.com/java/technologies/downloads/)
- **Python 3.8 or higher** - [Download from Python.org](https://www.python.org/downloads/)
- **MySQL 8.0 or higher** - [Download from MySQL](https://dev.mysql.com/downloads/mysql/)
- **Maven 3.6 or higher** - [Download from Apache Maven](https://maven.apache.org/download.cgi)
- **Git** (optional) - [Download from Git](https://git-scm.com/downloads)

### Verify Installations
Open a terminal/command prompt and verify installations:

```bash
java -version
python --version
mysql --version
mvn --version
```

## 🗄️ Database Setup

### Step 1: Start MySQL Server
Ensure your MySQL server is running. On Windows, you can start it from Services or MySQL Workbench.

### Step 2: Create Database and User
1. Open MySQL command line or MySQL Workbench
2. Run the following commands:

```sql
-- Create database
CREATE DATABASE inventory_db;

-- Create user (optional, you can use root)
CREATE USER 'inventory_user'@'localhost' IDENTIFIED BY 'inventory_password';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'inventory_user'@'localhost';
FLUSH PRIVILEGES;
```

### Step 3: Create Tables and Sample Data
Navigate to the project directory and run:

```bash
# Create tables
mysql -u root -p inventory_db < database/schema.sql

# Insert sample data
mysql -u root -p inventory_db < database/sample_data.sql
```

Or if using the created user:
```bash
mysql -u inventory_user -p inventory_db < database/schema.sql
mysql -u inventory_user -p inventory_db < database/sample_data.sql
```

## 🐍 Backend Setup (Python FastAPI)

### Step 1: Navigate to Backend Directory
```bash
cd backend
```

### Step 2: Create Virtual Environment (Recommended)
```bash
# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows:
venv\Scripts\activate
# On macOS/Linux:
source venv/bin/activate
```

### Step 3: Install Dependencies
```bash
pip install -r requirements.txt
```

### Step 4: Configure Environment Variables
1. Copy the `.env` file from the project root to the backend directory
2. Edit the `.env` file with your database credentials:

```env
# Backend Configuration
BACKEND_PORT=8000
BACKEND_HOST=localhost

# Database Configuration
DATABASE_HOST=localhost
DATABASE_PORT=3306
DATABASE_USER=root
DATABASE_PASSWORD=yourpassword
DATABASE_NAME=inventory_db

# API Configuration
API_BASE_URL=http://localhost:8000

# Security
SECRET_KEY=your-secret-key-here

# Logging
LOG_LEVEL=INFO
```

### Step 5: Test Backend
```bash
# Run the backend server
python main.py
```

The backend should start on `http://localhost:8000`. You can test it by visiting:
- `http://localhost:8000` - Root endpoint
- `http://localhost:8000/docs` - Interactive API documentation
- `http://localhost:8000/health` - Health check

### Step 6: Run Backend Tests (Optional)
```bash
# Run unit tests
python -m pytest test_api.py -v

# Or run with coverage
pip install pytest-cov
python -m pytest test_api.py --cov=. --cov-report=html
```

## ☕ Frontend Setup (JavaFX)

### Step 1: Navigate to Frontend Directory
```bash
cd frontend
```

### Step 2: Verify Maven Configuration
Ensure your `JAVA_HOME` environment variable is set correctly:

```bash
# Windows
echo %JAVA_HOME%

# macOS/Linux
echo $JAVA_HOME
```

If not set, add it to your system environment variables pointing to your Java installation directory.

### Step 3: Install Dependencies and Compile
```bash
# Clean and compile the project
mvn clean compile

# Download dependencies
mvn dependency:resolve
```

### Step 4: Configure Application Settings
The application will create an `application.properties` file on first run. You can also create it manually in the frontend directory:

```properties
# API Configuration
api.base.url=http://localhost:8000
api.timeout=30000

# UI Configuration
ui.items.per.page=20
ui.theme=light

# Inventory Configuration
inventory.low.stock.threshold=10
```

### Step 5: Run the Frontend Application
```bash
# Run the JavaFX application
mvn javafx:run
```

Alternatively, you can create an executable JAR:
```bash
# Create executable JAR
mvn clean package

# Run the JAR (from target directory)
java -jar target/inventory-management-frontend-1.0.0.jar
```

## 🚀 Running the Complete System

### Step 1: Start Backend Server
In the backend directory:
```bash
# Activate virtual environment (if using)
venv\Scripts\activate  # Windows
source venv/bin/activate  # macOS/Linux

# Start the server
python main.py
```

### Step 2: Start Frontend Application
In a new terminal, navigate to the frontend directory:
```bash
cd frontend
mvn javafx:run
```

### Step 3: Verify Everything Works
1. The JavaFX application should open
2. Check the status bar shows "Connected to server"
3. Navigate to Dashboard to see statistics
4. Try adding, editing, and deleting products

## 🔧 Troubleshooting

### Common Issues and Solutions

#### Backend Issues

**Issue: Database connection failed**
- Verify MySQL server is running
- Check database credentials in `.env` file
- Ensure database `inventory_db` exists
- Test connection: `mysql -u root -p inventory_db`

**Issue: Port 8000 already in use**
- Change `BACKEND_PORT` in `.env` file
- Update `api.base.url` in frontend configuration
- Or kill the process using port 8000

**Issue: Module not found errors**
- Ensure virtual environment is activated
- Reinstall dependencies: `pip install -r requirements.txt`
- Check Python version compatibility

#### Frontend Issues

**Issue: JavaFX runtime components missing**
- Ensure you're using Java 11 or higher
- JavaFX is included in the Maven dependencies
- Try: `mvn clean install` then `mvn javafx:run`

**Issue: Cannot connect to backend**
- Verify backend is running on correct port
- Check firewall settings
- Ensure `api.base.url` is correct in settings

**Issue: Maven build fails**
- Verify `JAVA_HOME` is set correctly
- Check Maven version: `mvn --version`
- Clear Maven cache: `mvn dependency:purge-local-repository`

#### Database Issues

**Issue: Access denied for user**
- Check username and password in `.env`
- Verify user has proper permissions
- Try connecting manually: `mysql -u username -p`

**Issue: Table doesn't exist**
- Run schema creation: `mysql -u root -p inventory_db < database/schema.sql`
- Check if database was created: `SHOW DATABASES;`
- Check tables: `USE inventory_db; SHOW TABLES;`

## 📁 Project Structure

```
inventory-management/
├── backend/                 # Python FastAPI backend
│   ├── main.py             # Main application file
│   ├── models.py           # Pydantic models
│   ├── database.py         # Database manager
│   ├── utils.py            # Utility functions
│   ├── test_api.py         # API tests
│   └── requirements.txt    # Python dependencies
├── frontend/               # JavaFX frontend
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # FXML and CSS files
│   └── pom.xml            # Maven configuration
├── database/              # Database scripts
│   ├── schema.sql         # Database schema
│   └── sample_data.sql    # Sample data
├── .env                   # Environment configuration
├── requirements.txt       # Python dependencies
└── README.md             # Project documentation
```

## 🎯 Features Overview

### Dashboard
- Total products, categories, and inventory value
- Low stock alerts and statistics
- Interactive charts showing category distribution
- Quick action buttons

### Product Management
- Add, edit, and delete products
- Search and filter functionality
- Pagination for large datasets
- Real-time validation

### Settings
- API configuration
- UI preferences
- Inventory settings
- About information

## 🔒 Security Notes

- Change default passwords in production
- Use environment variables for sensitive data
- Enable HTTPS in production
- Implement proper authentication if needed

## 📈 Performance Tips

- Use connection pooling for database (already implemented)
- Enable caching for frequently accessed data
- Optimize database queries with indexes
- Use async operations for better UI responsiveness

## 🆘 Getting Help

If you encounter issues:

1. Check the logs in `backend/logs/inventory_api.log`
2. Verify all prerequisites are installed correctly
3. Ensure all services are running
4. Check network connectivity between components

## 🎉 Success!

If everything is set up correctly, you should have:
- ✅ MySQL database with sample data
- ✅ Python FastAPI backend running on port 8000
- ✅ JavaFX frontend connected to the backend
- ✅ Full CRUD operations working
- ✅ Dashboard showing statistics and charts

Congratulations! Your Inventory Management System is now ready to use.
