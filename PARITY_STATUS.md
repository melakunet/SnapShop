# Android vs iOS Parity Audit - SnapShop

Target: iOS version feature set. Audit performed on 2026-09-11.

## 1. IMPLEMENTED

| Feature | Status | Notes |
| :--- | :--- | :--- |
| **Precision Scan** | WORKING | CameraX capture -> Adjust Crop -> Backend identification. |
| **Deep Scan (Video)** | WORKING | Video capture (10s) -> Frame extraction (8 keyframes) -> Gemini identification. |
| **Import from Gallery** | WORKING | Image/Video selection -> Adjust Crop (for images) -> Identification. |
| **URL Scan** | WORKING | URL validation -> Backend identification. |
| **Results Screen** | WORKING | Header card with thumbnail, sort pills (Price/Reviewed), Retailer cards with badges. |
| **Item Detail** | WORKING | Large image, rating breakdown (via productReviews API), "Open in store". |
| **Scan History** | WORKING | Room persistence, reactive Flow queries, swipe-to-delete. |
| **Price Alerts** | WORKING | Save item -> Set target price -> Room persistence. |
| **Quota Management** | WORKING | Monthly rollover via SharedPreferences; free limit (10 release / 300 debug). |
| **Paywall** | WORKING | Dark/gold UI triggered on quota exhaustion or Deep mode access. |
| **Pro Gating** | WORKING | Deep mode and quota bypass when Pro is active. |
| **Settings** | WORKING | Sectioned layout: Scanning, Retailers, Notifications, Privacy, Account, Debug. |
| **Onboarding** | WORKING | 3-slide flow with iOS-matching copy and privacy checklist. |
| **Sign In** | PARTIAL | Stubbed. "Continue in demo mode" works; Google Sign-In shows toast. |

## 2. MISSING VS IOS

| Feature | Effort | Description |
| :--- | :--- | :--- |
| **Live Barcode Scanning** | MEDIUM | iOS uses AVFoundation for on-device barcode overlay. Android has camera but no live ML Kit overlay. |
| **Price Sparkline** | MEDIUM | iOS displays a price history chart in item details. Missing in Android detail view. |
| **Multi-item Deep Chips** | MEDIUM | iOS shows horizontal chips for multiple objects found in video. Android only shows primary result. |
| **Alert Background Polling** | LARGE | iOS polls prices via background tasks. Android has the DB entities but no WorkManager sync yet. |
| **Voice Input** | MEDIUM | Mic button exists but shows "coming soon" toast. Needs SpeechRecognizer integration. |
| **Haptics** | SMALL | Toggles exist in Settings but `HapticFeedback` is not wired to capture/scan actions. |
| **Plant Poison Links** | SMALL | Plant identification works, but safety warnings lack deep-links to poison control. |
| **Confidence Escalation** | SMALL | No banner suggesting Deep mode when Precision confidence is low (< 0.7). |
| **Privacy Policy Content** | SMALL | Row exists but opens a simple AlertDialog instead of a full policy doc. |

## 3. TECHNICAL DEBT

*   **No Automated Tests**: Only boilerplate `ExampleUnitTest` exists. No coverage for Room, Backend, or ViewModels.
*   **Cleartext Traffic**: `android:usesCleartextTraffic="true"` in Manifest (unsafe for production).
*   **Sign-In Stub**: Authentication is purely local (SharedPreferences toggle) and not wired to a real provider.
*   **Hardcoded Backend URL**: `192.168.2.12` in `build.gradle.kts` (needs environment-based config).
*   **Resource Management**: JPEG compression (toCappedJpeg) uses `Bitmap.createScaledBitmap` which can be memory-intensive on large images.

## 4. RECOMMENDED SUBMISSION SCOPE (2-3 Days)

1.  **WorkManager Price Polling (LARGE)**: Essential for the "Alerts" feature to actually function. Wiring `PriceAlert` entities to a background worker.
2.  **Live Barcode Overlay (MEDIUM)**: High visual impact for "Precision" mode. Uses ML Kit `BarcodeScanning` on the `ImageAnalysis` use-case.
3.  **Haptic Feedback (SMALL)**: Quick iOS parity win. Adding `LocalHapticFeedback` to capture and button clicks.
4.  **Confidence Escalation (SMALL)**: Logic fix in `CameraScreen` to show a "Try Deep Scan?" banner when confidence is low.
5.  **Manifest Cleanup (SMALL)**: Disable cleartext traffic and move to HTTPS for backend communication.

**Out of Scope**: Voice Input (OS-level complexity), Price Sparklines (needs historical data backend), Real Google Sign-In (Server-side setup required).
