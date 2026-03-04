CREATE TABLE top_sales_per_city (
    city VARCHAR(255) PRIMARY KEY,
    salesman_name VARCHAR(255) NOT NULL,
    sale_value DECIMAL(10, 2) NOT NULL,
    window_end TIMESTAMP NOT NULL
);

CREATE TABLE top_salesman (
    salesman_name VARCHAR(255) PRIMARY KEY,
    total_sales DECIMAL(10, 2) NOT NULL,
    window_end TIMESTAMP NOT NULL
);
