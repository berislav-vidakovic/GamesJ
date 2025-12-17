-- Initialize db_games PostgreSQL Database

-- Drop tables if they exist
DROP TABLE IF EXISTS sudokuboards CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS localization CASCADE;
DROP TABLE IF EXISTS healthcheck CASCADE;

-- Table: sudokuboards
CREATE TABLE sudokuboards (
    board_id SERIAL PRIMARY KEY,
    board VARCHAR(81) NOT NULL,
    solution VARCHAR(81) NOT NULL,
    name VARCHAR(20) NOT NULL,
    level SMALLINT NOT NULL,
    testedOK BOOLEAN DEFAULT FALSE,
    UNIQUE (board)
);

-- Table: users
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    login VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    isonline BOOLEAN DEFAULT FALSE
);

-- Table: refresh_tokens
CREATE TABLE refresh_tokens (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Table: localization
CREATE TABLE localization (
    id SERIAL PRIMARY KEY,
    paramkey VARCHAR(100) NOT NULL,
    paramvalue VARCHAR(100) NOT NULL,
    lang CHAR(2) NOT NULL,
    CONSTRAINT uk_param_lang UNIQUE (paramkey, lang)
);

-- Table: healthcheck
CREATE TABLE healthcheck (
    id SERIAL PRIMARY KEY,
    msg VARCHAR(255)
);
