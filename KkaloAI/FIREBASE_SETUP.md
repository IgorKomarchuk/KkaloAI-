# Firebase Setup Details

This file tracks the project-specific Firebase configuration and manual setup steps.

## Project Information
- **Project Name**: SnapCal
- **Project ID**: `snapcal-9b8e5` (Internal ID: `project-654613887637`)
- **Support Email**: `pokypka27@gmail.com`
- **Region**: `europe-west4` (Netherlands) - *Selected for low latency in Ukraine/Europe*

## 🔑 Critical: SHA-1 Fingerprint
The fingerprints have been retrieved and must be added to the Firebase Console:
- **SHA-1**: `88:D2:D6:D9:10:01:26:3E:2C:B5:8C:9C:9F:27:85:C6:E8:68:C7:99`
- **SHA-256**: `E5:57:F5:28:C3:B7:15:D6:CD:7F:43:12:18:F8:C4:4C:BF:2C:EE:18:17:64:9A:5C:41:78:8B:F2:56:39:DF:9C`

To add these:
1. Copy them from here.
2. Paste them into [Firebase Console -> Project Settings -> General -> Your Apps](https://console.firebase.google.com/u/0/project/snapcal-9b8e5/settings/general).
3. **Download the updated `google-services.json` and replace the one in your project.**

## 🛠 Services Status
- [x] **Authentication**: Anonymous enabled.
- [ ] **Authentication**: Google enabled (pending SHA-1).
- [ ] **Cloud Firestore**: Enabled (Pending region selection & mode).
- [ ] **Crashlytics**: SDK Integrated (Pending first crash to enable dashboard).
