# Parcelume

> A private, local-first parcel inbox for Android.

[简体中文](README.zh-CN.md) · English

Parcelume turns order and delivery details already visible in user-selected shopping apps into one calm, searchable parcel timeline. On Android, its opt-in screen-capture service is the primary source and notifications are an optional backup. It does not require a Parcelume account or connect to a server.

> [!IMPORTANT]
> Parcelume is an early Android MVP. It can only recognize supported order or delivery pages that the user actually opens after setup. It cannot read another app's private order database, import unseen history, guarantee zero omissions, or provide live carrier updates yet.

## Highlights

- **Automatic while shopping** — extracts useful fields when a supported order or delivery page is visible.
- **Notification backup** — notification access is optional rather than the only data source.
- **Encrypted local storage** — sensitive parcel fields are encrypted with non-exportable Android Keystore keys.
- **No internet permission** — the current build cannot upload notification or parcel data.
- **Source controls** — notifications from unselected apps are ignored before parsing.
- **Privacy mode by default** — new captures hide real item names unless the user opts in.
- **Sensitive-page rejection** — payment, card, password, and verification-code screens are not parsed.
- **Screen shielding** — parcel details stay out of screenshots, recordings, and recent-app previews.
- **Browse-only fallback** — the interface remains usable when access is declined.
- **Flexible retention** — keep completed records for one week, one month, one year, or forever.
- **Bilingual UI** — switch between Chinese and English without changing features or layout.

## Preview

| Parcel inbox | Permission explanation | Android system access |
| --- | --- | --- |
| <img src="docs/images/home.png" width="240" alt="Parcelume parcel inbox"> | <img src="docs/images/permission-setup.png" width="240" alt="Parcelume screen-capture disclosure"> | <img src="docs/images/system-access.png" width="240" alt="Android Accessibility settings showing Parcelume"> |

## How it works

```text
Visible order or delivery page in a selected app
          ↓
Android AccessibilityService (read-only use)
          ↓
On-device deterministic parser
          ↓
Encrypted structured fields in SQLite
          ↓
Parcelume timeline and pickup view
```

Parcelume may extract the source, optional item title, order reference, delivery status, tracking number, pickup code, and update time. Raw screen and notification text is processed in memory and is not stored. Payment or credential screens are rejected, sensitive rows are removed before extraction, and stored private fields are encrypted locally. The service does not click, scroll, type, perform gestures, or take screenshots.

## Download the preview

Download [`parcelume-0.3.0-debug.apk`](https://github.com/MalphtieYU/parcelume-android/releases/tag/v0.3.0-preview) from the 0.3.0 preview release. This is a debug-signed evaluation build, not a production Play Store build; review the limitations below before granting system access.

## Quick start

1. Install the APK on Android 8.0 or later.
2. Open Parcelume and review the prominent screen-capture disclosure.
3. Select only the shopping or logistics apps Parcelume may process.
4. Confirm the disclosure once and tap **Enable shopping screen capture**. It is not shown again on normal launches unless consent is revoked or the disclosed data scope materially changes.
5. Parcelume opens Android's real Accessibility settings. Select Parcelume and grant access there, then return.
6. Open an order confirmation, order detail, or delivery page in a selected app. Matching fields are captured locally.
7. Optionally enable notification access as a backup source.

If you choose **Not now — browse only**, Parcelume opens normally and keeps a visible reminder until screen capture and at least one source are enabled.

## System permission behavior

Screen-capture and notification access are controlled exclusively by Android. Parcelume cannot silently grant either permission.

- The primary button opens Android's Accessibility settings, where the user must explicitly enable **Parcelume shopping screen capture**.
- Android displays a broad system warning for accessibility services. Parcelume narrows its actual behavior in code: selected package allowlist, visible text only, no gestures or actions, and no raw-screen storage.
- Consent is stored locally after the affirmative first-run choice. Users can pause all capture or revoke that consent at any time.
- Optional notification backup opens Parcelume's listener detail page on Android 11+ when supported, with list/settings fallbacks on other devices.

Menu names vary across Samsung, Xiaomi, OPPO, vivo, Huawei, Honor, OnePlus, Google Pixel, and other Android variants. Some vendors may also require users to allow background operation in their battery or auto-start settings. Parcelume deliberately does not request unrelated permissions merely to bypass manufacturer restrictions.

## Privacy model

| Area | Parcelume behavior |
| --- | --- |
| Network | No `INTERNET` or `ACCESS_NETWORK_STATE` permission |
| Accounts | No Parcelume account, login, or cloud sync |
| Screen scope | Android grants broad accessibility access; Parcelume filters by a fixed supported-package list plus the user's selected sources before parsing visible text |
| Screen actions | No clicks, gestures, scrolling, typing, overlays, or screenshots |
| Notification scope | Optional backup; unselected packages are ignored before notification fields are parsed |
| Stored data | Parsed parcel fields only; no raw screen tree, notification body, screenshots, or attachments |
| At-rest protection | Item titles, order references, tracking numbers, and pickup codes use randomized AES-GCM encryption backed by Android Keystore; keyed fingerprints support local matching without searchable plaintext |
| Sensitive pages | Payment, card, password, and verification-code screens are rejected; address, contact, authentication, and payment rows are removed before extraction |
| Display protection | Android `FLAG_SECURE` blocks app screenshots, screen recording, and recent-app previews |
| Other personal data | Does not read contacts, SMS, photos, location, or shopping-app credentials |
| Deletion | User-controlled retention plus one-tap deletion of all local data |

See [Privacy details](docs/PRIVACY.md) for the exact boundary.

## Supported sources

Version 0.3 includes package-scoped support for 19 apps: Taobao, JD, Pinduoduo, Tmall, Cainiao, SF Express, Amazon, Temu, AliExpress, eBay, Walmart, SHEIN, Etsy, Alibaba.com, Lazada, Shopee Indonesia, Flipkart, Mercado Libre, and Rakuten Ichiba.

Support means Parcelume recognizes known visible-page and notification patterns; it does not integrate with or represent these companies. Layout, wording, custom-rendered views, region, and app-version changes can reduce recognition accuracy.

## Important limitations

- Past or hidden orders are not imported unless the user opens a recognizable page.
- Some custom-rendered or protected pages expose no readable text to Android accessibility services.
- Parcelume does not control other apps and does not navigate pages on the user's behalf.
- Live carrier lookup is not included in 0.3; status changes require another recognizable page or notification.
- Screen layouts, wording, and package names vary by region and app version.
- Accessibility use requires prominent consent and may need additional store-policy review before public store distribution.
- This debug APK is for evaluation and is not a signed production release.

## Build from source

Requirements: Android Studio with Android SDK 37, JDK 17, and an Android 8.0+ device or emulator.

```bash
git clone https://github.com/MalphtieYU/parcelume-android.git
cd parcelume-android
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

Run verification:

```bash
./gradlew testDebugUnitTest lintDebug
```

## Project status

Current MVP validation includes Android 15 emulator UI and real accessibility-permission flow checks, screen and notification parser unit tests, Android Lint, database migration coverage through installation testing, and manifest verification confirming no network permission. Real shopping-app pages and devices from different Android vendors are still needed before a stable release.

## Contributing

Issues and pull requests are welcome, especially anonymized notification-format reports, parser tests, and device-compatibility findings. Never post real tracking numbers, pickup codes, addresses, phone numbers, or complete notification content.

## License

Licensed under the [Apache License 2.0](LICENSE).
