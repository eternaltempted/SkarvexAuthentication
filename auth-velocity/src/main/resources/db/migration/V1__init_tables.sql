CREATE TABLE IF NOT EXISTS users (
    uuid VARCHAR(36) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    registration_ip VARCHAR(45) NOT NULL,
    last_login_ip VARCHAR(45) NOT NULL,
    auto_login BOOLEAN NOT NULL DEFAULT TRUE,
    email VARCHAR(255) DEFAULT NULL,
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_email (email)
);

CREATE TABLE IF NOT EXISTS restricted_ips (
    ip VARCHAR(45) PRIMARY KEY,
    blocked_until TIMESTAMP NOT NULL
);