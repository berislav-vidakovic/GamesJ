#!/bin/bash
# run-gamesj-test.sh
# Simple script to run GamesJ backend in Docker

# Stop and remove any existing container
docker rm -f gamesj-backend-test 2>/dev/null

# Run container
docker run -d \
  --name gamesj-backend-test \
  -p 8084:8082 \
  --restart unless-stopped \
  -e DB_URL=jdbc:mysql://barryonweb.com:3306/db_gamestest \
  -e DB_USER=barry75 \
  -e DB_PASSWORD=abc123 \
  berislavvidakovic/gamesj-backend-test:1.0

echo "Container 'gamesj-backend-test' started. Access backend via port 8084."
