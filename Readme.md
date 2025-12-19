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
8. [Connect backend to DB via JPA/Hibernate](#8-connect-backend-to-db-via-jpahibernate)
9. [Web socket and CORS policy to connect Frontend with backend](#9-web-socket-and-cors-policy-to-connect-frontend-with-backend)
10. [Hashing password and JWT authentication](#10-hashing-password-and-jwt-authentication)
11. [Refresh token and auto login/logout](#11-refresh-token-and-auto-loginlogout)
12. [Session monitor and idle cleanup](#12-session-monitor-and-idle-cleanup)
13. [Data Migration between MySQL and PostgreSQL database](#13-data-migration-between-mysql-and-postgresql-database)
14. [Add support for GraphQL](#14-add-support-for-graphql)



### 1. Create Project skeleton

1. Generate Spring Boot Project on  https://start.spring.io

    - Fill

      - Project	Maven
      - Language	Java
      - Spring Boot - latest stable (no RC2, SNAPSHOT)
      - Group	com.gamesj
      - Artifact	gamesj
      - Name	gamesj
      - Packaging	Jar
      - Java	21

    - Add Dependencies (click Add Dependencies):

      - Spring Web (for REST API)
      - WebSocket (for WebSocket support)
      - Spring Boot DevTools
    
    - Download and Extract

2. Define Port (default is 8080) in application.yaml (root key)

      ```yaml
      server:
        port: 8082
      ```

3. Run

        mvn spring-boot:run

4. Git init, commit, push

    - Create Repo on GitHub
    - Run
      ```bash
      git init
      git add .
      git commit -m "Initial commit"
      ```
    - Get Remote Repo SSH link and run
      ```bash
      git remote add origin git@github.com:berislav-vidakovic/ChatAppJn.git
      ```




### 2. Add ping endpoint

- Create Controllers/PingController.java
  - Enpoint: /api/ping
  - Response: { response: pong }

- Test connection
  ```
  http://localhost:8082/api/ping
  ```

### 3. Create Nginx Config Template for Spring Boot Backend

- Create basic file /etc/nginx/sites-available/gamesj

    ```bash
    server {
      listen 80;
      server_name gamesj.barryonweb.com;

      location / {
          proxy_pass http://127.0.0.1:8082/;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
      }

      # Health check endpoint
      location /ping {
          proxy_pass http://127.0.0.1:8082/api/ping;
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

- Check Linux version, CPU, memory, disk
  ```bash
  lsb_release -a
  htop
  free -h
  df -h
  ```


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


### 8. Connect backend to DB via JPA/Hibernate

- Add dependencies JPA and MySQL to pom.xml

- Remote access

  - Configure datasource in src/main/resources/application.yaml
    ```yaml
    url: jdbc:mysql://barryonweb.com:3306/db_games?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    ```

  - Check if remote MySQL accepts external connections, update and restart
    ```bash
    sudo netstat -tulnp | grep 3306
    /etc/mysql/mysql.conf.d/mysqld.cnf -> [mysqld] bind-address = 0.0.0.0
    sudo systemctl restart mysql
    ```

  - Check existing users
    ```sql
    SELECT User, Host, authentication_string, plugin FROM mysql.user;
    ```

  - Create a remote-access version of this user and grant it privileges
    ```sql
    CREATE USER 'barry75'@'%' IDENTIFIED WITH caching_sha2_password BY 'StrongPwd!';
    GRANT ALL PRIVILEGES ON db_games.* TO 'barry75'@'%';
    FLUSH PRIVILEGES;
    ```

- Create Controller, Model, Repository

- MySQL
  - Get MySQL version and current database selected
    ```sql
    SELECT VERSION();
    SELECT DATABASE();
    ```

  - Show tables and details
    ```sql
    SHOW TABLES;
    DESCRIBE sudokuboards;
    ```

  - Add missing column
    ```sql
    ALTER TABLE  sudokuboards
    ADD COLUMN testedOK tinyint(1) DEFAULT 0;
    ```

  - Copy content from local MySQL database to remote
    ```sql
    -- On source server
    SELECT * 
    INTO OUTFILE 'C:/ProgramData/MySQL/MySQL Server 9.4/Data/Uploads/sudokuboards.csv'
    FIELDS TERMINATED BY ','
    ENCLOSED BY '"'
    LINES TERMINATED BY '\n'
    FROM sudokuboards;

    scp 'C:/ProgramData/MySQL/MySQL Server 9.4/Data/Uploads/sudokuboards.csv' barry75@barryonweb.com:/var/www/games/gamesj/data/mysql/

    -- On target server
    sudo mysql -u root -p
    GRANT FILE ON *.* TO 'barry75'@'localhost';
    FLUSH PRIVILEGES;

    SHOW VARIABLES LIKE 'secure_file_priv';
    sudo cp /var/www/games/gamesj/data/mysql/sudokuboards.csv /var/lib/mysql-files/
    sudo chown mysql:mysql /var/lib/mysql-files/sudokuboards.csv

    LOAD DATA INFILE '/var/lib/mysql-files/sudokuboards.csv'
    INTO TABLE sudokuboards --existing table
    FIELDS TERMINATED BY ','
    ENCLOSED BY '"'
    LINES TERMINATED BY '\n';    
    ```
- Test connection

  - MySQL Internal and external connection
    ```bash
    mysql -u barry75 -p
    mysql -h barryonweb.com -P 3306 -u barry75 -p db_games;
    ```

  - Backend connection to DB
    ```bash
    http://localhost:8082/api/pingdb
    ```


### 9. Web socket and CORS policy to connect Frontend with backend

- Add frontend dev and production URL to Config/CorsConfig.java

- Create Config/WebSocketConfig.java

- Create WebSocket/WebSocketHandler.java

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
    - Frontend stores JWT into sessionStorage

3. Any action
    - Request: send JWT in header Authorization: Bearer 
    - Backend checks JWT if endpoint not in white list within JwtAuthFilter

4. Logout 
    - Request: send JWT in header Authorization: Bearer 
    - Response: acknowledgement 
    - Frontend clears JWT from sessionStorage


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

- Frontend handles JWT with sessionStorage  

    - stores on login
    - removes on logout

- Include JWT in API Request Header

    ```ts
    "Authorization": "Bearer " + sessionStorage.getItem("authToken"),
    ```

- Apply JWT authentication check in backend - create filter class  JwtAuthFilter

  - define endpoints that skip authentication


### 11. Refresh token and auto login/logout

/api/users/login  
- API Request: { userId, password }
- API Response: { userId, isOnline = true, accessToken, refreshToken }
- WS Broadcast: { type: userSessionUpdate }

/api/users/refresh
- API Request: { refreshToken }
- API Response: { userId, isOnline = true, accessToken, refreshToken }
- WS Broadcast: { type: userSessionUpdate }

/api/users/logout
- Request: { userId }
- Response: { userId, isOnline = false }
- WS Broadcast: { type: userSessionUpdate }

/websocket
- Broadcast auto logout: { type: userSessionUpdate, data: { automaticLogout =true } }


### Login / Logout FSM — State Transitions



![JWT Image](JWT.png)
  

 <table border="1" cellspacing="0" cellpadding="6" style="border-collapse: collapse; width: 100%; text-align: left;">
  <thead>
    <tr>
      <th>#</th>
      <th>Start State</th>
      <th>Transition</th>
      <th>Next State</th>
    </tr>
  </thead>
  <tbody>
    <tr><td>1</td><td>S1 – Logged out</td><td>T1 – Browser Refresh</td><td>S2 – Browser Refresh</td></tr>
    <tr><td>2</td><td>S1 – Logged out</td><td>T2 – Manual Login</td><td>S3 – Auto Login</td></tr>
    <tr><td>3</td><td>S2 – Browser Refresh</td><td>T3 – Get Refresh Token</td><td>S4 – Refresh Token Validation</td></tr>
    <tr><td>4</td><td>S3 – Auto Login</td><td>T4 – Get Refresh Token</td><td>S4 – Refresh Token Validation</td></tr>
    <tr><td>5</td><td>S4 – Refresh Token Validation</td><td>T5 – Invalid</td><td>S5 – invalid Refresh Token</td></tr>
    <tr><td>6</td><td>S5 – invalid Refresh Token</td><td>T6 – Browser Refresh</td><td>S1 – Logged out</td></tr>
    <tr><td>7</td><td>S5 – invalid Refresh Token</td><td>T7 – Auto Login</td><td>S6 – UI Dialog Login</td></tr>
    <tr><td>8</td><td>S6 – UI Dialog Login</td><td>T8 – Enter Password</td><td>S7 – Password Validation</td></tr>
    <tr><td>9</td><td>S7 – Password Validation</td><td>T9 – Invalid</td><td>S6 – UI Dialog Login</td></tr>
    <tr><td>10</td><td>S7 – Password Validation</td><td>T10 – Valid</td><td>S8 – Renew Tokens</td></tr>
    <tr><td>11</td><td>S8 – Renew Tokens</td><td>T11 – Tokens renewed</td><td>S9 – Logged In</td></tr>
    <tr><td>12</td><td>S4 – Refresh Token Validation</td><td>T12 – Valid</td><td>S8 – Renew Tokens</td></tr>
    <tr><td>13</td><td>S9 – Logged In</td><td>T13 – Get Access Token</td><td>S10 – Access Token Validation</td></tr>
    <tr><td>14</td><td>S10 – Access Token Validation</td><td>T14 – Invalid</td><td>S3 – Auto Login</td></tr>
    <tr><td>15</td><td>S10 – Access Token Validation</td><td>T15 – Valid</td><td>S9 – Logged In</td></tr>
    <tr><td>16</td><td>S9 – Logged In</td><td>T16 – Manual Logout</td><td>S11 – Clear Tokens</td></tr>
    <tr><td>17</td><td>S11 – Clear Tokens</td><td>T17 – Tokens cleared</td><td>S1 – Logged out</td></tr>
    <tr><td>18</td><td>S9 – Logged In</td><td>T18 – Auto Logout</td><td>S1 – Logged out</td></tr>
  </tbody>
</table>


### 12. Session Monitor and idle cleanup

- Websocket connection
  - Established on mount
  - Closed on idle timeout or Browser close
  - Close/Reconnected on Browser Refresh

- Websocket Monitor
  - Collect sessions and users to remove
  - Remove users - Auto logout
  - Close sessions and remove from Map

#### Implementing Timer for cleanup Users

- When the 1st User connected Timer started
- Timer is running while there are connected users
- When the last User disconnected Timer stops

There is checklist for Timer implementation

  - using a ScheduledExecutorService
  - starting the timer only when needed
  - stopping the timer when the last user is removed
  - shutting down cleanly via @PreDestroy
  - avoiding race-conditions with synchronized
  - stopping only the repeating task (not the whole executor)


1. Define parameters in application.yaml:

    ```yaml
    useridle:
      timeout-mins: 2
      check-interval-sec: 60
    ```

2. Inject parameters from application.yaml to class member variables

    ```java
    @Value("${useridle.timeout-mins}")
    private short idleTimeoutMinutes;

    @Value("${useridle.check-interval-sec}")
    private short cleanupIntervalSeconds;
    ```

3. Add scheduler and task member variables

    ```java
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> cleanupTask;  // to control start/stop
    ```

4. Create stopTimer method

    ```java
    private synchronized void stopTimer() {
      if (cleanupTask != null && !cleanupTask.isCancelled()) 
          cleanupTask.cancel(false);
    }
    ```


5. Create cleanup method

    - check user timestamp if it is older than idleTimeoutMinutes 
    - stop Timer when last user removed 

    ```java
    public void cleanupIdleUsers() {
      try {
        LocalDateTime now = LocalDateTime.now();
        Duration idleTimeout = Duration.ofMinutes(idleTimeoutMinutes);
        userActivityMap.entrySet().removeIf(entry -> {
          boolean idle = Duration.between(entry.getValue().getTimeStamp(), now).compareTo(idleTimeout) > 0;
          if (idle) {
            int userId = entry.getKey();
            System.out.println(" *** Removing idle user: " + userId);
            autoLogout(userId);
          }
          return idle;
        });
        if( userActivityMap.isEmpty() ) // Stop timer if no active users remain
          stopTimer();
      } 
      catch (Exception e) {
        System.err.println("Fatal error in cleanupIdleUsers: " + e.getMessage());
        e.printStackTrace();
      }
    }
    ```

6. Create start Timer method

    - assign scheduler.scheduleAtFixedRate return value to cleanupTask
      - attach cleanup function 
      - set interval for its execution to cleanupIntervalSeconds

    ```java
    private synchronized  void startTimer(){
      if (cleanupTask != null && !cleanupTask.isCancelled() && !cleanupTask.isDone())
        return; // already running
      cleanupTask = scheduler.scheduleAtFixedRate(
        this::cleanupIdleUsers,
        cleanupIntervalSeconds,
        cleanupIntervalSeconds,
        TimeUnit.SECONDS
      );
      System.out.println(" *** Timer started " );
    }
    ```

7. Call startTimer when the first user added

    - Timer not started if cleanupTask not defined 

    ```java
    public void updateUserActivity(int userId, UUID clientId) {
      // add or update userId in map  
      userActivityMap.compute(userId, (key, existingClient) -> {
        if (existingClient == null) {
          return new Client(LocalDateTime.now(), clientId);
        } 
        else {
          existingClient.setTimeStamp();
          existingClient.setClientId(clientId);
          return existingClient;
        }
      });
      if (cleanupTask == null || cleanupTask.isCancelled() || cleanupTask.isDone()) 
        startTimer();
    }
    ```

8. Call stopTimer when the last user removed

    ```java
    public synchronized void removeUser(int userId) {
      if (userActivityMap.remove(userId) != null) {
        System.out.println(" *** User " + userId + " removed from UserMonitor");
        if (userActivityMap.isEmpty()) { // If no users remain → stop timer
          System.out.println(" *** LAST User (" + userId + ") removed from UserMonitor");
          stopTimer();
        }
      } 
      else 
        System.out.println(" *** removeUser: user " + userId + " not found in map");
    }
    ```

9. Create shutdown method to be  called when the Spring application is shutting down.

    ```java
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        System.out.println(" *** Scheduler SHUTDOWN");
    }
    ```


### 13. Data Migration between MySQL and PostgreSQL database



#### Table of Contents

1. [Skills demonstrated](#skills-demonstrated)
2. [Challenges & Solutions](#challenges--solutions)
3. [Prerequisites](#prerequisites)
4. [Export from MySQL to CSV file](#export-from-mysql-to-csv-file)
5. [Import from CSV file to PostgreSQL](#import-from-csv-file-to-postgresql)
6. [Schedule synchronization job](#schedule-synchronization-job)
7. [Export from PostgreSQL and inport to MySQL](#export-from-postgresql-and-inport-to-mysql)



#### Skills demonstrated

- Cross-database migration (MySQL ↔ PostgreSQL)
- Bash scripting and automation with logging
- SQL troubleshooting: type conversion, transactions, constraints
- Linux permissions, cron jobs, and server file management
- Problem-solving for real-world database integration challenges

#### Challenges & Solutions 

- **Schema and Type Differences**: Cast PostgreSQL booleans to integers for MySQL compatibility and reset auto-increment IDs.

- **Foreign Key Constraints**: Foreign key checks can be temporarily disabled to allow truncation of dependent tables. Used alternative solution with DELETE instead of TRUNCATE in PostgreSQL->MySQL case.

- **File Permissions & Server Restrictions**: Used a dedicated directory and adjusted Linux permissions for safe CSV export/import.

- **Automation & Reliability**: Built a Bash script with logging and scheduled it via cron for daily execution.

- **Data Export/Import Differences**: Standardized CSV format and handled headers correctly for smooth cross-database migration.

#### Prerequisites

- Installed MySQL and PostgreSQL
- Existing Database with populated tables on MySQL
- Empty Database created in PostgreSQL


#### Export from MySQL to csv file

- Check MySQL’s allowed OUTFILE directory
  ```bash
  sudo mysql
  SHOW VARIABLES LIKE 'secure_file_priv';
  ```

- Add user to mysql group, relogin and verify
  ```bash
  sudo usermod -aG mysql barry75
  groups
  ```

- Login as root, check directory permissions, add rx to group
  ```bash
  ls -ld /var/lib/mysql-files
  chmod g+rx /var/lib/mysql-files
  ```

- Export table to CSV file as super user, which is required because SELECT … INTO OUTFILE needs write access to MySQL’s secure folder (/var/lib/mysql-files).
  ```sql
  sudo mysql
  use db_games
  SELECT * 
  INTO OUTFILE '/var/lib/mysql-files/sudokuboards.csv'
  FIELDS TERMINATED BY ','
  ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
  FROM sudokuboards;
  ```

- Copy CSV file
  ```bash
  cp /var/lib/mysql-files/sudokuboards.csv .
  ```


#### Import from csv file to PostgreSQL

- Copy schema script to server, connect to PostgreSQL and run script 
  ```bash
  scp -P 2222 schemapg.sql barry75@barryonweb.com:/var/www/data/migration/games/
  psql -U barry75 -d db_games
  \i /var/www/data/migration/games/schemapg.sql
  ```

- Copy all CSV files from mysql folder 
  ```bash
  cp /var/lib/mysql-files/*.csv /var/www/data/migration/games/ 
  ```

- Connect to PostgreSQL as regular user and Import from CSV
  ```sql
  psql -U barry75 -d db_games
  TRUNCATE sudokuboards RESTART IDENTITY CASCADE;
  \copy sudokuboards(board_id, board, solution, name, level, testedOK)
  FROM '/var/www/data/migration/games/sudokuboards.csv'
  DELIMITER ',' CSV QUOTE '"';
  ```


#### Schedule synchronization job

- MySQL password cfg  ~/.my.cnf
  ```bash
  [client]
  user=root
  ```

- PostgreSQL password cfg ~/.pgpass
  ```bash  
  localhost:5432:db_games:barry75:StrongPwd!
  chmod 600 ~/.pgpass
  ```

- Enable user to run mysql via sudo
  ```bash
  sudo visudo
  barry75 ALL=(root) NOPASSWD: /usr/bin/mysql
  ```

- Enable deleting old files
  ```bash
  sudo chmod 770 /var/lib/mysql-files
  ```

- Create and copy <a href="src/main/resources/mysql_to_pg.sh">bash script</a>
  ```bash
  scp -P 2222 mysql_to_pg_daily.sh barry75@barryonweb.com:/var/www/data/migration/games/
  ```

- Make script executable and Run manually
  ```bash
  chmod +x mysql_to_pg_daily.sh
  ./mysql_to_pg_daily.sh
  ```

- Edit crontab:
  ```bash
  crontab -e
  ```

- Run every day at 03:55 PM (crontab format: minute,hour,day of month, month, day of week)
  ```bash
  55 15 * * * /var/www/data/migration/games/mysql_to_pg_daily.sh
  ```

- Logs will be written to:

      /var/www/data/migration/games/mysql_to_pg.log

- Current datetime on Linux, MySQL, PostgreSQL
  ```bash
  date
  SELECT NOW(); # PostgreSQL and MySQL
  ```

#### Export from PostgreSQL and import to MySQL

- Export from PostgreSQL (with boolean conversion)
  ```sql
  \copy (SELECT board_id, board, solution, name, level, 
  (testedOK::int) AS testedOK FROM sudokuboards) 
  TO '/var/www/data/migration/games/pgsudokuboards.csv' 
  DELIMITER ',' CSV HEADER;
  ```

- Copy to MySQL secure location
  ```sql
  SHOW VARIABLES LIKE 'secure_file_priv';
  ```
  ```bash
  cp pgsudokuboards.csv /var/lib/mysql-files/
  ```

- Backup in MySQL (SELECT INTO not supported)
  ```sql
  CREATE TABLE sudoku_backup LIKE sudokuboards;  -- copy structure
  INSERT INTO sudoku_backup SELECT * FROM sudokuboards;  -- copy data
  ```

- Import to CSV file as super user
  ```bash
  sudo mysql
  use db_games
  LOAD DATA INFILE '/var/lib/mysql-files/pgsudokuboards.csv' 
  INTO TABLE sudokuboards FIELDS TERMINATED BY ',' 
  ENCLOSED BY '"' LINES TERMINATED BY '\n' 
  IGNORE 1 LINES (board_id,board, solution, name, level, testedOK);
  ```

- Create <a href="src/main/resources/pg_to_mysql.sh">bash script</a> and copy
  ```bash
  scp -P 2222 pg_to_mysql.sh barry75@barryonweb.com:/var/www/data/migration/games/
  ```

### 14. Add support for GraphQL

#### Basic support

- Add dependecies to pom.xml

- Create GraphQL schema schema.graphqls in resources/graphql directory:
  ```graphql
  type Query {
    ping: String
  }
  ```

- Create Controller in API/GraphQL directory
  ```java
  package com.gamesj.API.GraphQL;
  import org.springframework.graphql.data.method.annotation.QueryMapping;
  import org.springframework.stereotype.Controller;
  @Controller
  public class PingControllerGraphQL {
    // Method name ping() corresponds to the Query field in schema.graphqls
    @QueryMapping
    public String ping() {
      return "pong";
    }
  }
  ```

- Update SecurityConfigs
  ```java
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth.anyRequest().permitAll() )  // allow all requests
      .httpBasic(httpBasic -> httpBasic.disable())   // disable HTTP Basic
      .formLogin(formLogin -> formLogin.disable()); // disable login form
    return http.build();
  } 
  ```

- Add endpoint /graphql to White list in OncePerRequestFilter subclass

- Test from Postman as POST with JSON body:
  ```json
  {
    "query": "{ ping }"
  }
  ```
  - Response expected:
    ```json
    {
      "data": {
        "ping": "pong"
      }
    }
    ```

- Add pingDb query
  - Extend GraphQL schema definition with pingDb: String
  - Add Controller to API/GraphQL
  - Test from Postman as POST with JSON body:
    ```json
    {
      "query": "{ pingDb }"
    }
    ```
    - Response expected 
      - Row exists:
        ```json
        {
          "data": {
            "pingDb": "Hello world from DB!"
          }
        }
        ```
      - No row exists:
        ```json
        {
          "data": {
            "pingDb": null
          }
        }
        ```
      - Database Error:
        ```json
        {
          "errors": [
            {
              "message": "Database connection failed",
              "locations": [...],
              "path": ["pingDb"]
            }
          ]
        }
        ```


#### Get all users query

- Update GraphQL schema
- Add Controller
- Add User DTO
