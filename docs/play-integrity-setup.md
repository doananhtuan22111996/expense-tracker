# Google Play Integrity API & Protected with Play Setup Guide

This document describes the configuration, architecture, and testing procedures for **Google Play Integrity API** ("Protected with Play") introduced in Expense Tracker v3.19.0.

---

## 1. Overview & Architecture

Expense Tracker implements a **Hybrid Client-Attestation Architecture** designed specifically for offline-first personal finance applications:
- **Standard Integrity API**: Fast attestation requests with warm-up token preparation (`PrepareIntegrityTokenRequest`) at app launch, minimizing attestation latency.
- **Offline Tolerance**: Offline users are **never locked out** of their financial records. If no network connectivity is present, verification is deferred gracefully (`IntegrityResult.isOfflineMode = true`).
- **Client Defense-in-Depth**:
  - Installer package verification (`com.android.vending`, `com.google.android.feedback`).
  - App signing certificate validation against production digests.
- **Remediation**:
  - Unverified installations surface a soft status in **Settings > Security > App Protection**.
  - In-app resolution triggers Google Play's remediation flow (`GET_LICENSED`), redirecting users safely to the official Play Store listing to acquire or verify their copy.

---

## 2. Google Cloud Platform & Play Console Configuration

### 2.1 Google Cloud Project
- **Project Name**: `expense-tracker-b5472`
- **Project Number**: `423754710458` (linked from `app/google-services.json`)
- **API Enabled**: **Google Play Integrity API**
  1. Navigate to [Google Cloud Console](https://console.cloud.google.com/apis/library/playintegrity.googleapis.com).
  2. Select project `expense-tracker-b5472` (`423754710458`).
  3. Click **Enable**.

### 2.2 Google Play Console Linking
1. Open the [Google Play Console](https://play.google.com/console).
2. Select **Expense Tracker** (`dev.tuandoan.expensetracker`).
3. In the left navigation menu, go to **Release** > **App integrity**.
4. Under the **Play Integrity API** tab, click **Link a Cloud project**.
5. Select existing project: **expense-tracker-b5472** (ID/Number: `423754710458`).
6. Confirm the link.

### 2.3 Play App Signing & Automatic Integrity Protection
1. Ensure **Play App Signing** is active. Google Play manages and protects your app's signing key.
2. In **App integrity** > **Automatic integrity protection**:
   - Enable **Automatic integrity protection** for production releases to add anti-tampering and anti-piracy protections into the Android App Bundle (AAB).
   - This provides binary protection alongside the runtime Play Integrity API attestation.

---

## 3. Implementation Details

### 3.1 Dependencies
```toml
# gradle/libs.versions.toml
playIntegrity = "1.6.0"
play-integrity = { group = "com.google.android.play", name = "integrity", version.ref = "playIntegrity" }
```

### 3.2 Core Components
- `AppIntegrityManager`: Domain interface in `dev.tuandoan.expensetracker.domain.security`.
- `PlayIntegrityManagerImpl`: Data implementation in `dev.tuandoan.expensetracker.data.security`.
  - Warmed up asynchronously in `ExpenseTrackerApplication.onCreate()`.
  - Standard integrity provider requested using GCP project `423754710458L`.
  - Uses direct `Runnable::run` dispatching for instantaneous task completion without coroutine main-thread looper stalls.
- `SettingsViewModel` & `SettingsScreen`: UI layer exposing `integrityState` StateFlow and rendering the App Protection row under the Security section.

---

## 4. Testing & Verification

### 4.1 Internal Test Track Attestation
1. Build release bundle: `./gradlew :app:bundleRelease`.
2. Upload to Google Play Console under **Testing** > **Internal testing**.
3. Install the build via Google Play on a physical test device with Google Play Services.
4. Open **Settings** > **Security** > **App Protection**:
   - Status should display **Protected with Google Play** with a green shield icon and checkmark.

### 4.2 Offline Deferral Testing
1. Turn on **Airplane Mode** (disable Wi-Fi and mobile data).
2. Launch Expense Tracker and navigate to **Settings** > **Security**.
3. Verify the App Protection row displays **Offline (verification deferred)**.
4. Verify that transaction recording, editing, and gold holdings remain 100% accessible.
5. Turn off Airplane Mode and tap the row: verification updates cleanly to genuine status.

### 4.3 Sideloaded / Unverified Testing
1. Install an unaligned APK directly via `adb install` or a third-party store.
2. Open **Settings** > **Security**:
   - App Protection displays **Unverified installation** with an alert icon and **Verify on Google Play** button.
3. Tap **Verify on Google Play**:
   - Play Store application opens to the official Expense Tracker store listing.

---

## 5. Security & Privacy Guarantees
- **No PII Transmission**: Play Integrity requests transmit only a cryptographically secure random nonce (`UUID.randomUUID()`) and Google Cloud Project number. No financial records, category names, or device identifiers are sent.
- **Strictly Non-Blocking**: Offline-first users are never locked out of their local Room database.
