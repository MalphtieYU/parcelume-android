# Parcelume 0.3.4 Capture Health & Recovery

## What changed

- Adds a core-source health view for Taobao, JD, Pinduoduo, Tmall, Cainiao, and SF Express.
- Keeps a local ring of up to 20 privacy-safe capture outcomes and coalesces repeated delayed samples.
- Reports page-not-ready, duplicate, unsupported, low-confidence, sensitive-page, excluded-category, storage-error, and successful outcomes without storing page or order content.
- Shows a short success toast containing only the number of organized parcels.
- Writes every multi-parcel capture inside one SQLite transaction.
- Lets users correct a parcel name or status and delete one local parcel.
- Safely undoes only rows newly inserted by the most recent capture; pre-existing rows are never removed by undo.
- Separates six core sources from experimental sources in Settings.
- Adds a direct Android App info shortcut for sideloaded-app restricted settings.

## Privacy boundary

The APK still has no internet, network-state, contacts, SMS, phone, media, broad storage, location, or query-all-packages permission. Diagnostic entries contain metadata and counts only. Sharing remains an explicit user action through Android's share sheet.

## Local verification

- 42 unit tests passed with zero failures or skips.
- Android Lint completed successfully.
- Debug APK assembled successfully for minSdk 26 and targetSdk 37.
- Final APK manifest was inspected directly; only WorkManager lifecycle permissions and the AndroidX dynamic-receiver protection permission are present.
- Android 15 emulator regression covered first launch, one-time consent, the real system accessibility flow, English/Chinese layout, local editing, single-record deletion, and reboot persistence.
- Six local QA fixtures with fake data exercised Taobao, JD, Pinduoduo, Tmall, Cainiao, and SF Express end to end. Each source created one correct record; a second pass created no duplicates.
- A fake checkout/password page was blocked as sensitive, and its diagnostic contained metadata and counts only.
- A read-only plaintext scan of the app sandbox found none of the fake item, order, tracking, card, or password values.
- The candidate APK SHA-256 is `66E820044F0F44537674ABC9757A66E7BE6F5155DA466D2B1F9B0610560BB557`.

## Remaining limitation

This candidate is still debug-signed. Controlled tests cannot certify future live shopping-app layouts, so the next gate is privacy-safe testing on Xiaomi/Redmi, Huawei, Honor, and Samsung devices before publishing a release.

---

# Parcelume 0.3.4 识别健康与恢复版本

## 本次更新

- 为淘宝、京东、拼多多、天猫、菜鸟和顺丰增加逐平台识别健康状态。
- 最多保留 20 条本地脱敏识别结果，并合并短时间内重复的延迟采样。
- 能区分页面未就绪、重复内容、非支持页面、信息不足、敏感页面、排除类别、本地保存失败和识别成功，不保存页面或订单原文。
- 成功后只显示包含包裹数量的简短提示。
- 同一次多包裹识别使用一个 SQLite 事务写入。
- 支持修正包裹名称或状态，以及删除单条本地记录。
- “撤销最近识别”只删除最近一次新插入的记录，不会删除此前已经存在的包裹。
- 设置页明确分开六个重点平台和实验性平台。
- 增加 Android 应用信息入口，方便处理侧载应用的受限设置。

## 隐私边界

APK 仍不包含联网、网络状态、联系人、短信、通话、媒体、广泛存储、位置或查询全部应用权限。诊断只包含元数据和计数，分享仍必须由用户主动点击并通过 Android 系统分享面板完成。

## 本地验证

- 42 项单元测试全部通过，没有失败或跳过。
- Android Lint 检查通过。
- Debug APK 构建成功，最低 Android 8.0，目标 API 37。
- 已直接读取成品 APK 权限；只包含 WorkManager 生命周期权限和 AndroidX 动态接收器保护权限。
- Android 15 模拟器已覆盖首次启动、一次性同意、真实系统辅助功能授权、中英文排版、本地修正、单条删除和重启后数据保留。
- 使用只含假数据的本地 QA 夹具，对淘宝、京东、拼多多、天猫、菜鸟和顺丰完成端到端识别；六个平台各新增一条正确记录，第二轮没有重复入库。
- 含假收银台、银行卡和密码字段的页面被判定为敏感页面并拒绝解析，诊断中只有元数据和计数。
- 对 App 自身沙盒进行了只读明文扫描，没有发现假商品名、订单号、运单号、银行卡或密码字段。
- 当前候选 APK 的 SHA-256 为 `66E820044F0F44537674ABC9757A66E7BE6F5155DA466D2B1F9B0610560BB557`。

## 仍有限制

当前候选包仍是 Debug 签名。受控测试不能代表未来所有购物应用真实页面，因此发布前下一道门槛是在小米/红米、华为、荣耀和三星设备上进行脱敏真机测试。
