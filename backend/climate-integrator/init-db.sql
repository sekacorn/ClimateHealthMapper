-- Initialize Climate Health Mapper Database
-- This script runs on first database creation

-- Create database if not exists (already created by Docker)
-- CREATE DATABASE IF NOT EXISTS climate_health_db;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS climate_data;
CREATE SCHEMA IF NOT EXISTS health_data;
CREATE SCHEMA IF NOT EXISTS genomic_data;

-- Grant privileges
GRANT ALL PRIVILEGES ON SCHEMA climate_data TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA health_data TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA genomic_data TO postgres;

-- Create indexes for better performance (tables will be created by JPA)
-- Additional custom indexes can be added here

-- Create function for updating timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Climate Health Mapper database initialized successfully';
END $$;
