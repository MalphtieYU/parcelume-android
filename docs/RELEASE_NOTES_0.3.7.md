# Parcelume 0.3.7 Stability Preview

## What changed

- Contains parser and database failures inside the background capture boundary instead of allowing them to stop the service or process.
- Safely ignores work submitted while the accessibility or notification service is shutting down.
- Prevents stale pages and notifications from moving a parcel status, update time, or capture source backwards.
- Keeps delivered and ready-for-pickup parcels from being regressed by a late exception, while allowing a real later status to recover from an earlier exception.
- Requires nearby logistics context before accepting an unlabelled tracking-number pattern and explicitly rejects promotion, coupon, SKU, and redemption-code contexts.
- Maps an unknown stored status to a visible exception state rather than crashing the parcel list during startup.
- Makes successful-capture confirmations quiet by default, with an opt-in bilingual switch in Privacy protection.
- Adds a privacy-safe processing-error diagnostic that contains metadata only.

## Verification

- 58 JVM tests passed, including 1,000 deterministic promotion/noise cases.
- 5 Android 15 instrumentation tests passed in three consecutive runs. They cover first launch, screenshot protection, sensitive shared-text rejection, unknown database-status recovery, and stale-notification rollback prevention.
- Android Lint completed with zero errors. Its only warning is the informational availability of a newer Gradle wrapper.
- Debug and unsigned Release variants both build successfully.
- The published evaluation APK verifies with Android APK Signature Scheme v2.
- The merged APK permission list contains no `INTERNET` or `ACCESS_NETWORK_STATE` permission.
- APK SHA-256: `B702AE4749648C5B4180EF3FD3D6B2FD6C964316499DA406944B4A5A6027C2C9`.

## Boundary

This remains a debug-signed evaluation build. Automated tests verify Parcelume itself and fixed parser fixtures; they cannot prove compatibility with every current or future page layout in third-party shopping apps. Parcelume 0.3.7 remains offline-only, cannot import pages the user never opens, and does not provide live carrier tracking.

---

# Parcelume 0.3.7 稳定性预览版

## 本次更新

- 把解析和数据库异常限制在后台识别边界内，避免一次异常导致服务或进程停止。
- 辅助功能或通知服务关闭时，不再执行已经过期的新任务。
- 旧页面与旧通知不会再让包裹状态、更新时间或识别来源倒退。
- 已送达和待取件不会被迟到的异常状态覆盖；真正的新状态仍可让早先的异常恢复。
- 无标签运单号必须出现在物流语境附近，并明确排除优惠码、兑换码、SKU 和促销码语境。
- 数据库出现未来版本或异常状态值时，显示为异常状态，不再让包裹列表启动崩溃。
- 识别成功提示默认静默；用户可在“隐私保护”中按需开启，中英文排版一致。
- 新增仅包含元数据的“处理异常”诊断结果，不保存页面原文或包裹标识。

## 验证结果

- 58 项 JVM 测试通过，其中包含 1,000 轮固定促销/噪声样本。
- 5 项 Android 15 设备测试连续三轮通过，覆盖首次启动、截屏防护、敏感分享文本拦截、未知数据库状态恢复和旧通知防倒退。
- Android Lint 零错误；唯一警告只是提示存在更新的 Gradle Wrapper。
- Debug 与未签名 Release 两种构建均成功。
- 发布用测试 APK 已通过 Android APK Signature Scheme v2 验签。
- APK 权限清单不含 `INTERNET` 或 `ACCESS_NETWORK_STATE`。
- APK SHA-256：`B702AE4749648C5B4180EF3FD3D6B2FD6C964316499DA406944B4A5A6027C2C9`。

## 仍需注意

此版本仍是 Debug 签名测试包。自动化测试能验证 Parcelume 自身逻辑和固定样本，不能证明第三方购物应用当前或未来每一种页面都兼容。0.3.7 仍完全离线，不能导入用户从未打开过的页面，也不提供实时快递联网查询。
