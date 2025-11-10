# Games - Full stack project in React, Java and MySql

<a href="https://gamesj.barryonweb.com">Run Project</a>  
<a href="https://github.com/berislav-vidakovic/GamesJ">View Code</a>


## Complete vertical

1. [Create Project skeleton](#1-create-project-skeleton)
2. [Add ping endpoint](#2-add-ping-endpoint)
3. [Create Nginx Config Template for Spring Boot Backend](#3-create-nginx-config-template-for-spring-boot-backend)
4. [Issue SSL Certificate with Certbot for gamesj. subdomain](#4-issue-ssl-certificate-with-certbot-for-gamesj-subdomain)
5. [Build backend, Deploy, install Java Runtime and Test](#5-build-backend-deploy-install-java-runtime-and-test)
6. [Register backend as service](#6-register-backend-as-service)
7. [Create CI/CD pipeline](#7-create-cicd-pipeline)
8. [Connect backend to DB](#8-connect-backend-to-db)
9. [Web socket and CORS policy to connect Frontend with backend](#9-web-socket-and-cors-policy-to-connect-frontend-with-backend)
10. [Hashing password and JWT authentication](#10-hashing-password-and-jwt-authentication)

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

- Create Controllers/PingController.java

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


### 7. Create CI/CD pipeline

- Create .github/workflows/deploy.yml

- Dev (Win): Create and update key pair 

      ssh-keygen -t ed25519 -C "github-ci" -f github_ci
  
  - copy keys to ~/.ssh/

- VPS (Linux): Add the Public key 

  - append github_ci.pub content to ~/.ssh/authorized_keys 

- Test connection Dev-VPS: 

      ssh -i ~/.ssh/github_ci barry75@barryonweb.com

- GitHub: Add the Private Key to GitHub Secrets

  - Repo: Settings - Secrets and variables - Actions - New repository secret
    - Paste full content of private key github_ci
  - (Optional TODO) Add Known Hosts Fingerprint

- Test connection GitHub-VPS

  - Create .github/workflows/test-ssh.yml

      ```yml
      name: Test SSH Connection
      on:
        workflow_dispatch:  # Allows trigger manually in GitHub
      jobs:
        test-ssh:
          runs-on: ubuntu-latest
          steps:
            - name: Checkout repository
              uses: actions/checkout@v4
            - name: Start SSH agent and load key
              uses: webfactory/ssh-agent@v0.9.0
              with:
                ssh-private-key: ${{ secrets.SSH_PRIVATE_KEY }}
            - name: Test SSH connection
              run: ssh -o StrictHostKeyChecking=no barry75@barryonweb.com "echo Connected successfully from GitHub!"
      ```

  - GitHub Actions - Run workflow


### 8. Connect backend to DB

- Add dependencies JPA and MySQL to pom.xml

- Configure datasource in src/main/resources/application.yaml

- Create Controller, Model, Repository


### 9. Web socket and CORS policy to connect Frontend with backend

- Add frontend dev and production URL to Config/CorsConfig.java

- Create Config/WebSocketConfig.java

- Create WebSockets/WebSocketHandler.java

- Add copy frontends to gamesj dir to frontend deploy yaml file 

- Add Websocket and Frontend sections to Nging config file gamesj 



### 10. Hashing password and JWT authentication

#### Workflow

1. Register
    - Request: send raw password
    - Backend applies hashing and stores hashed password into DB 
    - Response: acknowledgement 

2. Login
    - Request: send raw password
    - Backend compares matching password and stored hashed value
    - Response: JWT (JSON Web Token)
    - Frontend stores JWT into localStorage

3. Any action
    - Request: send JWT in header Authorization: Bearer 
    - Backend checks JWT if endpoint not in white list within JwtAuthFilter

4. Logout 
    - Request: send JWT in header Authorization: Bearer 
    - Response: acknowledgement 
    - Frontend clears JWT from localStorage


#### Implementation

- Add dependencies into pom.xml

- Create class SecurityConfig 

- Hashing password at user register using BCrypt 

    ```java
    String hashedPwd = passwordEncoder.encode(password);
    ```

- Validating password at user login

    ```java
    boolean passwordsMatch = passwordEncoder.matches(password, user.getPwd());
    ```

- Created class JwtUtil

  - SECRET_KEY, EXPIRATION_TIME_MS and generateToken

- Generate JWT and send it in API Response 

    ```java
    String token = JwtUtil.generateToken(user.getUserId(), user.getLogin());
    ```

- Frontend handles JWT with localStorage  

    - stores on login
    - removes on logout

- Include JWT in API Request Header

    ```ts
    "Authorization": "Bearer " + localStorage.getItem("authToken"),
    ```

- Apply JWT authentication check in backend - create filter class  JwtAuthFilter

  - define endpoints that skip authentication


### 11. Refresh token

/api/users/login  
/api/users/refresh

Actions From the logout state
  - First login with password
  - Next login automatic

Actions From login state
  - Action that requires authentication
  - Logout

![JWT Image](JWT.png)
  
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse: collapse; width: 100%; text-align: left;">
  <thead>
    <tr>
      <th>Transition</th>
      <th>From State</th>
      <th>Trigger (Frontend Request / Action)</th>
      <th>Backend Action</th>
      <th>Next State / Response</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>T1</td>
      <td><b>S1 – Logged Out</b></td>
      <td>Login</td>
      <td>Frontend decides to attempt login</td>
      <td><b>S2 – Logging</b></td>
    </tr>
    <tr>
      <td>T2</td>
      <td><b>S2 – Logging</b></td>
      <td>Get Refresh Login</td>
      <td>Send refresh token to backend</td>
      <td><b>S3 – Validate Refresh Token</b></td>
    </tr>
    <tr>
      <td>T3</td>
      <td><b>S3 – Validate Refresh Token</b></td>
      <td>Valid</td>
      <td>Issue new access + refresh tokens → store refresh token in DB</td>
      <td><b>S6 – Logged In</b></td>
    </tr>
    <tr>
      <td>T4</td>
      <td><b>S3 – Validate Refresh Token</b></td>
      <td>Invalid</td>
      <td>Reject → request manual login</td>
      <td><b>S4 – Manual Login</b></td>
    </tr>
    <tr>
      <td>T5</td>
      <td><b>S4 – Manual Login</b></td>
      <td>Enter password</td>
      <td>Send password to backend for validation</td>
      <td><b>S5 – Validate Password</b></td>
    </tr>
    <tr>
      <td>T6</td>
      <td><b>S5 – Validate Password</b></td>
      <td>Valid</td>
      <td>Issue access + refresh tokens → store refresh token in DB</td>
      <td><b>S6 – Logged In</b></td>
    </tr>
    <tr>
      <td>T7</td>
      <td><b>S5 – Validate Password</b></td>
      <td>Invalid</td>
      <td>Reject login → notify frontend</td>
      <td><b>S4 – Manual Login</b></td>
    </tr>
    <tr>
      <td>T8</td>
      <td><b>S6 – Logged In</b></td>
      <td>Get Access Token</td>
      <td>Send access token to backend for validation</td>
      <td><b>S7 – Validate Access Token</b></td>
    </tr>
    <tr>
      <td>T9</td>
      <td><b>S7 – Validate Access Token</b></td>
      <td>Valid</td>
      <td>Perform business logic</td>
      <td><b>S6 – Logged In</b></td>
    </tr>
    <tr>
      <td>T10</td>
      <td><b>S7 – Validate Access Token</b></td>
      <td>Invalid</td>
      <td>Reject request (401) → optionally trigger refresh flow</td>
      <td><b>S2 – Logging</b></td>
    </tr>
    <tr>
      <td>T11</td>
      <td><b>S6 – Logged In</b></td>
      <td>Logout</td>
      <td>Mark user offline → delete refresh token from DB</td>
      <td><b>S8 – Logging Out</b></td>
    </tr>
    <tr>
      <td>T12</td>
      <td><b>S8 – Logging Out</b></td>
      <td>Automatic</td>
      <td>No frontend action required</td>
      <td><b>S1 – Logged Out</b></td>
    </tr>
    <tr>
      <td>T13</td>
      <td><b>S8 – Logging Out</b></td>
      <td>Manual</td>
      <td>Clear tokens on frontend → mark user offline → delete refresh token from DB</td>
      <td><b>S1 – Logged Out</b></td>
    </tr>
  </tbody>
</table>
