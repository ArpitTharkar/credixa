# Render Deployment Guide

## Environment Variables Required

When deploying on Render, set these environment variables in the Render dashboard:

```
DB_URL=jdbc:mysql://mysql.railway.internal:3306/railway
DB_USER=root
DB_PASSWORD=<your-railway-mysql-password>
PORT=8080
SPRING_PROFILES_ACTIVE=prod
SHOW_SQL=false
```

## From Railway MySQL

If using Railway MySQL, find credentials in Railway dashboard:
1. Go to Railway → Your Project → MySQL service
2. Copy the connection details (host, port, database, username, password)
3. Format the `DB_URL` as: `jdbc:mysql://HOST:PORT/DATABASE?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`

## Docker Build

The Dockerfile:
- Builds the backend module only (ignores Android app)
- Compiles to bootJar executable
- Runs on the JRE image with environment variables passed to Spring Boot

## Health Check

After deployment, test the API:
```bash
curl https://<your-render-service>.onrender.com/api/health
```

## Troubleshooting

**Issue: Hibernate dialect error**
- Ensure `DB_URL` is set and accessible
- Verify MySQL port 3306 is open on Railway
- Check database exists at specified URL

**Issue: Connection timeout**
- Verify Railway MySQL is running
- Check firewall/network policies allow Render → Railway
- Ensure credentials are correct

