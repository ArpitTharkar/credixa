# Error Log and Fix Summary

Date: 2026-05-10
Project: MyApplication3

This file records all problems fixed today, why they happened, which files were changed, and how each problem was solved.

## 1. App could not reach backend because wrong LAN IP was configured

### Symptom
- Android app could not connect to backend from physical devices on Wi-Fi.
- Backend was running, but requests were still failing from device.

### Root cause
- The app was pointing to an old LAN IP address.
- Physical devices need the current PC LAN IP, not `127.0.0.1` or an outdated address.

### Files changed
- `app/build.gradle.kts`
  - Updated `buildTypes.debug.SERVER_BASE_URL`
  - Updated `buildTypes.release.SERVER_BASE_URL`

### Fix
- Replaced old backend base URL with the detected local network IP:
  - `http://192.168.1.14:8081/api/`
- This allowed devices on the same Wi-Fi to call the backend.

### How to solve this in future
- Check the current PC Wi-Fi IP before testing on real devices.
- Update `SERVER_BASE_URL` when the local network changes.
- Use a public backend URL for production instead of a LAN IP.

---

## 2. Android blocked HTTP requests with cleartext error

### Symptom
- App showed:
  - `Cannot reach server: CLEARTEXT communication to 192.168.1.14 not permitted by network security policy`

### Root cause
- Android 9+ blocks plain HTTP by default.
- The manifest and network security config explicitly disabled cleartext traffic.

### Files changed
- `app/src/main/AndroidManifest.xml`
  - Changed `android:usesCleartextTraffic` from `false` to `true`
- `app/src/main/res/xml/network_security_config.xml`
  - Added allowed local development hosts/IPs for cleartext traffic

### Fix
- Enabled cleartext traffic for local development.
- Allowed these local hosts/IPs:
  - `10.0.2.2`
  - `127.0.0.1`
  - `localhost`
  - `192.168.1.14`

### How to solve this in future
- For local testing, allow HTTP only for local IPs.
- For production, use HTTPS and remove cleartext exceptions.

---

## 3. Payment between two devices failed because receiver phone was treated as backend user ID

### Symptom
- Sending money across devices failed even though server was running.
- Entering a phone number in Send Money caused transfer failure.

### Root cause
- `SendMoneyActivity` accepted phone/user ID text.
- `WalletViewModel.sendMoney()` tried to parse the input directly as backend numeric user ID.
- A phone number such as `9822445222` was incorrectly treated as backend user ID `9822445222`.

### Files changed
- `app/src/main/java/com/arpit/myapplication/WalletViewModel.java`
  - Updated `sendMoney()`
- `app/src/main/java/com/arpit/myapplication/WalletRepository.java`
  - Added receiver resolve flow
- `app/src/main/java/com/arpit/myapplication/remote/BackendApi.java`
  - Added user resolve endpoint
- `app/src/main/java/com/arpit/myapplication/remote/model/ResolveUserResponse.java`
  - Added response model
- `backend/src/main/java/com/arpit/walletbackend/api/UserController.java`
  - Added `/api/users/resolve`
- `backend/src/main/java/com/arpit/walletbackend/user/AppUserRepository.java`
  - Added repository lookup support

### Fix
- Added `/api/users/resolve?identifier=...` in backend.
- App now resolves receiver first by:
  - phone number
  - username
  - backend user ID
- After resolve succeeds, the app transfers using the real backend `userId`.

### How to solve this in future
- Never treat free-form receiver input directly as backend ID.
- Always resolve user input to a canonical backend identity first.

---

## 4. Payment failed when receiver account did not exist yet

### Symptom
- Logs showed:
  - `GET /api/users/resolve?identifier=9822445222 -> 404`
- Transfer stopped before execution.

### Root cause
- The receiver phone number was not yet present in backend user data.
- Resolve step failed, so transfer could not continue.

### Files changed
- `app/src/main/java/com/arpit/myapplication/WalletRepository.java`
  - Added fallback logic after resolve `404`
- `app/src/main/java/com/arpit/myapplication/remote/model/AuthBridgeRequest.java`
  - Reused for phone-login fallback

### Fix
- If `/api/users/resolve` returns `404` and input looks like a phone number:
  - app now calls `/api/auth/phone-login`
  - backend creates or returns the receiver account
  - transfer continues using the returned user ID

### How to solve this in future
- Use a create-or-get identity flow for phone-based users.
- Avoid forcing receiver registration before transfer if the product should support phone-first transfers.

---

## 5. Money transfer succeeded but receiver device still showed old balance

### Symptom
- Transfer completed successfully.
- Sender balance changed correctly.
- Receiver device still showed the same balance until reopening screens or logging in again.

### Root cause
- Balance screen only refreshed once in `onCreate()`.
- Receiver UI was not re-fetching balance when screen reopened.

### Files changed
- `app/src/main/java/com/arpit/myapplication/CheckBalanceActivity.java`
  - Added refresh in `onResume()`

### Fix
- Balance now refreshes whenever the screen resumes.
- This allows receiver device to fetch the latest backend balance after incoming transfers.

### How to solve this in future
- Any screen showing server state should refresh in `onResume()` or via periodic polling / push updates.

---

## 6. Receiver transaction history showed wrong type or did not refresh properly

### Symptom
- Receiver got the money correctly.
- History was not always refreshed.
- Incoming transaction could appear incorrectly.

### Root cause
- Transaction screen only loaded history in `onCreate()`.
- No refresh happened after returning to the screen.

### Files changed
- `app/src/main/java/com/arpit/myapplication/TransactionsActivity.java`
  - Added refresh in `onResume()`

### Fix
- Transaction list now refreshes every time the screen resumes.

### How to solve this in future
- Refresh server-backed lists on resume when data can change from another device.

---

## 7. Receiver account mismatch caused balance to update on wrong backend user

### Symptom
- Transfer succeeded, but the balance increase did not appear on the expected receiving device/account.

### Root cause
- Phone normalization was inconsistent:
  - signup stored `+91XXXXXXXXXX`
  - fallback receiver creation sometimes used `+XXXXXXXXXX`
- This could create or resolve a different backend account for the same person.

### Files changed
- `backend/src/main/java/com/arpit/walletbackend/auth/AuthService.java`
  - Updated `normalizePhone()`
- `backend/src/main/java/com/arpit/walletbackend/api/UserController.java`
  - Updated `normalizePhone()`
- `app/src/main/java/com/arpit/myapplication/WalletRepository.java`
  - Updated `normalizePhone()`

### Fix
- Standardized 10-digit phone numbers to India format:
  - `+91XXXXXXXXXX`
- Both registration and transfer fallback now resolve the same user identity.

### How to solve this in future
- Normalize phone numbers in one consistent format everywhere.
- Ideally use E.164 format across app and backend.

---

## 8. Transaction history showed wrong sign/color for received money

### Symptom
- Receiver balance increased correctly.
- Transactions screen still sometimes showed received money as red `-200 Sent` instead of green `+200 Received`.

### Root cause
- Transaction direction was inferred on the client.
- Client-side mapping can become inconsistent if stale data or parsing mismatch happens.

### Files changed
- `backend/src/main/java/com/arpit/walletbackend/api/TransactionController.java`
  - Changed response mapping for transaction history
- `backend/src/main/java/com/arpit/walletbackend/api/TransactionView.java`
  - Added direction-aware response view
- `app/src/main/java/com/arpit/myapplication/remote/model/TransactionDto.java`
  - Added `direction`
- `app/src/main/java/com/arpit/myapplication/WalletRepository.java`
  - Prefer backend `direction` field when mapping transactions

### Fix
- Backend now computes transaction direction explicitly:
  - `ADD`
  - `SEND`
  - `RECEIVED`
- App uses backend `direction` first, instead of relying only on local inference.

### How to solve this in future
- Compute transaction direction on the server when possible.
- Send presentation-safe fields for anything critical to business meaning.

---

## 9. Transaction timestamps in app did not use backend time

### Symptom
- Transaction list used current device time instead of actual backend transaction time.

### Root cause
- App was building transaction rows using `System.currentTimeMillis()` instead of backend `createdAt`.

### Files changed
- `app/src/main/java/com/arpit/myapplication/WalletRepository.java`
  - Updated transaction mapping in `getTransactionsAsync()`

### Fix
- Parse backend `createdAt` and convert it to epoch milliseconds.
- Fallback to current time only if parsing fails.

### How to solve this in future
- Use backend timestamps for transaction history.
- Avoid generating fake event times on the client for server-generated records.

---

## 10. Spending Tracker page always showed zero for weekly/monthly totals

### Symptom
- Tracking page opened, but showed:
  - `Total spending: ₹0`
  - empty weekly/monthly data
- Even after successful transfers.

### Root cause
- `TrackingActivity` was reading old local transaction storage using:
  - `repo.getTransactions(user)`
- Real transaction data now comes from backend via `WalletViewModel.transactions`.
- So tracker was calculating from stale/empty local data.

### Files changed
- `app/src/main/java/com/arpit/myapplication/TrackingActivity.java`
  - Reworked data flow to use live backend-backed transactions

### Fix
- `TrackingActivity` now:
  - observes `walletViewModel.transactions`
  - calls `walletViewModel.refreshTransactions()`
  - recalculates weekly/monthly totals from backend data
  - refreshes again in `onResume()`
- Tracker now counts only successful `SEND` transactions as spending.

### How to solve this in future
- Do not mix old local storage with backend-synced features.
- Use one source of truth for transaction history.

---

## 11. Backend startup and local device routing support

### Symptom
- Backend needed to be started repeatedly during testing.
- Physical devices required correct routing to local machine.

### Files involved
- `start-server.ps1`
  - Used to stop old server, build jar if needed, start backend, and run `adb reverse`

### What was done
- Started backend in foreground and detached mode during debugging.
- Used `adb reverse tcp:8081 tcp:8081` for USB-connected device testing.
- Confirmed backend health endpoint was up.

### Important note
- `adb reverse` only helps USB-connected devices.
- For Wi-Fi devices, app must use the LAN IP and firewall must allow inbound traffic.

---

## Quick list of changed files today

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/main/java/com/arpit/myapplication/WalletRepository.java`
- `app/src/main/java/com/arpit/myapplication/WalletViewModel.java`
- `app/src/main/java/com/arpit/myapplication/CheckBalanceActivity.java`
- `app/src/main/java/com/arpit/myapplication/TransactionsActivity.java`
- `app/src/main/java/com/arpit/myapplication/TrackingActivity.java`
- `app/src/main/java/com/arpit/myapplication/remote/BackendApi.java`
- `app/src/main/java/com/arpit/myapplication/remote/model/ResolveUserResponse.java`
- `app/src/main/java/com/arpit/myapplication/remote/model/TransactionDto.java`
- `backend/src/main/java/com/arpit/walletbackend/api/UserController.java`
- `backend/src/main/java/com/arpit/walletbackend/api/TransactionController.java`
- `backend/src/main/java/com/arpit/walletbackend/api/TransactionView.java`
- `backend/src/main/java/com/arpit/walletbackend/auth/AuthService.java`
- `backend/src/main/java/com/arpit/walletbackend/user/AppUserRepository.java`

---

## Final summary

Main themes of today’s fixes:
- Local device networking and HTTP policy issues
- Receiver identity resolution between devices
- Correct backend account mapping through phone normalization
- Live balance/transaction refresh across devices
- Correct transaction direction and timestamps
- Spending Tracker using backend transaction data instead of old local storage

If new issues appear later, append them below this file with:
- symptom
- root cause
- changed files
- exact fix
- prevention note
