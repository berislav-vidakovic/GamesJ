#!/bin/bash
set -euo pipefail

# Paths
MYSQL_IN="/var/lib/mysql-files"
WORKDIR="/var/www/data/migration/games"
LOGFILE="/var/www/data/migration/games/pg_to_mysql.log"

echo "=== Migration started at $(date) ===" >> "$LOGFILE"

# 0. Clean old CSV files
rm -f "${MYSQL_IN}"/pgsudokuboards.csv
rm -f "${MYSQL_IN}"/pglocalization.csv
rm -f "${MYSQL_IN}"/pgrefresh_tokens.csv
rm -f "${MYSQL_IN}"/pgusers.csv

# 1. Export from PostgreSQL
psql -U barry75 -d db_games <<EOF
BEGIN;
\copy (SELECT board_id, board, solution, name, level, (testedOK::int) AS testedOK FROM sudokuboards) TO '${WORKDIR}/pgsudokuboards.csv' DELIMITER ',' CSV HEADER; 
\copy (SELECT id, paramkey, paramvalue, lang FROM localization) TO '${WORKDIR}/pglocalization.csv' DELIMITER ',' CSV HEADER;
\copy (SELECT id, user_id, token, expires_at FROM refresh_tokens) TO '${WORKDIR}/pgrefresh_tokens.csv' DELIMITER ',' CSV HEADER;
\copy (SELECT user_id, password_hash, login, full_name, (isonline::int) AS isonline FROM users) TO '${WORKDIR}/pgusers.csv' DELIMITER ',' CSV HEADER;
COMMIT;
EOF
echo "PostgreSQL export completed" >> "$LOGFILE"

# 2. Copy CSVs to working directory
cp ${WORKDIR}/pgsudokuboards.csv "$MYSQL_IN"/
cp ${WORKDIR}/pglocalization.csv "$MYSQL_IN"/
cp ${WORKDIR}/pgrefresh_tokens.csv "$MYSQL_IN"/
cp ${WORKDIR}/pgusers.csv "$MYSQL_IN"/


# 3. Import to MySQL
sudo mysql db_games <<EOF

DELETE FROM sudokuboards;
DELETE FROM localization;
DELETE FROM refresh_tokens;
DELETE FROM users;

LOAD DATA INFILE '${MYSQL_IN}/pgsudokuboards.csv' 
INTO TABLE sudokuboards FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' LINES TERMINATED BY '\n' 
IGNORE 1 LINES (board_id,board, solution, name, level, testedOK);

LOAD DATA INFILE '${MYSQL_IN}/localization.csv' 
INTO TABLE localization FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' LINES TERMINATED BY '\n' 
IGNORE 1 LINES (id, paramkey, paramvalue, lang);

LOAD DATA INFILE '${MYSQL_IN}/pgusers.csv' 
INTO TABLE users FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' LINES TERMINATED BY '\n' 
IGNORE 1 LINES (user_id, password_hash, login, full_name, isonline);

LOAD DATA INFILE '${MYSQL_IN}/pgrefresh_tokens.csv' 
INTO TABLE refresh_tokens FIELDS TERMINATED BY ',' 
ENCLOSED BY '"' LINES TERMINATED BY '\n' 
IGNORE 1 LINES (id, user_id, token, expires_at);
EOF

echo "MySQL import completed" >> "$LOGFILE"

echo "=== Migration finished at $(date) ===" >> "$LOGFILE"
