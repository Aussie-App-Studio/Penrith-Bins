# Privacy Policy for Penrith Bins

**Last updated:** October 20, 2026

This Privacy Policy applies to the **Penrith Bins** mobile application ("the App"), developed and maintained by **Aussie App Studio**. Penrith Bins is an independent community utility created for residents of the Penrith City Council area to view bin collection dates, receive bin reminders, and look up local waste services.

---

### 1. Information Collection and Processing

- **No Personal Account Required:** The App does not require account creation, registration, or login. We do not collect your name, email address, phone number, or passwords.
- **Local Storage Only:** Your selected property/street address, custom notification reminder times, bin overrides, and chosen app icon are stored **100% locally on your device** (via Android SharedPreferences). We do not operate external databases, user-tracking servers, or cloud storage for your personal data.
- **No Third-Party Analytics or Advertising:** The App contains **no advertisements, no marketing trackers, and no third-party analytics SDKs** (such as Firebase Analytics, Facebook SDK, or Google Analytics).

---

### 2. Device Permissions and Usage

The App requests the following device permissions solely to provide core features:

- **Location (ACCESS_COARSE_LOCATION / ACCESS_FINE_LOCATION):**
  - **Purpose:** Used optionally when you tap "Use Current GPS Location" in the address picker to help detect your nearest street or suburb within the Penrith local government area.
  - **How it is handled:** Location coordinates are reverse-geocoded on-device using the standard Android system Geocoder. Your location is **never tracked, never logged, and never transmitted to our servers or third parties**. You can use the app without granting location permissions by searching or selecting your street manually.
- **Notifications (POST_NOTIFICATIONS):**
  - **Purpose:** Used on Android 13+ to deliver local notification reminders for bin night (evening before, morning of collection, or afternoon bin bring-in).
  - **Local Scheduling Only:** Reminders are scheduled locally on your device via Android's AlarmManager / BroadcastReceiver. No push notification services or remote notification servers are used.
- **Internet Access (INTERNET):**
  - **Purpose:** Used to query public Penrith LGA bin schedule data (from `penrithcity.nsw.gov.au` / `penrith.waste-info.com.au`) to fetch property collection days, and for Google Play Billing.

---

### 3. In-App Purchases (Optional Developer Support)

The App offers optional, voluntary developer support tips via **Google Play In-App Billing**.
- All transactions are processed directly and securely by Google Play.
- We do not receive, process, or store your credit card or financial information.
- All core features of Penrith Bins remain completely free with no paywalls or advertising.

---

### 4. Non-Affiliation Disclaimer & Official Data Sources

Penrith Bins is an independent utility created by a local developer and is not affiliated with, endorsed by, or representing Penrith City Council or the NSW Government. 

Waste collection guidelines, bulky waste clean-up rules, and drop-off facility details are sourced from publicly available information provided by Penrith City Council (`penrithcity.nsw.gov.au` and `penrith.waste-info.com.au`) and the NSW EPA Return and Earn network (`returnandearn.org.au`).

---

### 5. Children's Privacy

The App does not knowingly collect or solicit any personal information from children under the age of 13.

---

### 6. Changes to This Privacy Policy

We may update this Privacy Policy periodically. Any updates will be published with a revised "Last updated" date at the top of this document.

---

### 7. Contact Us

If you have any questions or suggestions regarding this Privacy Policy, please contact us by opening an issue on our GitHub repository:  
[https://github.com/Aussie-App-Studio/Penrith-Bins](https://github.com/Aussie-App-Studio/Penrith-Bins)
