# Parcelume privacy details

[简体中文](PRIVACY.zh-CN.md) · English

Parcelume is designed to work without an account or server.

## Notification access

Android exposes notification-listener access as a broad system permission. The operating system's warning may mention messages, contacts, or photos because a notification can contain that kind of content. Parcelume narrows this access in its own code:

1. The posting app's package name is checked against sources explicitly enabled by the user.
2. Notifications from all other apps return immediately and are not parsed.
3. Matching notification title and text are processed in memory by deterministic local rules.
4. Only extracted parcel fields are written to the local database.

Parcelume does not reply to, dismiss, snooze, or modify notifications.

## Stored fields

Depending on what the source notification contains, a local record can include the source, parcel status, tracking number, pickup code, item or notification title, timestamps, and completion or archive state.

Raw notification bodies, images, attachments, addresses, account passwords, and shopping histories are not stored.

## Network boundary

The current Android manifest contains no internet or network-state permission. Parcelume has no analytics, ads, account system, remote logging, or cloud sync.

## User control

Users can disable any source, revoke notification access in Android settings, choose a retention period for completed records, or delete all local parcel data from Parcelume settings.
