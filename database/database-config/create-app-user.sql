-- Run as a MySQL administrator:  mysql -u root -p < database/database-config/create-app-user.sql
-- CHANGE THE PASSWORD BELOW before running. Never commit a real password.
CREATE DATABASE IF NOT EXISTS lifelink CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lifelink_app'@'localhost' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON lifelink.* TO 'lifelink_app'@'localhost';
FLUSH PRIVILEGES;
