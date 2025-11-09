-- Initialize db_games MySQL Database
START TRANSACTION;


DROP TABLE IF EXISTS sudokuboards;
CREATE TABLE sudokuboards (
  board_id INT AUTO_INCREMENT PRIMARY KEY,
  board VARCHAR(81) NOT NULL,
  solution VARCHAR(81) NOT NULL,
  name VARCHAR(20) NOT NULL,
  level TINYINT NOT NULL,
  UNIQUE (board)
);

DROP TABLE IF EXISTS users;
CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  password_hash VARCHAR(255) NOT NULL,
  login VARCHAR(100) NOT NULL UNIQUE,
  full_name VARCHAR(255),
  isonline BOOLEAN DEFAULT FALSE
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

INSERT INTO healthcheck (msg) VALUES ('Hello world from DB!');

COMMIT;
