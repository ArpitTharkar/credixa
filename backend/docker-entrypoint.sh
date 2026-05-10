#!/bin/sh

# Startup script for Spring Boot backend on Render
# This script ensures environment variables are properly passed to the JVM

set -e

echo "=========================================="
echo "Wallet Backend Startup"
echo "=========================================="

# Check if required environment variables are set
if [ -z "$DB_URL" ]; then
    echo "ERROR: DB_URL environment variable not set!"
    echo "Please set DB_URL in Render environment variables."
    echo "Example: jdbc:mysql://mysql.railway.internal:3306/railway?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    exit 1
fi

if [ -z "$DB_USER" ]; then
    echo "ERROR: DB_USER environment variable not set!"
    exit 1
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "ERROR: DB_PASSWORD environment variable not set!"
    exit 1
fi

# Set defaults for optional variables
PORT=${PORT:-8080}
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
SHOW_SQL=${SHOW_SQL:-false}

echo "Configuration:"
echo "  PORT: $PORT"
echo "  SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
echo "  DB_URL: $DB_URL"
echo "  DB_USER: $DB_USER"
echo "  DB_PASSWORD: [REDACTED]"
echo ""
echo "Starting application..."
echo "=========================================="

# Start Spring Boot with all required properties
exec java \
    -Dserver.port=$PORT \
    -Dspring.datasource.url="$DB_URL" \
    -Dspring.datasource.username="$DB_USER" \
    -Dspring.datasource.password="$DB_PASSWORD" \
    -Dspring.jpa.database-platform=org.hibernate.dialect.MySQLDialect \
    -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect \
    -Djakarta.persistence.jdbc.url="$DB_URL" \
    -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
    -Dspring.jpa.show-sql=$SHOW_SQL \
    -jar /app/app.jar
