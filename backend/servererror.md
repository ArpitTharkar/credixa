# Server errors and recovery notes

Date: 2026-05-06

## Summary
This document records the server problems encountered today, their root causes, how they were fixed, and exact commands/files you can use to start the backend locally.

## Problems observed (root cause + fix)

- Duplicate main-class / wrong package
  - Cause: a duplicate package `com.arpit.backend` (extra main) existed alongside the real `com.arpit.walletbackend` class which caused build/run conflicts.
  - Fix: deleted the duplicate package and set the explicit `mainClass` in `backend/build.gradle.kts` to `com.arpit.walletbackend.WalletBackendApplication`.

- Java not on PATH (JDK present but not exposed)
  - Cause: JDK 17 installed at `C:\Program Files\Java\jdk-17` but not added to system PATH.
  - Fix: start script (`start-server.ps1`) sets `$env:JAVA_HOME` and prepends its `bin` to `$env:PATH` before running Gradle or `java`.

- Running Gradle from wrong directory
  - Cause: `gradlew.bat` is at project root; running it from `backend/` caused errors.
  - Fix: Always run commands from project root `C:\Android\MyApplication3` or use the root script `start-server.ps1` which runs from root.

- Port 8081 conflicts / zombie process
  - Cause: previous server process remained bound to port 8081 after Ctrl+C or crash.
  - Fix: `start-server.ps1` kills any process owning TCP port 8081 before starting the server (uses `Get-NetTCPConnection` + `Stop-Process`).

- Gradle `bootRun` progress bar confusing (appears stuck at 80%)
  - Cause: `bootRun` runs inside Gradle and the progress indicator stays at 80% while the app runs; users interpreted that as “not started”.
  - Fix: use `./gradlew :backend:bootJar` once, then run the produced fat JAR with `java -jar` to avoid Gradle’s long-running progress indicator. `start-server.ps1` now follows this pattern.

- Android device cannot reach server
  - Cause: physical device requires `adb reverse` to map device localhost to host, and the mapping resets when device reconnects or server restarts.
  - Fixes:
    - Run `adb reverse tcp:8081 tcp:8081` after pairing the device.
    - `start-server.ps1` now detects `adb` and runs `adb reverse tcp:8081 tcp:8081` automatically when available.

- Firebase ID token verification placeholder
  - Cause: Firebase Admin SDK added but verification was left as a placeholder in the backend (`/api/auth/phone-login` accepts the phone without validating tokens).
  - Next step: supply a Firebase service account JSON and implement token verification in `AuthController`/`AuthService`.

- H2 vs MySQL confusion
  - Note: Local dev uses `application-dev.yml` with H2 in-memory DB (`jdbc:h2:mem:myappdb`) so you do not need Docker/MySQL to develop. Production uses `application.yml` (MySQL). Use `--spring.profiles.active=dev` when testing locally.

## Files that contain server/backend code

- `backend/build.gradle.kts` — backend build configuration (dependencies, `springBoot.mainClass` set here)
- `backend/src/main/java/com/arpit/walletbackend/WalletBackendApplication.java` — Spring Boot `main` class
- `backend/src/main/java/com/arpit/walletbackend/user/AppUser.java` — `AppUser` entity
- `backend/src/main/java/com/arpit/walletbackend/user/AppUserRepository.java` — user repository
- `backend/src/main/java/com/arpit/walletbackend/wallet/WalletAccount.java` — wallet entity
- `backend/src/main/java/com/arpit/walletbackend/wallet/WalletAccountRepository.java`
- `backend/src/main/java/com/arpit/walletbackend/tx/WalletTransaction.java`
- `backend/src/main/java/com/arpit/walletbackend/tx/TransactionRepository.java`
- `backend/src/main/java/com/arpit/walletbackend/auth/AuthController.java` — auth endpoints (`/api/auth/*`)
- `backend/src/main/java/com/arpit/walletbackend/auth/AuthService.java` — auth business logic (OTP bridge)
- `backend/src/main/java/com/arpit/walletbackend/api/HealthController.java` — health endpoint (`/api/health`)
- `backend/src/main/resources/application.yml` — production config (MySQL)
- `backend/src/main/resources/application-dev.yml` — dev config (H2)
- `backend/docker-compose.yml` — MySQL compose (not used if Docker is not installed)
- `start-server.ps1` (project root) — helper script to build/kill/start server and (now) run `adb reverse`

## Commands to start and test the server (exact commands)

1) One-time: build the backend JAR (run from project root):

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :backend:bootJar
```

2) Start server (recommended — uses the helper script):

```powershell
.\start-server.ps1
```

What `start-server.ps1` does now:
- Kills any process on port 8081
- Sets `JAVA_HOME` and updates `PATH`
- Builds the JAR if missing (`:backend:bootJar`)
- Runs `adb reverse tcp:8081 tcp:8081` if `adb` is available
- Starts the server with: `java -jar backend\build\libs\<name>.jar --spring.profiles.active=dev`

3) Manual run (equivalent to the script):

```powershell
java -jar backend\build\libs\backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

4) Ensure device mapping (if using a physical Android device):

```powershell
adb reverse tcp:8081 tcp:8081
adb reverse --list
```

5) Test endpoints (from host machine):

```powershell
curl http://localhost:8081/api/health
curl -X POST http://localhost:8081/api/auth/phone-login -H "Content-Type: application/json" -d '{"phone":"+919876543210"}'
```

## Tips & notes

- Always run the start commands from project root: `C:\Android\MyApplication3`.
- Keep the terminal open — closing it will stop the server when you started it inside that session.
- If you change backend code, re-run `:backend:bootJar` (or just re-run `start-server.ps1`, it will rebuild if needed).
- If you want persistent data, configure MySQL and use `application.yml` (or enable Docker and use `backend/docker-compose.yml`).
- For production-like testing, install Docker and bring up the `docker-compose` MySQL service, then point `application.yml` accordingly.

If you want, I can also:
- Add a quick health-check script to wait for the server to be fully ready before returning.
- Implement Firebase token verification (requires Firebase service account JSON).

-- End of report
