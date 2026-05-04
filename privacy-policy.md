# Privacy Policy

**Last updated:** 2026-05-04

## 1. Introduction

This Privacy Policy describes how **Expense Tracker** (“the app”) handles user data.

Expense Tracker is an **offline-first personal expense tracking application**.
We are committed to protecting your privacy, being transparent about our data practices, and giving you control over what (if anything) leaves your device.

---

## 2. Data Collection

**Expense Tracker does not collect any personal or sensitive data by default.** Your financial data stays on your device and is never transmitted.

Specifically, the app does **not** collect:
- Personal information (such as name, email address, or phone number)
- Financial account or banking information
- Location data
- Persistent device identifiers
- Advertising identifiers

The app does **not** display advertisements and does **not** share data with advertisers.

Starting with version 3.11.0, the app includes two **independent, opt-in** Firebase features: crash reporting and anonymous usage analytics. Both are **off by default** and only activate if you explicitly opt in to each one. Opting in to one does **not** imply opting in to the other.

### Optional anonymous crash reporting

The app includes **Firebase Crashlytics** for crash reporting. Crash collection is **off by default** and only activates if you explicitly opt in:

- **When you are asked:** once, via a clearly-worded dialog after you finish onboarding. Declining is a first-class choice — there is no pre-checked box and no "skip" button.
- **Where you can change your answer:** at any time from **Settings → Share anonymous crash reports**.
- **What is collected when enabled:** automatically-captured crash stack traces and device OS/hardware metadata that Crashlytics attaches by default (Android version, device model, app version). No user identifiers are attached.
- **What is never collected, regardless of your choice:** your transactions, transaction amounts, category names, notes, any text you've entered into the app, or any persistent identifier tied to you personally.
- **Data retention:** crash reports are retained for **90 days** (Firebase Crashlytics free-tier default), then automatically deleted.
- **Data processor:** Google LLC (Firebase). See [Firebase's privacy documentation](https://firebase.google.com/support/privacy) for their handling of this data.

### Optional anonymous usage analytics

The app also includes **Firebase Analytics** to help me understand which features are actually used, so I can prioritize future improvements. Analytics event collection is **off by default** and only activates if you explicitly opt in:

- **When you are asked:** alongside the crash-reporting question in the post-onboarding dialog, as a separate checkbox. Accepting crash reports does not auto-accept analytics — each choice is independent. Declining is a first-class choice.
- **Where you can change your answer:** at any time from **Settings → Share anonymous usage data**.
- **What is collected when enabled:** a fixed, documented set of feature-usage events with minimal parameters:
  - `app_open` (build type: debug or release)
  - `onboarding_completed`
  - `widget_added` (size: small or medium), `widget_removed`
  - `transaction_added` (type: expense or income — **no amount, no category, no note**)
  - `backup_exported` / `backup_imported` (format: json or encrypted)
  - `insight_shown` (which Insights row surfaced: biggest mover / daily pace / no-budget fallback / day-of-month)
- **What is never collected, regardless of your choice:** transaction amounts, category names, note text, dates of activity, currency codes that could correlate to your location, or any free-text you've entered. Firebase Analytics' automatic events (`screen_view`, `first_open`, `session_start`) are also **disabled** — only the events listed above ever fire. No user ID is ever set.
- **Data retention:** event-level data is retained for **14 months** (Google Analytics 4 default), then automatically deleted.
- **Data processor:** Google LLC (Firebase / Google Analytics). See [Firebase's privacy documentation](https://firebase.google.com/support/privacy) and [Google Analytics data retention](https://support.google.com/analytics/answer/7667196) for their handling of this data.

When both toggles are disabled (the default), no data leaves your device via Firebase.

---

## 3. Data Storage and Processing

All data entered into the app (income and expense records, categories, budgets, gold holdings, etc.) is:

- Stored **locally on your device only**
- Processed entirely **on-device**
- **Never transmitted** to any external server
- **Never shared** with any third party

The app works fully offline. Backups are written to the location you choose on your device; the app does not upload them anywhere.

The **only exceptions** to the "no transmission" rule are the two opt-in Firebase features described in Section 2 — crash reports and anonymous usage analytics — each of which is transmitted only when you have explicitly enabled its toggle in Settings.

---

## 4. Data Sharing

Apart from the two optional, opt-in Firebase features described in Section 2 — anonymous crash reports and anonymous usage analytics — Expense Tracker does **not share any data** with third parties. No user data is sent outside of your device.

---

## 5. Permissions

The app does **not request any sensitive device permissions**.

Any optional system access (such as file access for manual backup or export, if available) is:
- Explicitly initiated by the user
- Used only for the requested action
- Never performed automatically

---

## 6. Children’s Privacy

Expense Tracker does not knowingly collect data from children under the age of 13.

The app has no sign-in flow, asks for no identifying information, and collects no personal data by default. Both optional Firebase features described in Section 2 (crash reporting and anonymous usage analytics) are **off by default**, contain **no user identifiers**, and cannot be tied back to a specific individual — adult or child. No child-specific data is ever collected or retained.

---

## 7. Changes to This Privacy Policy

This Privacy Policy may be updated if the app’s functionality changes. Any updates will be published at this same URL.

### Change log

- **2026-05-04** (v3.11.0): Added Section 2 disclosures for two optional, opt-in Firebase features — anonymous crash reporting (Firebase Crashlytics) and anonymous usage analytics (Firebase Analytics). Both are independent toggles and both remain **off by default**. No change to default privacy posture — no data leaves your device unless you explicitly opt in to one or both.

---

## 8. Contact Information

If you have any questions or concerns about this Privacy Policy, please contact:

**Email:** doananhtuan22111996@gmail.com
