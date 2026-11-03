# Parcelume 0.3.5 Narrower Capture & Manual Fallback

## What changed

- Adds an explicit **Import a parcel** flow for pasted text and Android plain-text sharing.
- Keeps imported raw text in memory only and saves only fields accepted by the existing privacy-aware parser.
- Dynamically limits Android accessibility event delivery to the source packages currently enabled by the user.
- Skips editable fields and Android 14+ accessibility-sensitive nodes before collecting visible text.
- Cancels stale delayed samples when a newer page arrives or source settings change.
- Moves parsing and database work off the accessibility callback thread to improve responsiveness on slower devices.
- Updates AndroidX Core and WorkManager to current compatible versions.

## Privacy boundary

The APK still has no internet, network-state, contacts, SMS, phone, media, broad storage, location, or query-all-packages permission. Parcelume never reads the clipboard automatically. Manual import is always a direct user action and still rejects payment, card, password, verification-code, food/instant-delivery, refund, cancellation, and after-sales content.

## Verification

- 46 unit tests passed with zero failures or skips.
- Android Lint completed with zero errors.
- Debug APK assembled successfully for minSdk 26 and targetSdk 37.
- The finished APK manifest contains no internet or network-state permission; its only declared uses-permissions are WorkManager lifecycle permissions and AndroidX's non-exported dynamic-receiver protection permission.
- APK Signature Scheme v2 verification passed. The debug APK SHA-256 is `28E962342E06F8107854693442450D2E839A7D0C1A5551A4E4F3D0D198DFF99E`.
- Manual import tests cover a valid JD order, sensitive payment/password rejection, hidden item titles, and unrelated-text rejection.
- A clean Android 15 emulator was prepared, but the local emulator runtime exited before completing ADB boot; no device-level claim is made from that environment.

## Remaining limitation

This candidate is debug-signed and still requires privacy-safe validation on real Xiaomi/Redmi, Huawei, Honor, and Samsung devices. Manual import reduces missed records but does not provide live carrier tracking or access hidden shopping-app history.

---

# Parcelume 0.3.5 更窄识别范围与主动导入兜底

## 本次更新

- 新增“主动导入包裹”，支持粘贴文字和 Android 纯文字分享。
- 导入原文只在内存中处理，只有通过既有隐私解析规则的字段才会保存。
- Android 辅助功能事件范围会动态收窄到用户当前开启的来源包名。
- 收集可见文字前跳过可编辑字段和 Android 14 及以上标记为辅助功能敏感的节点。
- 新页面到来或来源设置变化时取消过时的延迟采样。
- 把解析和数据库工作移出辅助功能回调线程，改善较慢设备上的响应。
- 更新 AndroidX Core 与 WorkManager 到当前兼容版本。

## 隐私边界

APK 仍不包含联网、网络状态、联系人、短信、通话、媒体、广泛存储、位置或查询全部应用权限。Parcelume 不会自动读取剪贴板；主动导入始终由用户明确操作，并继续拒绝付款、银行卡、密码、验证码、外卖/即时配送、退款、取消和售后内容。

## 验证情况

- 46 项单元测试全部通过，没有失败或跳过。
- Android Lint 零错误完成。
- Debug APK 构建成功，最低 Android 8.0，目标 API 37。
- 成品 APK 的 Manifest 不包含联网或网络状态权限；声明的 uses-permission 只有 WorkManager 生命周期权限与 AndroidX 非导出动态接收器保护权限。
- APK Signature Scheme v2 验签通过。Debug APK 的 SHA-256 为 `28E962342E06F8107854693442450D2E839A7D0C1A5551A4E4F3D0D198DFF99E`。
- 主动导入测试覆盖有效京东订单、支付/密码敏感内容拒绝、隐藏商品名和无关文字拒绝。
- 已准备全新 Android 15 模拟器，但本机模拟器运行时在 ADB 完成启动前退出，因此不把该环境算作设备级验证通过。

## 仍有限制

当前候选包仍是 Debug 签名，还需要在小米/红米、华为、荣耀和三星真机上进行脱敏验证。主动导入可以减少遗漏，但不能提供实时联网物流追踪，也不能读取购物应用隐藏的历史订单。
