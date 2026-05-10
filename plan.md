# Fintech Project Upgrade Plan

## PHASE 0 — FOUNDATION (KEEP IT LIGHT)
**Goal: Project runs cleanly**

### Do only this:
- Keep existing Java codebase (do NOT switch to Kotlin now)
- Add Retrofit dependency in [app/build.gradle.kts](app/build.gradle.kts)
- Add Firebase Auth (Phone OTP) dependency and google-services plugin
- Set up Spring Boot backend project with MySQL database

### Skip for now (these are Phase 4+ topics):
- KSP
- Hilt / any DI framework
- WorkManager
- LeakCanary
- Performance and profiling tools

---

## PHASE 1 — BACKEND + AUTH (MOST IMPORTANT)
**Goal: Multi-device system**

### Backend (Spring Boot + MySQL):
- Create `User` table: id, phone
- Create `Wallet` table: userId, balance
- Create `Transaction` table: id, senderId, receiverId, amount, status, createdAt

### Auth flow:
```
Phone → OTP → Firebase success
  → send phone + Firebase ID token to backend
      → backend verifies token, creates/returns User + Wallet
          → Android stores userId for all future API calls
```

### Android:
- Replace [LoginActivity.java](app/src/main/java/com/arpit/myapplication/LoginActivity.java) and [SignInActivity.java](app/src/main/java/com/arpit/myapplication/SignInActivity.java) with Firebase OTP flow
- Replace [UserRepository.java](app/src/main/java/com/arpit/myapplication/UserRepository.java) local SharedPreferences logic with backend auth call
- Store backend userId and session token securely after login

---

## PHASE 2 — CORE FINTECH FLOW
**Goal: Real transaction system**

### Build backend APIs:
- `GET /wallet` → return balance for logged-in user
- `POST /wallet/add-money` → credit amount to wallet
- `POST /transfer` → debit sender, credit receiver
- `GET /transactions` → return history for logged-in user

### Important fintech concepts to implement:
- **Transaction lifecycle**: INITIATED → SUCCESS / FAILED
- **Idempotency**: each transfer request carries a unique key; duplicate requests are rejected at DB level (unique constraint on idempotency key column)
- **Atomic transfer**: deduct from sender and add to receiver inside a single DB transaction — if any step fails, the whole transfer rolls back

---

## Recent Build & Implementation Notes (automated change log)

**What I did:**
- Built the Android release APK and implemented Phase 2 backend endpoints (wallet, add-money, transfer, transactions) with idempotency and atomic transfers.

**Why I did it:**
- To enable end-to-end testing of the fintech flows on physical devices and to secure the backend with transaction lifecycle and idempotency guarantees.

**Files changed / added:**
- `backend/src/main/java/com/arpit/walletbackend/tx/WalletTransaction.java` — added `idempotencyKey` column to track unique transfer attempts.
- `backend/src/main/java/com/arpit/walletbackend/tx/WalletTransactionRepository.java` — added lookup methods `findByIdempotencyKey(...)` and `findBySenderUserIdOrReceiverUserIdOrderByCreatedAtDesc(...)`.
- `backend/src/main/java/com/arpit/walletbackend/wallet/WalletService.java` — **new** service implementing `addMoney()`, `transfer()` with transactional, atomic updates and idempotency handling, and `historyForUser()`.
- `backend/src/main/java/com/arpit/walletbackend/api/WalletController.java` — **new** endpoints: `GET /api/wallet?userId=...`, `POST /api/wallet/add-money`.
- `backend/src/main/java/com/arpit/walletbackend/api/TransferController.java` — **new** endpoint: `POST /api/transfer` (body: `senderUserId`, `receiverUserId`, `amount`, `idempotencyKey`).
- `backend/src/main/java/com/arpit/walletbackend/api/TransactionController.java` — **new** endpoint: `GET /api/transactions?userId=...`.
- `app/build.gradle.kts` — updated `BuildConfig.SERVER_BASE_URL` to the PC LAN IP used for local device testing (example: `http://192.168.202.194:8081/api/`).

**APK produced:**
- `app/build/outputs/apk/release/app-release.apk` — built successfully.

**How to test locally:**
1. Ensure your phone is on the same LAN as the dev PC (same Wi‑Fi network) or use USB + `adb reverse tcp:8081 tcp:8081`.
2. Install the release APK on the phone:
```
adb install -r app/build/outputs/apk/release/app-release.apk
```
3. Verify backend reachability from the phone with:
```
http://<PC_IP>:8081/api/health
```

**Notes / Next steps:**
- If the phone still cannot reach the PC, ensure the Spring Boot server binds to all interfaces (`server.address=0.0.0.0`) and Windows Firewall allows inbound port `8081` on the correct network profile.
- Add automated integration tests for idempotency and insufficient-funds scenarios.

### Android Integration Changes (Phase 3) — summary of what I changed

- What I did: Replaced local SharedPreferences wallet logic with backend-powered Retrofit calls and updated the ViewModel to use the backend user id and async network calls. Added DTOs and extended `BackendApi` with wallet endpoints so the Android app talks to the new Phase 2 backend.

- Why I did it: To move all wallet state and transfer logic off-device so two physical devices can see consistent balances and transaction history (multi-device flows), and to validate the backend APIs end-to-end from the real app.

- Files changed / added (workspace-relative):
  - `app/src/main/java/com/arpit/myapplication/WalletRepository.java` — replaced with a Retrofit-backed implementation exposing async methods: `getBalanceAsync`, `addMoneyAsync`, `transferAsync`, `getTransactionsAsync`. Kept backwards-compatible SharedPreferences helpers used by split/debt UI features.
  - `app/src/main/java/com/arpit/myapplication/WalletViewModel.java` — switched to use `UserRepository.getBackendUserId()` and invoke the async `WalletRepository` methods; updates LiveData on network responses.
  - `app/src/main/java/com/arpit/myapplication/remote/BackendApi.java` — added wallet endpoints: `GET /wallet`, `POST /wallet/add-money`, `POST /transfer`, `GET /transactions`.
  - `app/src/main/java/com/arpit/myapplication/remote/model/*` — added DTOs: `WalletResponse`, `AddMoneyRequest`, `TransferRequest`, `TransferResponse`, `TransactionDto`.
  - `app/src/main/java/com/arpit/myapplication/remote/ApiClient.java` — already present and used; left `resolveBaseUrl()` logic intact to select emulator vs device.

- APKs built:
  - Debug: `app/build/outputs/apk/debug/app-debug.apk` (built during integration step)
  - Release: `app/build/outputs/apk/release/app-release.apk` (built earlier)

- How to test from your phone:
  1. Ensure your phone is on the same LAN as the dev PC or use USB + `adb reverse tcp:8081 tcp:8081`.
 2. Install the debug APK and open the wallet screens.
 3. Use the UI to call add-money / send-money flows; or call the endpoints directly:
```
POST http://<PC_IP>:8081/api/wallet/add-money?userId=<id>
  body: { "amount": 100 }

POST http://<PC_IP>:8081/api/transfer
  body: { "senderUserId":1, "receiverUserId":2, "amount": 50, "idempotencyKey": "uuid" }

GET  http://<PC_IP>:8081/api/transactions?userId=1
```

If you want, I can now:
- Install the debug APK onto your phone (`adb install -r app/build/outputs/apk/debug/app-debug.apk`) and run a quick UI smoke test; or
- Run an automated smoke test from this machine hitting the backend endpoints (add-money → transfer → transactions) to demonstrate idempotency and atomic transfer behavior.

### Recent: Manual DI wiring (what / why / files / APK)

- **What I did:** Wired a minimal manual dependency injection system so the app uses singleton repositories and API instances provided at startup instead of constructing them everywhere.
- **Why I did it:** Prepares the app for Phase 4 (DI and cleaner architecture), reduces coupling, simplifies testing and swapping implementations, and avoids adding new Gradle dependencies.
- **Files changed / added:**
  - `app/src/main/java/com/arpit/myapplication/ServiceLocator.java` — **new**: provides singletons for `BackendApi`, `WalletRepository`, and `UserRepository`.
  - `app/src/main/java/com/arpit/myapplication/MyApplication.java` — **new**: Application subclass that initializes the `ServiceLocator` on startup.
  - `app/src/main/AndroidManifest.xml` — **modified**: registered `MyApplication` (`android:name=".MyApplication"`).
  - `app/src/main/java/com/arpit/myapplication/WalletViewModel.java` — **modified**: now obtains `WalletRepository` and `UserRepository` from `ServiceLocator` instead of `new`.
- **What changed:** Repository construction is centralized; `WalletViewModel` and future ViewModels can request dependencies from `ServiceLocator` for easier testing and incremental refactors.
- **APK(s):** Rebuilt the debug APK successfully: `app/build/outputs/apk/debug/app-debug.apk`. (Release APK was already produced earlier.)
- **Next steps:** Replace remaining direct `new` constructions across activities and other ViewModels, add `CompletableFuture`-returning adapters in `WalletRepository`, and migrate `WalletViewModel` to use futures → LiveData bridging. I can proceed with these next.

### Recent: Phase 5 — Android changes (what / why / files / APK / errors & fixes)

- **What I did:** Enforced network security (block cleartext by default), added a token `Interceptor` to attach `Authorization: Bearer <token>`, wired token storage in `UserRepository`, implemented client-side retry for transfers using the same idempotency key, and moved keystore passwords to `local.properties` with a sample file.
- **Why I did it:** Enforce HTTPS-only behavior, introduce token-based auth, make transfers resilient to transient network errors while relying on server-side idempotency, and avoid committing keystore secrets.
- **Files changed / added:**
  - [app/src/main/res/xml/network_security_config.xml](app/src/main/res/xml/network_security_config.xml) — **new** (blocks cleartext by default).
  - [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) — **modified** (references network config, disables cleartext).
  - [app/src/main/java/com/arpit/myapplication/remote/TokenInterceptor.java](app/src/main/java/com/arpit/myapplication/remote/TokenInterceptor.java) — **new** (adds Authorization header).
  - [app/src/main/java/com/arpit/myapplication/remote/ApiClient.java](app/src/main/java/com/arpit/myapplication/remote/ApiClient.java) — **modified** (wires interceptor + logging client + timeouts).
  - [app/src/main/java/com/arpit/myapplication/UserRepository.java](app/src/main/java/com/arpit/myapplication/UserRepository.java) — **modified** (session token getters/setters).
  - [app/src/main/java/com/arpit/myapplication/WalletRepository.java](app/src/main/java/com/arpit/myapplication/WalletRepository.java) — **modified** (retry-on-network-failure using same idempotency key).
  - [app/build.gradle.kts](app/build.gradle.kts) — **modified** (read release keystore props from `local.properties`).
  - [local.properties.sample](local.properties.sample) — **new** (example to avoid committing secrets).
- **APK(s):** Rebuilt debug APK successfully: [app/build/outputs/apk/debug/app-debug.apk](app/build/outputs/apk/debug/app-debug.apk). (Release APK available from earlier step.)
- **Errors encountered & fixes:**
  - Build error in `app/build.gradle.kts`: "Unresolved reference 'util' / 'io'" when loading `java.util.Properties` and `java.io.FileInputStream` from the Kotlin DSL. Fix: added `import java.util.Properties` and `import java.io.FileInputStream` at the top of `build.gradle.kts` and used the imported names.
  - Manifest merge error: duplicate `usesCleartextTraffic` attribute caused `processDebugMainManifest` failure. Fix: removed the duplicate attribute so the manifest references the network security config only once.
- **Verification:** Ran `.\gradlew :app:assembleDebug` — build succeeded after the fixes.
- **Remaining work (backend):** I did not implement the server-side token-exchange endpoint or detailed transaction event logging; these remain TODOs (see Phase 5 backend items).


## PHASE 3 — ANDROID INTEGRATION
**Goal: Replace all local logic with backend**

### Replace:
- [WalletRepository.java](app/src/main/java/com/arpit/myapplication/WalletRepository.java) SharedPreferences wallet logic → Retrofit API calls
- [WalletViewModel.java](app/src/main/java/com/arpit/myapplication/WalletViewModel.java) local transfer logic → ViewModel calls repository which calls API
- All balance reads → from `/wallet` API response

### Keep architecture simple for now:
- MVVM: Activity → ViewModel → Repository → Retrofit
- LiveData still fine here

### Optional in this phase:
- Add Room database for caching transaction list (only if offline UX is needed)

---

## PHASE 4 — UPGRADE ARCHITECTURE
**Goal: Make it interview-strong**

### Add gradually (do not rush):
1. **DI** — Add Hilt or manual dependency injection for cleaner code structure
2. **Coroutines + Flow** — Replace callback/LiveData with coroutines for async work; use StateFlow for screen state
3. **Repository pattern** — Formalize the data layer separation from ViewModel

> After this phase your app reflects 2+ years of real-world Android experience

---

## PHASE 5 — SECURITY + RELIABILITY
**Goal: Real fintech behavior**

### Add:
- **HTTPS only** — enforce TLS in network security config, no cleartext traffic allowed
- **Token-based auth** — send Authorization header (Bearer token) on every API call
- **Retry logic** — if a transfer fails due to network, retry with the same idempotency key
- **Transaction logging** — all events stored in backend DB with status and timestamp
- **Secret hardening** — remove hardcoded keystore passwords from [app/build.gradle.kts](app/build.gradle.kts); load from local.properties

### Actionable tasks (Phase 5)

- **Android — Enforce HTTPS**: add `network_security_config.xml` to require TLS and remove `usesCleartextTraffic` or restrict it to debug builds only. Update `ApiClient` base URL to `https://` when testing with TLS (or use a dev reverse proxy / self-signed cert installed on test devices).
- **Android — Token interceptor**: add an OkHttp `Interceptor` that reads the stored session/token from `UserRepository` and sets `Authorization: Bearer <token>` on every request; attach it in `ApiClient` Retrofit builder.
- **Android — Retry + idempotency**: implement retry-on-network-failure logic in `WalletRepository.transfer` that resends the same `idempotencyKey` up to N times with exponential backoff (only for idempotent-safe failures, not for HTTP 4xx).
- **Backend — Transaction logging**: ensure `WalletTransaction` records lifecycle states (INITIATED, SUCCESS, FAILED) and timestamps; add a `transaction_events` table or embed events in the transaction entity, and expose logs on admin-only endpoints.
- **Secret hardening**: remove any hardcoded keystore passwords from `app/build.gradle.kts` and read them from `local.properties` using gradle property lookups; document how to populate `local.properties` during developer setup.

### Implementation notes

- Use HTTPS locally for Phase 5 testing by either: 1) generating a dev certificate and installing it on devices, or 2) running an SSL-terminating reverse proxy (nginx) on the dev machine and forwarding to the Spring Boot app. Update `start-server.ps1` docs accordingly.
- Token flow: after Firebase OTP verification, send the Firebase ID token to the backend to exchange for a backend session token; store token in `UserRepository` (encrypted/sharedpref) and expire/refresh as required.
- Retry policy: implement retries in the client for network errors (IOException) only; rely on server-side idempotency key uniqueness to prevent duplicates.
- Secrets: add an example `local.properties.sample` (do NOT commit real secrets) and update README with instructions.


---

## PHASE 6 — POLISH + TESTING
**Goal: Make it production-like and demo-ready**

### Test scenarios:
- Device A registers and logs in → adds money → sends to Device B
- Device B logs in → receives money → balance and transaction history both update correctly

### Fix:
- UI loading states (show spinner while API call is in flight)
- Error states (show message on transfer failure)
- Edge cases (send to self, send more than balance, network timeout)

### Add regression tests:
- Unit test for transfer validation rules
- Unit test for idempotency key uniqueness behavior
- Integration test for OTP → backend user creation flow

---

## FINAL STRUCTURE (SIMPLE AND CORRECT)

| Phase | Focus |
|-------|-------|
| Phase 0 | Basic setup — Retrofit, Firebase, Spring Boot |
| Phase 1 | Auth + Backend — OTP login, user/wallet creation |
| Phase 2 | Transfer logic — lifecycle, idempotency, atomicity |
| Phase 3 | Android integration — replace local storage with APIs |
| Phase 4 | Architecture upgrade — DI, Flow, clean repository |
| Phase 5 | Security + retry — HTTPS, tokens, logging |
| Phase 6 | Testing — 2-device E2E, edge cases, UI polish |

---

## INTERVIEW PARAGRAPH

"I built a multi-device digital wallet Android app with a Spring Boot + MySQL backend. I started with the core wallet logic — login, add money, send money, and transaction history — running locally to validate the product idea. Then I migrated to a real backend by integrating Firebase phone OTP for cross-device identity, building REST APIs with Retrofit for all financial operations, and implementing proper transaction handling with lifecycle states (INITIATED → SUCCESS/FAILED), idempotency keys to prevent double payments, and atomic database transactions so no partial transfers can occur. After the core system was stable, I upgraded the Android architecture with the MVVM pattern, Hilt for dependency injection, and Kotlin coroutines with Flow for clean async handling. Finally I hardened the app with HTTPS-only networking, token-based auth, and retry logic for reliability. The result is a system where two users on different devices can transfer money between each other with consistent balances and a full transaction audit trail."
