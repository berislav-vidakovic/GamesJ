## Introducing TLS

### 1. Ensure ports 80 and 443 are reachable (HTTP and HTTPS)

    sudo ss -tuln | grep -E ':(80|443)\b' || sudo netstat -tuln | grep -E ':(80|443)\b'

Only port 80 for HTTP is OK

### 2. Backup current Nginx configuration and site data

    sudo cp -a /etc/nginx /etc/nginx.backup.$(date +%F_%T)

### 3. Install and verify certbot

    sudo apt update
    sudo apt install certbot python3-certbot-nginx
    which certbot
    certbot --version

### 4. Request and install certificate for gamesj. subdomain 

- Verify  certificates, timer and renewal 

  ```bash
  sudo certbot certificates
  sudo systemctl status certbot.timer
  systemctl list-timers | grep certbot
  sudo certbot renew --dry-run
  ```

Test renewal command simulates the renewal of all Let’s Encrypt certificates without actually changing anything.
It runs the exact same steps as a real renewal (checks DNS, validates domain ownership, contacts Let’s Encrypt).
But instead of issuing new certs, it requests test certificates from Let’s Encrypt’s staging servers.
It doesn’t modify live certificates or Nginx. 
It reports any issues (permissions, missing plugins, wrong credentials, DNS propagation issues, etc).

        

- Check IP address it points to 

  ```bash
  dig +short gamesj.barryonweb.com 
  dig +short barryonweb.com 
  ```

- Ensure Nginx has a server block for it

  
- Create symbolic link into sites-enabled

  ```bash
  sudo ln -s /etc/nginx/sites-available/gamesj /etc/nginx/sites-enabled/
  sudo nginx -t
  sudo systemctl reload nginx
  ```

- Run certbot

  ```bash
  sudo certbot --nginx -d gamesj.barryonweb.com
  ```

- Add HSTS header to the end of listen 443 ssl block in Nginx conf

  ```bash
  add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
  ```

- Test

   - curl -I http://gamesj.barryonweb.com
  
   - curl -I https://games.barryonweb.com
  
   
### 5. Ensure HTTP → HTTPS redirect

- Update last section in certbot-updated conf file gamesj:

      # Redirect all HTTP requests to HTTPS
      server {
          listen 80;
          server_name gamesj.barryonweb.com;
          return 301 https://$host$request_uri;
      }


- Check syntax and reload

      sudo nginx -t
      sudo systemctl reload nginx


- Verify

      curl -I http://gamesj.barryonweb.com

  
### 6. Add HSTS (HTTP Strict Transport Security)

#### Purpose: tells browsers “always use HTTPS for this domain.”

Once a browser sees it, it will force HTTPS and never attempt plain HTTP again (until expiry).

- Add to server block (listen 443 ssl) in conf file:

      add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

- Check syntax, reload and verify

      sudo nginx -t
      sudo systemctl reload nginx
      curl -I https://gamesj.barryonweb.com

  - Expected line in output

        Strict-Transport-Security: max-age=31536000; includeSubDomains

### 7. Verify TLS Versions & Ciphers

Ensure in /etc/letsencrypt/options-ssl-nginx.conf

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;






