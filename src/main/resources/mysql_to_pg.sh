#!/bin/bash
set -euo pipefail

# Paths
MYSQL_OUT="/var/lib/mysql-files"
WORKDIR="/var/www/data/migration/games"
LOGFILE="/var/www/data/migration/games/mysql_to_pg.log"

echo "=== Migration started at $(date) ===" >> "$LOGFILE"

# 0. Clean old CSV files
rm -f "${MYSQL_OUT}"/sudokuboards.csv
rm -f "${MYSQL_OUT}"/localization.csv
rm -f "${MYSQL_OUT}"/refresh_tokens.csv
rm -f "${MYSQL_OUT}"/users.csv

# 1. Export from MySQL
sudo mysql db_games <<EOF
SELECT * INTO OUTFILE '${MYSQL_OUT}/sudokuboards.csv'
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n'
FROM sudokuboards;

SELECT * INTO OUTFILE '${MYSQL_OUT}/localization.csv'
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n'
FROM localization;

SELECT * INTO OUTFILE '${MYSQL_OUT}/refresh_tokens.csv'
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n'
FROM refresh_tokens;

SELECT * INTO OUTFILE '${MYSQL_OUT}/users.csv'
FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n'
FROM users;
EOF

echo "MySQL export completed" >> "$LOGFILE"

# 2. Copy CSVs to working directory
cp ${MYSQL_OUT}/*.csv "$WORKDIR"/

# 3. Import into PostgreSQL (single transaction)
psql -U barry75 -d db_games <<EOF
BEGIN;

TRUNCATE sudokuboards RESTART IDENTITY CASCADE;
\copy sudokuboards(board_id, board, solution, name, level, testedOK) FROM '${WORKDIR}/sudokuboards.csv' DELIMITER ',' CSV QUOTE '"';

TRUNCATE localization RESTART IDENTITY CASCADE;
\copy localization(id, paramkey, paramvalue, lang) FROM '${WORKDIR}/localization.csv' DELIMITER ',' CSV QUOTE '"';

TRUNCATE users RESTART IDENTITY CASCADE;
\copy users(user_id, password_hash, login, full_name, isonline) FROM '${WORKDIR}/users.csv' DELIMITER ',' CSV QUOTE '"';

TRUNCATE refresh_tokens RESTART IDENTITY CASCADE;
\copy refresh_tokens(id, user_id, token, expires_at) FROM '${WORKDIR}/refresh_tokens.csv' DELIMITER ',' CSV QUOTE '"';

COMMIT;
EOF

echo "PostgreSQL import completed" >> "$LOGFILE"
echo "=== Migration finished at $(date) ===" >> "$LOGFILE"
