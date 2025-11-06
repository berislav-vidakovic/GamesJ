# Games backend project in Java

## Complete vertical

### Table of Content

  [1. Create Project skeleton](#1-create-project-skeleton)    
  [2. Add ping endpoint](#2-add-ping-endpoint) 



### 1. Create Project skeleton

1. Generate Spring Boot Project on  https://start.spring.io

    - Fill

      - Project	Maven
      - Language	Java
      - Group	com.gamesj
      - Artifact	gamesj
      - Name	gamesj
      - Packaging	Jar
      - Java	21

    - Add Dependencies (click Add Dependencies):

      - Spring Web (for REST API)

      - Spring WebSocket (for WebSocket support)

      - Spring Boot DevTools

2. Download, Extract and run

        mvn spring-boot:run

3. Git init, commit, push

### 2. Add ping endpoint

### 3. Create Nginx Config Template for Spring Boot Backend

- Create file /etc/nginx/sites-available/gamesj

- Activate it

      sudo ln -s /etc/nginx/sites-available/gamesj /etc/nginx/sites-enabled/
      sudo nginx -t
      sudo systemctl reload nginx

### 4. Issue SSL Certificate with Certbot for gamesj. subdomain

- Run Certbot with Nginx plugin

      sudo certbot --nginx -d gamesj.barryonweb.com

- After success, certificates are stored in:

      /etc/letsencrypt/live/gamesj.barryonweb.com/fullchain.pem
      /etc/letsencrypt/live/gamesj.barryonweb.com/privkey.pem

- Reload Nginx:

      sudo systemctl reload nginx




