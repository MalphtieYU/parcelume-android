# Parcelume 0.3.6 Guided Device Test

## What changed

- Adds an in-app device-test guide for Taobao, JD, Pinduoduo, Tmall, Cainiao, and SF Express.
- Verifies privacy-safe diagnostics, the selected source, local consent, Android accessibility access, and capture state before a test run.
- Times one source at a time and filters the shared report to diagnostics created during that run.
- Keeps reports metadata-only: device/app/parser versions, page kind, result code, and node/candidate counts. No page text or parcel identifiers are accepted by the report builder.
- Adds Android 15 instrumentation tests for first launch, screenshot protection, and local rejection of payment/password content.
- Adds a privacy-gated GitHub compatibility form so testers are warned not to post screenshots or order data.

## Verification boundary

- 50 unit tests passed twice, including one forced rebuild; no failures or skips.
- 3 Android 15 instrumentation tests passed on an AOSP ATD virtual device; no failures or skips.
- Lint completed with zero errors. Its only warning is the informational availability of a newer Gradle wrapper.
- The APK verifies with Android APK Signature Scheme v2. The merged manifest contains no `INTERNET` or `ACCESS_NETWORK_STATE` permission.
- APK SHA-256: `82F09E403CE44238969103C5B9C7F0893FA1A45D75C18EC5D3496F6CB92966CC`.

Automated tests use deterministic fixtures and an Android 15 virtual device. They verify application behavior, not every current layout in third-party shopping apps. Physical-device testers should test only one selected source at a time, open existing order/tracking pages without making a purchase, never enter payment or credential pages for testing, and share only the generated privacy-safe report.

This APK is debug-signed and remains an evaluation build. Parcelume is offline-only in 0.3.6 and cannot provide live carrier tracking or import pages the user never opens.

---

# Parcelume 0.3.6 真机测试向导版

## 本次更新

- 为淘宝、京东、拼多多、天猫、菜鸟和顺丰新增应用内真机测试向导。
- 测试前逐项检查脱敏诊断、所选平台、本机隐私同意、Android 辅助功能权限和识别暂停状态。
- 每轮只测试一个平台，并只把本轮开始后产生的诊断加入分享报告。
- 报告严格限制为设备/应用/解析器版本、页面类型、结果代码和节点/候选数量；报告构建器不接收页面原文或包裹标识。
- 新增 Android 15 自动化测试，覆盖首次启动、截屏防护，以及付款/密码内容的本地拒绝。
- 新增带隐私确认的 GitHub 兼容性反馈表，明确禁止上传截图和订单数据。

## 验证边界

- 50 项单元测试通过两轮，其中一轮强制重编译；没有失败或跳过。
- 3 项 Android 15 界面测试在 AOSP ATD 虚拟设备上全部通过；没有失败或跳过。
- Lint 零错误；唯一警告只是提示存在更新的 Gradle Wrapper。
- APK 已通过 Android APK Signature Scheme v2 签名验证，合并后的 Manifest 不含 `INTERNET` 或 `ACCESS_NETWORK_STATE` 权限。
- APK SHA-256：`82F09E403CE44238969103C5B9C7F0893FA1A45D75C18EC5D3496F6CB92966CC`。

自动化测试使用固定样本和 Android 15 虚拟设备，只能验证 Parcelume 自身行为，不能代表第三方购物应用当前所有真实页面。实体设备测试时，应当每轮只选择一个平台，只打开已有订单与物流页面而无需重新购买；不要为了测试进入付款、密码或验证码页面，只分享应用生成的脱敏报告。

此 APK 仍为 Debug 签名测试包。0.3.6 完全离线，不能提供实时快递查询，也不能导入用户从未打开过的页面。
