-- Initialize db_games MySQL Database
START TRANSACTION;


DROP TABLE IF EXISTS sudokuboards;
CREATE TABLE sudokuboards (
  board_id INT AUTO_INCREMENT PRIMARY KEY,
  board VARCHAR(81) NOT NULL,
  solution VARCHAR(81) NOT NULL,
  name VARCHAR(20) NOT NULL,
  level TINYINT NOT NULL,
  testedOK BOOLEAN DEFAULT FALSE,
  UNIQUE (board)
);

DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  password_hash VARCHAR(255) NOT NULL,
  login VARCHAR(100) NOT NULL UNIQUE,
  full_name VARCHAR(255),
  isonline BOOLEAN DEFAULT FALSE
);

CREATE TABLE refresh_tokens (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL UNIQUE, 
  token VARCHAR(255) NOT NULL,
  expires_at DATETIME NOT NULL,
  CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


DROP TABLE IF EXISTS localization;
CREATE TABLE localization (
  id INT AUTO_INCREMENT PRIMARY KEY,
  paramkey VARCHAR(100) NOT NULL,
  paramvalue VARCHAR(100) NOT NULL,
  lang VARCHAR(2) NOT NULL,
  UNIQUE KEY uk_param_lang (paramkey, lang)
);

DROP TABLE IF EXISTS healthcheck;
CREATE TABLE healthcheck (
  id INT AUTO_INCREMENT PRIMARY KEY,
  msg VARCHAR(255)
);


COMMIT;
