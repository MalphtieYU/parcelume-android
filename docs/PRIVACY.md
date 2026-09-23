# Parcelume privacy details

[简体中文](PRIVACY.zh-CN.md) · English

Parcelume 0.2 works without a Parcelume account or server. The current build remains offline-only.

## Shopping screen capture

Android exposes accessibility-service access as a broad and sensitive system capability. Parcelume shows a prominent in-app disclosure before opening the system settings, and records consent only after an affirmative user action.

Parcelume narrows its actual behavior in code:

1. The foreground app must belong to Parcelume's fixed supported-package list.
2. The app must also be explicitly enabled by the user inside Parcelume.
3. Only text and content descriptions already visible in the active window are inspected.
4. Deterministic local rules require order or delivery evidence before creating a record.
5. Raw window text is discarded after parsing; only extracted parcel fields reach the database.

The service does not click, scroll, type, perform gestures, draw overlays, take screenshots, navigate another app, or submit information on the user's behalf.

## Optional notification backup

Notification access is optional. When enabled, Parcelume first checks the posting package against the same user-selected sources. Notifications from every other package return immediately. Matching title and text are processed in memory and discarded after structured fields are extracted.

Parcelume does not reply to, dismiss, snooze, or modify notifications.

## Stored fields

A local record can include the selected source, item title, order reference, parcel status, tracking number, pickup code, capture method, timestamps, and completion or archive state.

Raw screen trees, notification bodies, screenshots, images, attachments, addresses, phone numbers, account passwords, payment details, and complete shopping histories are not intentionally stored.

## Network boundary

The current Android manifest contains no internet or network-state permission. Parcelume has no analytics, ads, account system, remote logging, cloud sync, or live carrier lookup.

## User control

Users can disable any source, revoke accessibility or notification access in Android settings, choose a retention period for completed records, or delete all local parcel data from Parcelume settings.
