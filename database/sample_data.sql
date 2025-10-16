-- Sample data for testing
USE inventory_db;

-- Insert sample products
INSERT INTO products (name, category, quantity, price, description) VALUES
('iPhone 14 Pro', 'Electronics', 25, 999.99, 'Latest iPhone with advanced camera system'),
('Samsung Galaxy S23', 'Electronics', 18, 899.99, 'Flagship Android smartphone with excellent display'),
('MacBook Air M2', 'Electronics', 12, 1199.99, 'Lightweight laptop with Apple M2 chip'),
('Dell XPS 13', 'Electronics', 8, 1099.99, 'Premium ultrabook with InfinityEdge display'),
('Sony WH-1000XM4', 'Electronics', 35, 349.99, 'Noise-canceling wireless headphones'),

('Nike Air Max 270', 'Clothing', 45, 129.99, 'Comfortable running shoes with air cushioning'),
('Adidas Ultraboost 22', 'Clothing', 32, 179.99, 'High-performance running shoes'),
('Levi''s 501 Jeans', 'Clothing', 28, 89.99, 'Classic straight-fit denim jeans'),
('North Face Jacket', 'Clothing', 15, 199.99, 'Waterproof outdoor jacket'),
('Nike Dri-FIT T-Shirt', 'Clothing', 60, 24.99, 'Moisture-wicking athletic t-shirt'),

('The Great Gatsby', 'Books', 40, 12.99, 'Classic American novel by F. Scott Fitzgerald'),
('To Kill a Mockingbird', 'Books', 35, 14.99, 'Pulitzer Prize-winning novel by Harper Lee'),
('1984', 'Books', 50, 13.99, 'Dystopian novel by George Orwell'),
('Python Programming', 'Books', 22, 49.99, 'Comprehensive guide to Python programming'),
('Data Science Handbook', 'Books', 18, 59.99, 'Essential tools and techniques for data science'),

('Garden Hose 50ft', 'Home & Garden', 25, 39.99, 'Durable garden hose with spray nozzle'),
('LED Light Bulbs', 'Home & Garden', 100, 8.99, 'Energy-efficient LED bulbs, pack of 4'),
('Cordless Drill', 'Home & Garden', 12, 89.99, 'Powerful cordless drill with battery'),
('Plant Fertilizer', 'Home & Garden', 30, 19.99, 'Organic plant fertilizer for indoor plants'),
('Outdoor Chair Set', 'Home & Garden', 8, 299.99, 'Weather-resistant patio furniture set'),

('Basketball', 'Sports', 20, 29.99, 'Official size basketball for indoor/outdoor use'),
('Tennis Racket', 'Sports', 15, 149.99, 'Professional tennis racket with grip'),
('Yoga Mat', 'Sports', 40, 34.99, 'Non-slip yoga mat with carrying strap'),
('Dumbbells Set', 'Sports', 10, 199.99, 'Adjustable dumbbells set, 5-50 lbs'),
('Running Shoes', 'Sports', 25, 119.99, 'Lightweight running shoes with cushioning'),

-- Insert some low stock items for testing alerts
('Wireless Mouse', 'Electronics', 3, 29.99, 'Ergonomic wireless mouse with USB receiver'),
('Bluetooth Speaker', 'Electronics', 2, 79.99, 'Portable Bluetooth speaker with bass boost'),
('Coffee Maker', 'Home & Garden', 1, 149.99, 'Programmable coffee maker with thermal carafe'),
('Protein Powder', 'Health & Beauty', 4, 39.99, 'Whey protein powder, vanilla flavor'),
('Car Phone Mount', 'Automotive', 2, 19.99, 'Universal car phone mount with adjustable grip');
