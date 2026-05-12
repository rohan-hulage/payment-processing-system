-- Auth Service Database Schema
-- V1: Create users, roles, and user_roles tables

CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    username                 VARCHAR(64)  NOT NULL,
    email                    VARCHAR(128) NOT NULL,
    password                 VARCHAR(255) NOT NULL,
    first_name               VARCHAR(64),
    last_name                VARCHAR(64),
    enabled                  TINYINT(1)   NOT NULL DEFAULT 1,
    account_non_expired      TINYINT(1)   NOT NULL DEFAULT 1,
    account_non_locked       TINYINT(1)   NOT NULL DEFAULT 1,
    credentials_non_expired  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at               DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at               DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default roles
INSERT IGNORE INTO roles (name) VALUES ('ROLE_USER');
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');
