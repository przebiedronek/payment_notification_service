-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on email for faster lookups
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);

-- Create index on name for search queries
CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);

-- Insert sample data for testing
INSERT INTO customers (id, email, name, created_at, updated_at) VALUES
    (1, 'alice.smith@example.com', 'Alice Smith', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'bob.jones@example.com', 'Bob Jones', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'charlie.brown@example.com', 'Charlie Brown', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

