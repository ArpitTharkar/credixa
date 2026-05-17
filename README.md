# Credixa – Fintech Wallet Android Application

## Overview

Credixa is a full-stack fintech-style wallet application built using Android and Spring Boot. The application allows users to register, log in, manage wallet balances, send money, and view transaction history in real time.

The project uses a cloud-hosted backend and database, enabling multi-device access and real-time synchronization.

---

# Features

## User Authentication

* User Registration
* Secure Login
* OTP-based signup flow
* Session handling

## Wallet Features

* Add Money
* Send Money
* Receive Money
* Wallet Balance Management

## Transactions

* Transaction History
* Sender & Receiver Tracking
* Timestamp-based records
* Cloud-synced transactions

## Backend Features

* REST APIs using Spring Boot
* MySQL database integration
* JPA/Hibernate ORM
* Cloud deployment with Docker

## Cloud Deployment

* Backend deployed on Render
* MySQL database hosted on Railway
* Dockerized backend deployment

---

# Tech Stack

## Android Frontend

* Java
* XML
* Retrofit
* RecyclerView
* Material Design
* MVVM Architecture

## Backend

* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* REST APIs

## Database

* MySQL
* Railway Cloud Database

## Deployment & Tools

* Docker
* Render
* GitHub
* Git

---

# Architecture

Android App
↓
Spring Boot Backend (Render)
↓
Railway MySQL Database

---

# Screenshots

## Login Screen

(Add screenshot here)

## Registration Screen

(Add screenshot here)

## Dashboard Screen

(Add screenshot here)

## Transaction History

(Add screenshot here)

---

# API Endpoints

## Authentication APIs

* POST /api/auth/register
* POST /api/auth/login

## Wallet APIs

* POST /api/wallet/add-money
* POST /api/wallet/send
* GET /api/wallet/balance

## Transaction APIs

* GET /api/transactions

---

# Project Setup

## Clone Repository

```bash
git clone https://github.com/ArpitTharkar/credixa.git
```

---

# Backend Setup

## Configure Database

spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/myappdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
    show-sql: ${SHOW_SQL:false}
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        use_sql_comments: true

server:
  port: ${PORT:8081}
  servlet:
    context-path: /api

```

---

## Run Backend

```bash
./gradlew :backend:bootRun
```

---

# Android Setup

## Open in Android Studio

1. Open Android Studio
2. Select project folder
3. Sync Gradle
4. Run app

---

# Deployment

## Backend Deployment

* Render
* Dockerized Spring Boot deployment

## Database Deployment

* Railway MySQL

---

# Challenges Solved

* Cloud backend deployment
* Docker configuration
* Environment variable management
* MySQL cloud database integration
* Android ↔ Backend API communication
* Multi-device synchronization
* Render deployment debugging

---

# Future Improvements

* Advanced UI animations
* Push Notifications
* QR Payments
* UPI Integration
* Biometric Authentication
* Firebase OTP Verification
* Dark Mode
* Payment Analytics Dashboard

---

# Author

## Arpit G. Tharkar

Android & Java Developer

GitHub:
[https://github.com/ArpitTharkar](https://github.com/ArpitTharkar)

LinkedIn:
www.linkedin.com/in/arpit-tharkar7

---

# Project Status

✅ Active Development

Credixa is currently under active development with ongoing UI enhancements and feature improvements.
