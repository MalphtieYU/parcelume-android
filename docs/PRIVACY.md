# Parcelume privacy details

[简体中文](PRIVACY.zh-CN.md) · English

Parcelume 0.3.6 works without a Parcelume account or server. The current build remains offline-only.

## Shopping screen capture

Android exposes accessibility-service access as a broad and sensitive system capability. Parcelume shows a prominent in-app disclosure before opening the system settings, and records consent only after an affirmative user action. That consent is requested once, remains local, and is not shown again during normal use unless the user revokes it or a future version materially changes the disclosed scope.

Parcelume narrows its actual behavior in code:

1. Android event delivery is dynamically narrowed to the source packages currently enabled by the user; if none are enabled, a non-existent sentinel package prevents vendor builds from interpreting an empty list as “all apps.”
2. The foreground app must belong to Parcelume's supported-package list and be explicitly enabled by the user inside Parcelume.
3. The event package and active-window package must match.
4. Password fields, all editable nodes, and nodes marked accessibility-sensitive by Android 14+ are skipped before text collection.
5. Payment, card, password, and verification-code screens are rejected; address, contact, authentication, and payment rows are removed before extraction.
6. Deterministic local rules require order or delivery evidence before creating a record.
7. Raw window text is discarded after parsing; only extracted parcel fields reach the encrypted database.

The service does not click, scroll, type, perform gestures, draw overlays, take screenshots, navigate another app, or submit information on the user's behalf.

Parsing and database writes run on a dedicated single-thread executor instead of blocking the accessibility event callback. Delayed samples are cancelled when the source selection changes, the service stops, or a newer page event replaces them.

## Explicit manual import

The user can paste order or delivery text into Parcelume or share plain text to it through Android's share sheet. This fallback does not need accessibility or notification access. The original input is bounded, processed only in memory, checked by the same sensitive-page rejection and parcel-evidence rules, and discarded after the operation. Only recognized structured parcel fields can reach the encrypted database. Parcelume never reads the clipboard automatically.

## Optional notification backup

Notification access is optional. When enabled, Parcelume first checks the posting package against the same user-selected sources. Notifications from every other package return immediately. Matching title and text are processed in memory and discarded after structured fields are extracted.

Parcelume does not reply to, dismiss, snooze, or modify notifications.

## Stored fields

A local record can include the selected source, optional item title, order reference, parcel status, tracking number, pickup code, capture method, timestamps, and completion or archive state. Item-title storage is disabled by default for new captures.

Item titles, order references, tracking numbers, and pickup codes are encrypted with randomized AES-GCM using a non-exportable Android Keystore key. Separate keyed fingerprints allow exact local matching without retaining searchable plaintext. Existing 0.2 plaintext fields are encrypted during the version-3 database migration. Android backup and device-transfer rules exclude the database and local settings.

Raw screen trees, notification bodies, screenshots, images, attachments, addresses, phone numbers, account passwords, payment details, and complete shopping histories are not intentionally stored.

## Optional privacy-safe compatibility report

Privacy-safe capture diagnostics are enabled by default for new 0.3.6 installations and can be disabled at any time. Parcelume keeps at most 20 metadata entries on device: device model, Android version, shopping-app name and version, parser version, page kind, result code, and node/candidate counts. Repeated delayed samples are coalesced. The guided device-test flow can add only the selected source, local start time, and readiness result to the report; it does not grant access or capture a page by itself. The history and report contain no raw page text, item name, order or tracking number, pickup code, address, phone number, account, or payment data. Disabling diagnostics clears the history. Android's share sheet opens only after the user taps **Share privacy-safe report**, and the user chooses whether and where to send it.

## Network boundary

The current Android manifest contains no internet or network-state permission. Parcelume has no analytics, ads, account system, remote logging, cloud sync, or live carrier lookup.

## User control

Users can enable multiple sources at the same time, disable any source, pause all capture immediately, hide item names, revoke in-app consent, revoke accessibility or notification access in Android settings, choose a retention period for completed records, correct or delete one parcel, safely undo parcels newly inserted by the latest capture, or delete all local parcel data. `FLAG_SECURE` keeps Parcelume content out of screenshots, screen recordings, and recent-app previews.
