-- Initialize db_games MySQL Database
START TRANSACTION;

-- 1) Tables
CREATE TABLE healthcheck (
  id INT AUTO_INCREMENT PRIMARY KEY,
  msg VARCHAR(255)
);

-- 2) Initial data
INSERT INTO healthcheck (msg) VALUES ('Hello world from DB!');

COMMIT;
