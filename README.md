# Parcelume

> A private, local-first parcel inbox for Android.

[简体中文](README.zh-CN.md) · English

Parcelume turns delivery notifications from selected shopping and logistics apps into one calm, searchable parcel timeline. It does not require an account, does not connect to a server, and does not import shopping history.

> [!IMPORTANT]
> Parcelume is an early Android MVP. It detects **future supported notifications after setup**. Installing it alone does not reveal past orders, and it cannot provide live carrier updates when the source app sends no notification.

## Highlights

- **Automatic, not manual** — extracts useful parcel fields from supported notifications.
- **Local-first** — structured records stay in an on-device SQLite database.
- **No internet permission** — the current build cannot upload notification or parcel data.
- **Source controls** — notifications from unselected apps are ignored before parsing.
- **Browse-only fallback** — the interface remains usable when access is declined.
- **Flexible retention** — keep completed records for one week, one month, one year, or forever.
- **Bilingual UI** — switch between Chinese and English without changing features or layout.

## Preview

| Parcel inbox | Permission explanation | Android system access |
| --- | --- | --- |
| <img src="docs/images/home.png" width="240" alt="Parcelume parcel inbox"> | <img src="docs/images/permission-setup.png" width="240" alt="Parcelume permission explanation"> | <img src="docs/images/system-access.png" width="240" alt="Android system notification access for Parcelume"> |

## How it works

```text
Selected app notification
          ↓
Android NotificationListenerService
          ↓
On-device deterministic parser
          ↓
Minimal structured fields in SQLite
          ↓
Parcelume timeline and pickup view
```

Parcelume may extract the source, delivery status, tracking number, pickup code, update time, and—only when present in the notification—the item name. Raw notification text is processed in memory and is not stored.

## Quick start

1. Install the APK on Android 8.0 or later.
2. Open Parcelume and review the first-use privacy explanation.
3. Select only the shopping or logistics apps you want Parcelume to process.
4. Tap **Enable notification access**.
5. Parcelume opens your phone's real Android system settings. Grant access there, then return to the app.
6. New matching delivery notifications will appear automatically.

If you choose **Not now — browse only**, Parcelume opens normally and keeps a visible reminder until access and at least one source are enabled.

## System permission behavior

Notification access is controlled exclusively by Android. Parcelume cannot silently grant it.

- Android 11 and later: opens Parcelume's system notification-listener detail page when the device supports it.
- Older or customized Android builds: falls back to the system notification-access list.
- Devices without either route: falls back to the main system settings screen.

Menu names vary across Samsung, Xiaomi, OPPO, vivo, Huawei, Honor, OnePlus, Google Pixel, and other Android variants. Some vendors may also require users to allow background operation in their battery or auto-start settings. Parcelume deliberately does not request unrelated permissions merely to bypass manufacturer restrictions.

## Privacy model

| Area | Parcelume behavior |
| --- | --- |
| Network | No `INTERNET` or `ACCESS_NETWORK_STATE` permission |
| Accounts | No Parcelume account, login, or cloud sync |
| Notification scope | Android grants broad listener access; Parcelume filters by user-selected source package before reading notification fields |
| Stored data | Parsed parcel fields only; no raw notification body, screenshots, or attachments |
| Other personal data | No contacts, SMS, photos, location, accessibility service, or shopping-app credentials |
| Deletion | User-controlled retention plus one-tap deletion of all local data |

See [Privacy details](docs/PRIVACY.md) for the exact boundary.

## Supported sources

The MVP contains local rules for notifications from Taobao, JD, Pinduoduo, Cainiao, SF Express, and Amazon Shopping.

Support means Parcelume recognizes known notification patterns; it does not integrate with or represent these companies. Wording changes in a source app can temporarily reduce recognition accuracy.

## Important limitations

- Past orders are not imported.
- Muted, hidden, delayed, or generic notifications cannot be parsed.
- Parcel status changes only when a supported source posts a useful notification.
- Item names are unavailable when the source notification omits them.
- Notification wording and package names can vary by region and app version.
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

Current MVP validation includes Android 15 emulator UI and permission-flow checks, a notification-to-local-record end-to-end test, parser unit tests, Android Lint, and manifest verification confirming no network permission. Real-device samples from different Android vendors are still needed before a stable release.

## Contributing

Issues and pull requests are welcome, especially anonymized notification-format reports, parser tests, and device-compatibility findings. Never post real tracking numbers, pickup codes, addresses, phone numbers, or complete notification content.

## License

Licensed under the [Apache License 2.0](LICENSE).
