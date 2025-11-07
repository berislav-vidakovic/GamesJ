# Games backend project in Java

## Complete vertical

1. [Create Project skeleton](#1-create-project-skeleton)
2. [Add ping endpoint](#2-add-ping-endpoint)
3. [Create Nginx Config Template for Spring Boot Backend](#3-create-nginx-config-template-for-spring-boot-backend)
4. [Issue SSL Certificate with Certbot for gamesj. subdomain](#4-issue-ssl-certificate-with-certbot-for-gamesj-subdomain)
5. [Build backend, Deploy, install Java Runtime and Test](#5-build-backend-deploy-install-java-runtime-and-test)
6. [Register backend as service](#6-register-backend-as-service)


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

- Create basic file /etc/nginx/sites-available/gamesj

    ```bash
    server {
      listen 80;
      server_name gamesj.barryonweb.com;

      location / {
          proxy_pass http://127.0.0.1:8080/;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
      }

      # Health check endpoint
      location /ping {
          proxy_pass http://127.0.0.1:8080/api/ping;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
      }
    }
    ```

- Activate, verify, reload

      sudo ln -s /etc/nginx/sites-available/gamesj /etc/nginx/sites-enabled/
      ls -l /etc/nginx/sites-enabled/
      sudo nginx -t
      sudo systemctl reload nginx

### 4. Issue SSL Certificate with Certbot for gamesj. subdomain

- Run Certbot with Nginx plugin

      sudo certbot --nginx -d gamesj.barryonweb.com

- After success, certificates are stored in:

      /etc/letsencrypt/live/gamesj.barryonweb.com/fullchain.pem
      /etc/letsencrypt/live/gamesj.barryonweb.com/privkey.pem

- Update last section in gamesj Nginx config file

  - Add HSTS line in SSL section - Force browsers to use HTTPS, even if the user tries HTTP

    ```bash
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    ```

  - Update last section 

    ```bash
    # Redirect all HTTP requests to HTTPS
    server {
        listen 80;
        server_name gamesj.barryonweb.com;
        return 301 https://$host$request_uri;
    }
    ```

- Reload Nginx:

      sudo systemctl reload nginx


### 5. Build backend, Deploy, install Java Runtime and Test

- Create target/gamesj-0.0.1-SNAPSHOT.jar

      mvn clean package

- Copy jar file to server 

      scp target/gamesj-0.0.1-SNAPSHOT.jar barry75@barryonweb.com:/var/www/games/gamesj/

- Install Java runtime and verify

      wget https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.9+10/OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10.tar.gz
      sudo mkdir -p /usr/lib/jvm
      sudo tar -xzf OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10.tar.gz -C /usr/lib/jvm
      sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-21.0.9+10/bin/java 1
      sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/jdk-21.0.9+10/bin/javac 1
      sudo update-alternatives --config java
      sudo update-alternatives --config javac
      java --version
      javac --version

- Run backend manually

      java -jar /var/www/games/gamesj/gamesj-0.0.1-SNAPSHOT.jar


- Test in Terminal

      curl https://gamesj.barryonweb.com/ping

- Test in Browser 

      https://gamesj.barryonweb.com/ping


### 6. Register backend as service

  - Create service file in  /etc/systemd/system/gamesj.service

  - Reload systemd to register the service

        sudo systemctl daemon-reload

  - Start/stop the service 

        sudo systemctl start gamesj
        sudo systemctl stop gamesj

  - Check the status 

        sudo systemctl status gamesj

  - Enable automatic start on boot

        sudo systemctl enable gamesj

  - Enable no password to restart service

        sudo visudo 
        barry75 ALL=(ALL) NOPASSWD: /bin/systemctl restart gamesj
        barry75 ALL=(ALL) NOPASSWD: /bin/systemctl reload nginx
        barry75 ALL=(ALL) NOPASSWD: /bin/cp, /bin/ln, /usr/sbin/nginx

  - Check no password commands for the user

        sudo -l -U barry75


  - Follow logs in realtime

        sudo journalctl -u gamesj -f
