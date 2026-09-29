# Parcelume 0.3.1 China Core Stabilization

This development build pauses platform expansion and concentrates on capture accuracy, local privacy, and diagnosability for Taobao, JD, Pinduoduo, Tmall, Cainiao, and SF Express.

## What changed

- Parses independent order cards separately instead of flattening an entire page into one parcel.
- Preserves multiple shipments under one order when they have different tracking numbers.
- Prefers the page's explicit current status and prevents older observations from moving a parcel backwards.
- Requires a stable order reference, tracking number, or pickup code before a page capture is saved.
- Samples dynamic pages more than once so asynchronously rendered order details have time to appear.
- Rejects payment, bank-card, password, and verification-code pages before extraction.
- Stops title-only database matching and adds local keyed pickup fingerprints for safer deduplication.
- Adds optional, off-by-default diagnostics containing only result codes and counts—never raw page text, item names, identifiers, addresses, or phone numbers.
- Keeps all processing on-device and still declares no network permission.

## Verification completed

- 23 unit tests: multi-card parsing, split shipments, state progression, sensitive-page rejection, and notification parsing.
- Android Lint and debug assembly.
- Android 15 emulator end-to-end capture of two separate Taobao-style cards with independent statuses.
- Installed upgrade from database version 3 (0.3.0) to version 4 (0.3.1).
- Manifest check confirming there is no `INTERNET` or `ACCESS_NETWORK_STATE` permission.

## Testing boundary

This remains a debug-signed evaluation build. The checks above use deterministic fixtures and an emulator; they do not prove that every live page variant of every shopping app is recognized. Real-device testers should use non-primary devices when possible, avoid payment and credential pages, share only the privacy-safe diagnostic code and app/device versions, and never send screenshots containing personal order data.

---

# Parcelume 0.3.1 中国区核心平台稳定版说明

这一开发版本暂停继续扩展平台，集中提高淘宝、京东、拼多多、天猫、菜鸟和顺丰的识别准确性、本地隐私保护与问题定位能力。

## 本次改进

- 按独立订单卡片解析，不再把整个页面文字压成一条包裹记录。
- 同一订单存在不同运单号时，保留为多件包裹，不互相覆盖。
- 优先采用页面明确标注的当前状态，并阻止旧页面让包裹状态倒退。
- 页面记录必须包含订单号、运单号或取件码之一，降低误识别。
- 对动态页面进行多次短间隔采样，等待异步加载的订单信息出现。
- 在提取前拒绝付款、银行卡、密码和验证码页面。
- 取消不安全的“仅按标题合并”，增加本地带密钥的取件码指纹用于去重。
- 新增默认关闭的脱敏诊断，只记录结果代码与数量，不记录页面原文、商品名、订单/运单号、地址或电话。
- 所有处理继续在本机完成，Manifest 仍不声明网络权限。

## 已完成验证

- 23 项单元测试，覆盖多订单卡片、一个订单多运单、状态推进、敏感页面拦截和通知解析。
- Android Lint 与 Debug 构建。
- Android 15 模拟器中完成两张淘宝风格订单卡片、两个独立状态的端到端识别。
- 完成数据库版本 3（0.3.0）到版本 4（0.3.1）的覆盖安装升级。
- Manifest 检查确认没有 `INTERNET` 或 `ACCESS_NETWORK_STATE` 权限。

## 测试边界

这仍是调试签名测试包。以上验证使用固定测试样本与模拟器，并不等于所有购物应用的所有真实页面版本都已通过。建议朋友优先使用非主力设备，不进入付款或账号验证页面，只反馈脱敏诊断代码、应用版本和手机系统版本，绝不能发送包含真实订单隐私的截图。
