-- Run as a PostgreSQL administrator using psql:
-- psql -U postgres -f database/setup.sql
-- This is a one-time alternative to Docker Compose. Use a fresh database/role.
CREATE ROLE employees WITH LOGIN PASSWORD 'employees';
CREATE DATABASE employees OWNER employees;
-- Flyway creates and versions application tables automatically on startup.
