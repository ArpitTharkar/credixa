# Server & Release APK connectivity notes

Date: 2026-05-07

## Problem (reported)
- When installing the release APK on a friend's phone (not connected by USB), pressing "Send OTP" shows "Cannot reach server". When the phone is connected to the PC via USB, it can reach the server (because `adb reverse` works). Without USB the app cannot connect.

## Root cause
- `adb reverse` only creates a tunnel from a USB-connected device to the host's `localhost`. It does not work over WiFi to a remote device. The app's `ApiClient` was pointing to `127.0.0.1:8081` (the device's own loopback) which only works with USB reverse tunneling.

## Fixes applied in this workspace
1. Determined the PC's LAN IP: `192.168.35.194`.
2. Updated `app/src/main/java/com/arpit/myapplication/remote/ApiClient.java`:
   - Replaced `DEVICE_BASE_URL` `http://127.0.0.1:8081/api/` with `http://192.168.35.194:8081/api/` for LAN testing.
   - Added comment reminding to change this to a proper cloud URL for production (e.g., `https://api.yourapp.com/api/`).
3. Opened Windows Firewall inbound rule for port `8081` so devices on the same WiFi can reach the backend:

```powershell
New-NetFirewallRule -DisplayName "WalletBackend-8081" -Direction Inbound -Protocol TCP -LocalPort 8081 -Action Allow
```

4. Rebuilt the debug APK and installed it on the device for testing:

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

5. (Related) For release builds we added ProGuard/R8 keep rules in `app/proguard-rules.pro` to preserve Retrofit/Gson DTOs so release APKs don't fail during serialization.

## Why USB-only worked previously
- While connected by USB you had run `adb reverse tcp:8081 tcp:8081` which maps the device's `127.0.0.1:8081` to the host machine's `localhost:8081`. That mapping is lost when the device is not connected to the PC.

## How to test on other phones (non-USB)
1. Ensure the phone is on the same WiFi network as your development PC.
2. Start the backend on your PC (use `start-server.ps1`):

```powershell
.\start-server.ps1
```

3. Make sure the server reports `Tomcat started on port 8081` and is reachable at `http://192.168.35.194:8081/api/`.
4. Install the debug APK or a release build that points to your LAN IP.

## Production / Play Store note
- Hardcoding a LAN IP is only for local testing. For Play Store releases you must host the backend at a public URL (cloud VM, PaaS, or other host) and change `DEVICE_BASE_URL` to that HTTPS URL, e.g. `https://api.yourapp.com/api/`.
- Recommended hosts: Railway, Render, Fly, or any VPS. After deployment, update `ApiClient` and rebuild the release APK.

## Quick recovery commands (summary)

Start backend (from project root):
```powershell
.\start-server.ps1
```

Open firewall (PowerShell admin):
```powershell
New-NetFirewallRule -DisplayName "WalletBackend-8081" -Direction Inbound -Protocol TCP -LocalPort 8081 -Action Allow
```

Rebuild debug APK and install on phone:
```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

If you still want to test via USB (dev convenience):
```powershell
adb reverse tcp:8081 tcp:8081
```

-- End of notes
