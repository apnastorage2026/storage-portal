# Apna Storage

Apna Storage is a photo library. A person creates an account, signs in, uploads pictures, and sees only their own. The API is Spring Boot. The website is React. PostgreSQL holds the accounts and the image records. The picture files live on disk.

## What works

| Action | Method and path |
|---|---|
| Create an account | `POST /api/signup/` |
| Sign in | `POST /api/signin/` |
| Upload a picture | `POST /api/upload-image/` |
| List your pictures | `GET /api/images/` |
| Your account details | `GET /api/account/` |
| Open a picture | `GET /media/...` |

Sign-up and sign-in return `{ "token", "username" }`. Later calls send `Authorization: Token <jwt>`. The same paths work with or without the trailing slash.

Passwords are stored as `pbkdf2_sha256` hashes. The plain password is never saved.

## Run it on your machine

You need Java 21, Node.js, and PostgreSQL.

Create the database once:

```sql
CREATE USER apna_user WITH PASSWORD 'choose-a-password';
CREATE DATABASE apnastorage OWNER apna_user;
```

Copy the settings file and fill in the password and a JWT secret of at least 32 characters:

```bash
cp backend/.env.example backend/.env
```

Start the API:

```bash
cd backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spring-boot:run
```

On the first start, Flyway creates `users`, `otp_codes`, and `images`. The API listens on http://localhost:8080.

Start the website in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The dev server forwards `/api` and `/media` to the API.

## Settings

`backend/.env` is not committed. Every key is listed in `backend/.env.example`.

| Key | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL |
| `JWT_SECRET` | Signs the login token. At least 32 characters. Use a new value on the server. |
| `JWT_EXPIRATION_MINUTES` | Token lifetime. Default is 1440 (one day). |
| `MEDIA_ROOT` | Folder where uploaded files are written |
| `MAX_UPLOAD_SIZE` | Largest upload. Default is 25MB. |
| `CORS_ALLOWED_ORIGINS` | Website origin allowed to call the API. Locally this is `http://localhost:5173`. On the server, set it to `https://your-domain.com`. |
| `PASSWORD_PBKDF2_ITERATIONS` | Cost of new password hashes. Default is 1500000. |
| `OTP_LENGTH`, `OTP_EXPIRY_MINUTES`, `OTP_MAX_ATTEMPTS` | Reserved for the OTP sign-in that is not built yet. The app still requires these keys. |

## Put the code on GitHub

Run these on your computer, from the project folder. Replace the GitHub address with the repo you created.

```bash
cd /Users/princekumar/Desktop/apna-storage
git add -A
git commit -m "Add the EC2 deploy files and README."
git remote add origin https://github.com/YOUR_USER/apna-storage.git
git push -u origin main
```

If `git remote -v` already shows `origin`, skip `git remote add` and push. If GitHub created the repo with a README and the push is rejected, run `git pull --rebase origin main` and then push again.

`.env`, `backend/media/`, `node_modules/`, and Maven `target/` stay off GitHub.

## Deploy on an EC2 instance

These steps use Ubuntu 24.04. The instance security group should allow 22, 80, and 443. Leave 8080 and 5432 closed to the internet. Point your domain's A record at the instance public IP before you request the certificate.

### 1. Install the server packages

```bash
sudo apt update
sudo apt install -y nginx openjdk-21-jre-headless postgresql postgresql-contrib certbot python3-certbot-nginx
```

### 2. Create the database

```bash
sudo -u postgres psql -c "CREATE USER apna_user WITH PASSWORD 'choose-a-password';"
sudo -u postgres psql -c "CREATE DATABASE apnastorage OWNER apna_user;"
```

### 3. Build on your computer and copy the files

On your computer:

```bash
cd backend
./mvnw -DskipTests package
cd ../frontend
npm install
npm run build
```

The jar is `backend/target/storage-0.0.1-SNAPSHOT.jar`. The website is `frontend/dist`.

```bash
scp backend/target/storage-0.0.1-SNAPSHOT.jar ubuntu@YOUR_SERVER:/tmp/storage.jar
scp -r frontend/dist ubuntu@YOUR_SERVER:/tmp/apnastorage-web
```

On the server:

```bash
sudo useradd --system --home /opt/apnastorage --shell /usr/sbin/nologin apnastorage
sudo mkdir -p /opt/apnastorage /var/www/apnastorage /var/lib/apnastorage/media
sudo mv /tmp/storage.jar /opt/apnastorage/storage.jar
sudo rm -rf /var/www/apnastorage/*
sudo cp -r /tmp/apnastorage-web/. /var/www/apnastorage/
sudo chown -R apnastorage:apnastorage /opt/apnastorage /var/lib/apnastorage
```

### 4. Write the server settings

```bash
sudo nano /opt/apnastorage/.env
```

Use `backend/.env.example` as the list of keys. On the server, set:

```
MEDIA_ROOT=/var/lib/apnastorage/media
CORS_ALLOWED_ORIGINS=https://your-domain.com
JWT_SECRET=a-new-secret-at-least-32-characters
```

Use the database password from step 2. Then:

```bash
sudo chown apnastorage:apnastorage /opt/apnastorage/.env
sudo chmod 600 /opt/apnastorage/.env
```

### 5. Start the API

```bash
sudo cp deploy/systemd/apnastorage.service /etc/systemd/system/apnastorage.service
sudo systemctl daemon-reload
sudo systemctl enable --now apnastorage
sudo systemctl status apnastorage
```

Copy that service file from this repo. If you are already on the server and the repo is not there, create `/etc/systemd/system/apnastorage.service` with the contents of `deploy/systemd/apnastorage.service`.

### 6. Nginx and the domain

Edit `deploy/nginx/apnastorage.conf` and replace `example.com` with your domain, including the `www` name if you use it. Then:

```bash
sudo cp deploy/nginx/apnastorage.conf /etc/nginx/sites-available/apnastorage
sudo ln -sf /etc/nginx/sites-available/apnastorage /etc/nginx/sites-enabled/apnastorage
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
sudo certbot --nginx -d your-domain.com -d www.your-domain.com
```

Certbot adds HTTPS and renews the certificate. Nginx serves the React build. It sends `/api/` and `/media/` to the API on port 8080. Uploads larger than 25MB are refused, matching the app limit.

Open `https://your-domain.com`, create an account, and upload a picture. The new file should appear under `/var/lib/apnastorage/media/user_<id>/`.
