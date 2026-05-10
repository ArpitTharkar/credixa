# Render Deployment Guide

## Environment Variables Required in Render

**CRITICAL:** Set these exact environment variables in the Render dashboard (Settings → Environment):

```
DB_URL=jdbc:mysql://mysql.railway.internal:3306/railway?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASSWORD=<your-railway-password>
PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

### Finding Railway MySQL Credentials

1. Go to Railway Dashboard → Your Project → MySQL service
2. Click the MySQL service card
3. In Variables tab, you'll see:
   - `MYSQLHOST`: Use as part of DB_URL (e.g., `mysql.railway.internal`)
   - `MYSQLDATABASE`: Use in DB_URL (e.g., `/railway`)
   - `MYSQLUSER`: Set as `DB_USER`
   - `MYSQLPASSWORD`: Set as `DB_PASSWORD`
   - `MYSQLPORT`: Should be 3306

### Example Railway Connection

If Railway shows:
```
MYSQLHOST=mysql.railway.internal
MYSQLPORT=3306
MYSQLDATABASE=railway
MYSQLUSER=root
MYSQLPASSWORD=UUFdYWpRjlPMkyVFMJfpQtObRarZXgOJ
```

Then in Render set:
```
DB_URL=jdbc:mysql://mysql.railway.internal:3306/railway?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASSWORD=UUFdYWpRjlPMkyVFMJfpQtObRarZXgOJ
```

## Deployment Steps

1. **Push code to GitHub:**
   ```bash
   git add .
   git commit -m "Fix startup and add validation script"
   git push
   ```

2. **In Render Dashboard:**
   - Create Web Service connected to your GitHub repo
   - Leave Build Command empty (Dockerfile will auto-detect)
   - Set Environment Variables (see above)
   - Deploy

3. **First deployment:**
   - Spring Boot will automatically create database schema
   - Check logs for `Started WalletBackendApplication` message
   - This indicates successful startup

## Troubleshooting

### Startup Fails Immediately

Check Render logs for error:
- "ERROR: DB_URL environment variable not set!" → Add DB_URL to Render environment
- "Connection refused" → Railway MySQL might be down, restart it
- "Unknown database" → Verify database name matches
- "Access denied for user" → Check username/password

### Hibernate Schema Errors

If logs show Hibernate validation errors:
- This is normal on first deployment
- The app will auto-create tables (ddl-auto=update)
- Wait 30-60s for initialization
- Check logs for "Started WalletBackendApplication"

### Logs Not Showing DB Info

If you need more debugging:
1. Render Settings → Environment
2. Add: `LOG_LEVEL=DEBUG`
3. Redeploy
4. Check full logs for database initialization sequence

## Health Check Endpoint

Once deployed, test the API:
```bash
curl https://<your-render-service>.onrender.com/api/health
```

Should return 200 OK if database connected successfully.

