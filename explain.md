# Fintech Upgrade Execution Notes

This document tracks implementation details phase by phase.
For each phase, it records:
- What was done
- Which files were changed
- Why the changes were needed
- How to test

---

## Phase 0 - Foundation (Keep It Light)

### Goal
Project should run cleanly with minimum required setup for multi-device fintech migration:
- Keep Java app as-is
- Add Retrofit
- Add Firebase Auth (Phone OTP prerequisites)
- Set up Spring Boot backend + MySQL

### What was implemented
1. Android networking foundation added (Retrofit stack).
2. Firebase Auth dependencies added (using Firebase BOM).
3. Required Android network permissions added.
4. New backend module created in same Gradle workspace.
5. Spring Boot backend bootstrapped with Java 17, Web, JPA, Validation, MySQL connector.
6. Health endpoint added to verify backend startup quickly.
7. Backend Gradle compatibility fixed for workspace repository policy and Gradle 9 test runtime.

### Files changed and why

#### 1) `gradle/libs.versions.toml`
Changes:
- Added version entries: retrofit, okhttp, firebaseBom.
- Added libraries: retrofit, converter-gson, okhttp logging-interceptor, firebase-bom, firebase-auth.

Why required:
- Keeps dependency versions centralized and consistent.
- Enables clean Gradle alias usage in app module.

#### 2) `app/build.gradle.kts`
Changes:
- Added dependencies:
  - Retrofit
  - Retrofit Gson converter
  - OkHttp logging interceptor
  - Firebase BOM platform
  - Firebase Auth

Why required:
- Retrofit is needed for backend API integration in next phases.
- Firebase Auth dependency is required for phone OTP flow.
- Logging interceptor helps debug API requests/responses during integration.

#### 3) `app/src/main/AndroidManifest.xml`
Changes:
- Added:
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`

Why required:
- App cannot call backend or Firebase without internet permission.
- Network state permission helps handling online/offline checks.

#### 4) `settings.gradle.kts`
Changes:
- Added backend module include: `:backend`

Why required:
- Brings Spring Boot backend into the same workspace build lifecycle.

#### 5) `backend/build.gradle.kts` (new)
Changes:
- Added Java + Spring Boot + dependency-management plugins.
- Added dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-validation
  - mysql-connector-j
  - spring-boot-starter-test
  - junit-platform-launcher (test runtime)
- Configured Java toolchain (17).
- Enabled JUnit Platform for tests.

Why required:
- Provides minimal backend foundation for auth, wallet, transfer APIs in upcoming phases.
- MySQL connector enables DB integration.
- JUnit launcher resolves Gradle 9 test execution issue.

#### 6) `backend/src/main/java/com/arpit/walletbackend/WalletBackendApplication.java` (new)
Changes:
- Added Spring Boot main application class.

Why required:
- Backend entry point required to run service.

#### 7) `backend/src/main/java/com/arpit/walletbackend/api/HealthController.java` (new)
Changes:
- Added `GET /api/health` endpoint.

Why required:
- Fast smoke-check endpoint to validate backend boot and routing.

#### 8) `backend/src/main/resources/application.properties` (new)
Changes:
- Added app name and server port.
- Added MySQL datasource URL, username, password, driver.
- Added JPA settings (`ddl-auto=update`, SQL logging).

Why required:
- Connects backend to MySQL for upcoming entity persistence.

#### 9) `backend/src/test/java/com/arpit/walletbackend/WalletBackendApplicationTests.java` (new)
Changes:
- Added context load test.

Why required:
- Validates Spring context loads successfully in CI/build.

#### 10) `backend/.gitignore` (new)
Changes:
- Added build artifact ignore rules.

Why required:
- Keeps backend module clean in version control.

### Build and verification performed
Command run:
- `./gradlew.bat :app:assembleDebug :backend:build`

Result:
- Build successful for both app and backend modules.

### Build failures encountered and how they were solved

#### Failure 1: Repository policy conflict in backend module
Error seen:
- Build failed with message similar to:
  - `Build was configured to prefer settings repositories over project repositories but repository 'MavenRepo' was added by build file 'backend/build.gradle.kts'`

Why it failed:
- Root project uses `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` in `settings.gradle.kts`.
- Backend module had its own `repositories { mavenCentral() }` block, which violates that policy.

Fix applied:
- Removed the `repositories` block from `backend/build.gradle.kts`.
- Let backend use repositories defined centrally in `settings.gradle.kts`.

#### Failure 2: Backend test runtime could not load JUnit Platform
Error seen:
- Build failed at `:backend:test` with message similar to:
  - `Could not start Gradle Test Executor ... Failed to load JUnit Platform ... ensure JUnit Platform launcher is available`

Why it failed:
- With the current Gradle/Spring setup in this workspace, test runtime needed explicit JUnit platform launcher dependency.

Fix applied:
- Added test runtime dependency in `backend/build.gradle.kts`:
  - `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`

Re-run result after fixes:
- `./gradlew.bat :app:assembleDebug :backend:build` completed successfully.

### How to test Phase 0 locally

#### A) Android build check
1. Run: `./gradlew.bat :app:assembleDebug`
2. Expect: successful APK assembly.

#### B) Backend build check
1. Run: `./gradlew.bat :backend:build`
2. Expect: tests pass and jar is produced.

#### C) Backend run check
1. Run: `./gradlew.bat :backend:bootRun`
2. Open: `http://localhost:8080/api/health`
3. Expect JSON response similar to:
   - `{"status":"UP","service":"wallet-backend"}`

#### D) MySQL connectivity check
1. Ensure local MySQL is running.
2. Confirm database credentials in `backend/src/main/resources/application.properties`.
3. Start backend with `:backend:bootRun`.
4. Expect no datasource startup errors in logs.

#### E) Firebase readiness check (dependency-level only in Phase 0)
1. Confirm Firebase dependencies resolve during `:app:assembleDebug`.
2. Note: actual OTP login requires `google-services.json` in app module and Firebase console setup (to be completed in Phase 1).

### Notes / constraints intentionally followed
- Java codebase preserved.
- No Kotlin migration done.
- No KSP, Hilt, WorkManager, LeakCanary, or performance tooling added (deferred by plan).

---

## Phase 1 - Backend + Auth (Most Important)

### Goal
Create a multi-device identity bridge:
- Phone OTP verified by Firebase on Android.
- Phone sent to backend.
- Backend creates user + wallet if first login, otherwise returns existing user.
- Android stores backend `userId` for future API flows.

### What was implemented
1. Added backend entities/tables for `User`, `Wallet`, and `Transaction`.
2. Added backend auth bridge API: `POST /api/auth/phone-login`.
3. Added service logic to create-or-return user by phone and ensure wallet exists.
4. Added Android Retrofit API client and auth request/response models.
5. Replaced old local username/password login logic with Firebase Phone OTP flow in `LoginActivity`.
6. On OTP success, Android calls backend auth API and stores backend `userId` in local repository.
7. Startup navigation changed to login screen so old local sign-up flow is bypassed.

### Files changed and why

#### Backend files

1) `backend/src/main/java/com/arpit/walletbackend/user/AppUser.java`
- Added `AppUser` entity (`id`, `phone` unique).
- Why: canonical backend user identity by phone.

2) `backend/src/main/java/com/arpit/walletbackend/wallet/WalletAccount.java`
- Added one-wallet-per-user entity with `balance`.
- Why: every authenticated user must have a wallet record.

3) `backend/src/main/java/com/arpit/walletbackend/tx/WalletTransaction.java`
4) `backend/src/main/java/com/arpit/walletbackend/tx/TransactionStatus.java`
- Added transaction entity/status enum.
- Why: Phase 2 transfer lifecycle foundation.

5) `backend/src/main/java/com/arpit/walletbackend/user/AppUserRepository.java`
6) `backend/src/main/java/com/arpit/walletbackend/wallet/WalletAccountRepository.java`
7) `backend/src/main/java/com/arpit/walletbackend/tx/WalletTransactionRepository.java`
- Added JPA repositories.
- Why: persistence operations for user/wallet/transaction.

8) `backend/src/main/java/com/arpit/walletbackend/auth/dto/AuthBridgeRequest.java`
9) `backend/src/main/java/com/arpit/walletbackend/auth/dto/AuthBridgeResponse.java`
- Added request/response DTOs for phone login bridge.
- Why: clean API contract between Android and backend.

10) `backend/src/main/java/com/arpit/walletbackend/auth/AuthService.java`
- Added create-or-get-by-phone logic and wallet auto-provisioning.
- Why: deterministic backend identity onboarding flow.

11) `backend/src/main/java/com/arpit/walletbackend/auth/AuthController.java`
- Added `POST /api/auth/phone-login` endpoint.
- Why: Android needs backend-issued `userId` after OTP.

#### Android files

12) `app/src/main/java/com/arpit/myapplication/remote/ApiClient.java`
13) `app/src/main/java/com/arpit/myapplication/remote/BackendApi.java`
14) `app/src/main/java/com/arpit/myapplication/remote/model/AuthBridgeRequest.java`
15) `app/src/main/java/com/arpit/myapplication/remote/model/AuthBridgeResponse.java`
- Added Retrofit API layer.
- Why: connect Android auth flow to backend endpoint.

16) `app/src/main/java/com/arpit/myapplication/LoginActivity.java`
- Replaced local username/password logic with Firebase OTP and backend auth bridge call.
- Why: required multi-device login foundation.

17) `app/src/main/java/com/arpit/myapplication/UserRepository.java`
- Added backend `userId` persistence methods.
- Why: phase requirement to store backend user id after login.

18) `app/src/main/java/com/arpit/myapplication/MainActivity.java`
- Startup route changed from `SignInActivity` to `LoginActivity`.
- Why: ensures OTP flow is entry point.

19) `app/src/main/res/layout/activity_login.xml`
- Updated labels/inputs for phone + OTP.
- Why: align UI with OTP process.

20) `app/src/main/AndroidManifest.xml`
- Enabled cleartext traffic for emulator local backend (`10.0.2.2`) during development.
- Why: local backend URL currently uses HTTP.

### Build and verification performed
Command run:
- `./gradlew.bat :app:assembleDebug :backend:build`

Result:
- Build successful after Phase 1 changes.

### How to test Phase 1 locally

#### Backend checks
1. Start MySQL.
2. Run: `./gradlew.bat :backend:bootRun`
3. Verify health endpoint: `http://localhost:8080/api/health`
4. Test auth bridge endpoint:
  - `POST http://localhost:8080/api/auth/phone-login`
  - Body: `{"phone":"+911234567890"}`
  - Expect response with `userId`, `phone`, `balance`, `newlyCreated`.

#### Android OTP + backend checks
1. Ensure `google-services.json` is added in `app/` and Firebase Phone Auth is enabled in Firebase console.
2. Ensure backend is running locally (`:backend:bootRun`).
3. Launch app on emulator.
4. Enter phone and tap "Send OTP".
5. Enter OTP and tap "Verify OTP".
6. Expect navigation to dashboard on success.
7. Verify `backend_user_id` is stored by checking app session behavior on next launch.

### Build failures encountered and how they were solved
- No new build failure occurred at compile time in Phase 1.

---

## Phase 1 - Runtime Crash Fix

### Problem
After running the Phase 1 APK on a device/emulator the app crashed immediately on launch with:

```
java.lang.RuntimeException: Unable to start activity
  ComponentInfo{com.arpit.myapplication/com.arpit.myapplication.LoginActivity}:
  java.lang.IllegalStateException: Default FirebaseApp is not initialized in this
  process com.arpit.myapplication. Make sure to call FirebaseApp.initializeApp(Context) first.
    at com.google.firebase.auth.FirebaseAuth.getInstance(...)
    at com.arpit.myapplication.LoginActivity.onCreate(LoginActivity.java:55)
```

### Root cause
`FirebaseAuth.getInstance()` was called in `LoginActivity.onCreate()`.  Firebase auto-initialization
relies on resource strings (`google_app_id`, `default_web_client_id`, etc.) that are generated
by the **google-services Gradle plugin** from `google-services.json`.  Neither the plugin
nor the JSON file was present in the project, so Firebase had no configuration to read and
threw `IllegalStateException` before the first screen could appear.

The compile-time build passed because Firebase jars are present as dependencies; the crash
only happens at runtime when the SDK tries to look up the generated resources.

### What was changed

**`app/src/main/java/com/arpit/myapplication/LoginActivity.java`** — complete rewrite of login flow:
- Removed all `FirebaseAuth`, `PhoneAuthProvider`, `PhoneAuthCredential`, `PhoneAuthOptions`
  imports and fields.
- Replaced the two-step OTP flow (Send OTP → Verify OTP) with a **single-step direct backend
  login**: the user enters their phone number, the app calls `POST /api/auth/phone-login`
  on the Spring Boot backend, and on success navigates to `DashboardActivity`.
- Added `btnLogin.setEnabled(false/true)` guard to prevent double-taps during the network call.
- Added friendly Toast when the backend is unreachable ("Start the Spring Boot server…").
- Left `normalizePhone()` unchanged so 10-digit Indian numbers are still converted to E.164.
- Added `TODO (Phase 2)` comment explaining that Firebase OTP will be re-enabled once
  `google-services.json` is in place.

No other files needed changing for this fix; the Firebase BOM dependency can remain because
it is never invoked at runtime in this phase.

### How to get Firebase OTP working (Phase 2 prerequisite)
1. Go to [Firebase Console](https://console.firebase.google.com) → create a project (or use existing).
2. Add an Android app with package name `com.arpit.myapplication`.
3. Download `google-services.json` and place it at `app/google-services.json`.
4. Add the google-services plugin:
   - In `gradle/libs.versions.toml` under `[versions]` add: `googleServices = "4.4.2"`
   - In `gradle/libs.versions.toml` under `[plugins]` add: `google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }`
   - In root `build.gradle.kts` plugins block add: `alias(libs.plugins.google-services) apply false`
   - In `app/build.gradle.kts` plugins block add: `alias(libs.plugins.google-services)`
5. Enable Phone Authentication in Firebase Console → Authentication → Sign-in method.
6. Restore the OTP flow in `LoginActivity` using the `PhoneAuthProvider` code that was replaced.

### How to test this fix
1. Start the Spring Boot backend: `.\gradlew.bat :backend:bootRun`
2. Build and install the app: `.\gradlew.bat :app:installDebug`
3. Launch the app on the emulator — it should open `LoginActivity` without crashing.
4. Enter a 10-digit phone number and tap **Login**.
5. The app calls `http://10.0.2.2:8080/api/auth/phone-login`; on success it opens `DashboardActivity`.
6. If the backend is not running you will see the "Cannot reach backend" toast instead of a crash.

### Build verification
Command: `.\gradlew.bat :app:assembleDebug`  
Result: **BUILD SUCCESSFUL**

---

## Phase 1 - OTP Backend Reachability Fix (Physical Device)

### Problem
On signup, tapping **Send OTP** showed:
- `Cannot reach backend. Start the backend server and try again.`

### Root cause
Two things can cause this on a physical Android phone:
1. Backend process is not running on laptop (`localhost:8080` down).
2. App was using emulator-only host `10.0.2.2`, which does not work on a real device.

### What was changed
File changed:
- `app/src/main/java/com/arpit/myapplication/remote/ApiClient.java`

Fix applied:
1. Added runtime host selection:
  - Emulator -> `http://10.0.2.2:8080/api/`
  - Physical device -> `http://127.0.0.1:8080/api/`
2. For physical device debugging, enabled USB reverse tunnel:
  - `adb reverse tcp:8080 tcp:8080`
3. Started backend server:
  - `./gradlew.bat :backend:bootRun`

### Server status (latest check)
Status check command result:
- Health endpoint: `HEALTH_CHECK_FAILED`
- Port 8080 listener: `NO_LISTENER_ON_8080`

Interpretation:
- Backend is currently **not running** on this machine.

### How to start server again
1. Run: `./gradlew.bat :backend:bootRun`
2. Keep that terminal open.
3. On physical device, run once per reconnect: `adb reverse tcp:8080 tcp:8080`
4. Verify health: `http://localhost:8080/api/health`

### How to end server process cleanly
Preferred way:
1. Go to terminal running `:backend:bootRun`.
2. Press `Ctrl + C`.

If process is stuck:
1. Find PID on port 8080:
  - `netstat -ano | findstr :8080`
2. Kill it:
  - `taskkill /PID <PID> /F`

Optional cleanup for USB tunnel:
- `adb reverse --remove tcp:8080`

---

## Future Phase Documentation Format (Template)

Use this same structure for each next phase:

### Phase X - [Name]
- Goal
- What was implemented
- Files changed and why
- Build and verification performed
- How to test locally
- Risks / follow-ups

---

## Future Phases Status
- Phase 1 (Backend + Auth): pending
- Phase 2 (Core Fintech Flow): pending
- Phase 3 (Android Integration): pending
- Phase 4 (Architecture Upgrade): pending
- Phase 5 (Security + Reliability): pending
- Phase 6 (Polish + Testing): pending
