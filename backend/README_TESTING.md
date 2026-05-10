# Testing the backend (Phase 1)

Follow these steps to run the database and the Spring Boot backend, then exercise the auth and wallet endpoints.

1) Start MySQL via Docker Compose (PowerShell):

```
cd backend
docker compose up -d
```

2) Run the backend (Windows PowerShell):

```
.\gradlew.bat bootRun
```

Notes:
- The app uses `spring.datasource.url: jdbc:mysql://localhost:3306/myappdb` (see `application.yml`).
- If you want Firebase verification, set `GOOGLE_APPLICATION_CREDENTIALS` to a service account JSON before running.

3) Quick API tests (replace placeholders):

- Verify / create user (temporary: token verification is a placeholder):

```
curl -X POST http://localhost:8081/auth/firebase -H "Content-Type: application/json" -d '{"phone":"+15551234567","token":"FAKE_FIREBASE_ID_TOKEN"}'
```

Expected response: JSON containing `userId` and `phone`.

- Add money to a wallet:

```
curl -X POST http://localhost:8081/wallet/add-money -H "Content-Type: application/json" -d '{"userId":1,"amount":1000}'
```

- Transfer (with idempotencyKey):

```
curl -X POST http://localhost:8081/transfer -H "Content-Type: application/json" -d '{"senderId":1,"receiverId":2,"amount":500,"idempotencyKey":"unique-key-123"}'
```

4) Inspect DB (example using Docker exec):

```
docker exec -it $(docker ps -qf "ancestor=mysql:8.0") mysql -uroot -ppassword myappdb -e "SELECT * FROM users; SELECT * FROM wallets; SELECT * FROM transactions;"
```

5) Next verification steps to enable real Firebase verification:
- Place your Firebase service account JSON in a safe path and export `GOOGLE_APPLICATION_CREDENTIALS`.
- In production, replace the placeholder verification in `AuthController.verifyFirebaseToken` with Firebase Admin SDK token verification.

If you want, I can run through these steps locally (start Docker, run backend) and report results, or implement real Firebase token verification now.


-- start the server
$p = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -First 1; if ($p) { Stop-Process -Id $p -Force }; $env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\gradlew.bat :backend:bootRun --args="--spring.profiles.active=dev"

start server
.\start-server.ps1


start server
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; cd C:\Android\MyApplication3; .\gradlew.bat :backend:bootJar