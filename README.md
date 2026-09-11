# SnapShop (Android)
### AI-powered shopping assistant for Android — scan products, barcodes, or video to instantly find prices across retailers. Built with Kotlin, Jetpack Compose, Cloudflare Workers, Gemini 2.5, and Groq vision.

**Capstone project — triOS College, Android Development Course, 2026**

## What It Does
Point your Android camera at any product and SnapShop identifies it and shows you the best prices from retailers like Amazon, eBay, and Best Buy — in seconds.

| Scan Mode | How It Works |
| :--- | :--- |
| **Photo** | Google ML Kit OCR reads on-device text, Groq vision identifies the product |
| **Barcode** | Google ML Kit detects EAN-13, UPC, and ISBN barcodes fully on-device |
| **Video (Deep Scan)** | Gemini 2.5 analyzes multiple frames for complex or hard-to-identify items |
| **Paste a Link** | Resolves the product URL and fetches live comparison prices instantly |

## Features
*   **Multi-modal scanning** — photo, barcode, video, and URL.
*   **On-device processing** — barcodes and OCR run locally via ML Kit; no data is sent for these steps.
*   **Real-time price comparison** — Best Buy, eBay, Amazon, and more via Google Shopping.
*   **Plant identification** — species detection with safety warnings for dangerous plants.
*   **Scan history** — every result saved locally using Room persistence with generated thumbnails.
*   **Price alerts** — save items and set target prices to track drops.
*   **Pro tier & Quota** — monthly scan limits for free users; unlimited scans via Pro gating.
*   **Sign in with Google** — secure authentication for personalized features.
*   **Material 3 Dark Mode** — premium dark/gold design tokens with accessible contrast.

## Tech Stack
### Android (On-Device)
*   **Kotlin & Jetpack Compose** — modern declarative UI.
*   **CameraX** — high-performance camera capture (photo and video).
*   **Google ML Kit** — on-device barcode scanning and text recognition.
*   **Room Database** — local persistence with Flow-based reactive queries.
*   **Google Play Billing Library** — in-app purchase flow for Pro features.
*   **Coil** — asynchronous image loading.

### Backend (Cloudflare Workers — Cloud AI)
*   **Gemini 2.5 Flash / Pro** — deep video identification.
*   **Groq** — ultra-fast vision inference for photo scans.
*   **SerpAPI** — Google Shopping results aggregation.
*   **Best Buy & eBay APIs** — direct retailer pricing integration.

## Architecture
Android App (Compose)
    │
    ├── ui/camera/        — CameraX implementation & Adjust Crop flow
    ├── ui/results/       — Bayesian-sorted price cards & Rating breakdowns
    ├── data/             — Room DB (Entities, DAOs) & QuotaManager
    ├── network/          — BackendClient using OkHttp & Kotlin Serialization
    │
    └── Backend (Workers)
            ├── /identify/precision   — Groq vision + barcode logic
            ├── /identify/deep        — Gemini 2.5 multi-frame analysis
            ├── /shop                 — Aggregated retailer price engine
            └── /transcribe           — Whisper audio processing

## Getting Started
### Prerequisites
*   **Android Studio Koala+** (or latest 2026 version)
*   **Android 8.0+** device (API 26+)
*   **Node.js 20+** (for backend)
*   **Wrangler CLI** (`npm i -g wrangler`)

### Backend
1.  `cd backend`
2.  `npm install`
3.  `npx wrangler deploy`
4.  Set secrets in Cloudflare: `GEMINI_API_KEY`, `GROQ_API_KEY`, `SERPAPI_KEY`.

### Android App
1.  Open the project in **Android Studio**.
2.  Update `BuildConfig.BACKEND_URL` in `app/build.gradle.kts` with your Worker URL.
3.  Sync Gradle and press **Run**.

## Project Structure
app/src/main/java/com/melakunet/snapshop/
├── data/           — Room Entities, DAO, Database, QuotaManager
├── models/         — ShopItem, IdentifyResult, ProductReviews
├── network/        — BackendClient (OkHttp implementation)
├── ui/
│   ├── camera/     — CameraScreen, AdjustCropScreen, ResultsScreen
│   ├── history/    — Flow-driven scan history with swipe-to-delete
│   ├── onboarding/ — Onboarding slides & Sign In screen
│   ├── settings/   — Sectioned preferences & Debug tools
│   └── theme/      — Brand tokens, Colors, and Typography

## License
Academic project — **triOS College, 2026**. Not licensed for commercial use. Developed for the Android Development Capstone.
