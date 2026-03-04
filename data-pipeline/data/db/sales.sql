CREATE TABLE sales (
    sale_id SERIAL PRIMARY KEY,
    salesman_id INT NOT NULL,
    salesman_name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    sale_value DECIMAL(10, 2) NOT NULL,
    sale_date TIMESTAMP NOT NULL
);

INSERT INTO sales (salesman_id, salesman_name, city, sale_value, sale_date) VALUES
(1, 'John Doe', 'New York', 100.00, '2024-01-05 10:00:00'),
(2, 'Jane Smith', 'New York', 150.00, '2024-01-05 11:00:00'),
(1, 'John Doe', 'New York', 200.00, '2024-01-06 10:00:00'),
(3, 'Peter Jones', 'London', 120.00, '2024-01-05 10:30:00'),
(3, 'Peter Jones', 'London', 180.00, '2024-01-06 11:30:00'),
(2, 'Jane Smith', 'New York', 50.00, '2024-01-07 12:00:00'),
(1, 'John Doe', 'London', 300.00, '2024-01-08 13:00:00');
