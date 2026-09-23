# Parcelume 0.3.0 Privacy Preview

[简体中文](#简体中文) · [English](#english)

## 简体中文

这是一次以隐私保护、可控权限和多平台覆盖为重点的预览版本。

### 新增与改进

- 支持同时选择 19 个购物与物流应用：淘宝、京东、拼多多、天猫、菜鸟、顺丰、Amazon、Temu、AliExpress、eBay、Walmart、SHEIN、Etsy、Alibaba.com、Lazada、Shopee Indonesia、Flipkart、Mercado Libre、日本乐天市场。
- 首次使用时集中说明读取范围并由用户确认；正常启动不重复弹出。
- 增加“暂停全部识别”和“撤销同意”，即使系统权限尚未关闭，服务也会立即停止处理。
- 默认隐藏真实商品名称；用户可以在设置中主动开启。
- 商品名称、订单号、运单号和取件码使用 Android Keystore 支持的 AES-GCM 加密保存。
- 付款、银行卡、密码、验证码页面不解析；地址、电话、邮箱和支付信息行在提取前过滤。
- 使用系统防窥标志阻止 Parcelume 界面被截屏、录屏或出现在最近任务预览中。
- 扩展中文、英文、西班牙文、葡萄牙文、印度尼西亚文和日文的物流识别规则。

### 安装与使用

1. 下载 `parcelume-0.3.0-debug.apk`，在 Android 8.0 或更高版本安装。
2. 阅读权限说明，选择允许识别的平台并确认一次。
3. Parcelume 会打开 Android 的辅助功能设置，系统权限必须由用户亲自开启。
4. 在所选购物应用中打开订单详情、下单成功或物流页面，Parcelume 会在本机提取可识别字段。
5. 通知访问是可选补充，不开启也可使用页面识别。

### 隐私与限制

- APK 不申请 `INTERNET` 或 `ACCESS_NETWORK_STATE`，没有账号、服务器或云同步。
- 不保存完整页面、原始通知、截图或购物平台密码。
- 只能识别用户实际打开且 Android 能提供可读文字的受支持页面，不能读取购物应用私有数据库，也不能保证零遗漏。
- 当前版本没有联网查询快递接口；状态更新依赖再次打开可识别页面或可选通知。
- 此 APK 使用调试签名，仅供体验与测试，不应视为应用商店正式发行版。

**SHA-256**

`7e06b74ccbb19aa9bc5c33a756b342c042d23973b2876e2e2ff5d5b83bacf34f`

## English

This preview focuses on privacy protection, controllable permissions, and broader platform coverage.

### What's new

- Simultaneous selection across 19 shopping and logistics apps: Taobao, JD, Pinduoduo, Tmall, Cainiao, SF Express, Amazon, Temu, AliExpress, eBay, Walmart, SHEIN, Etsy, Alibaba.com, Lazada, Shopee Indonesia, Flipkart, Mercado Libre, and Rakuten Ichiba.
- A prominent one-time disclosure before screen access; normal launches do not repeat it.
- Pause-all and revoke-consent controls stop processing even if Android system access remains enabled.
- Real item names are hidden by default and can be enabled explicitly.
- Item titles, order references, tracking numbers, and pickup codes are stored with Android Keystore-backed AES-GCM encryption.
- Payment, card, password, and verification-code screens are rejected; address, phone, email, and payment rows are filtered before extraction.
- Android display protection keeps Parcelume out of screenshots, screen recordings, and recent-app previews.
- Expanded deterministic parsing for Chinese, English, Spanish, Portuguese, Indonesian, and Japanese logistics text.

### Install and use

1. Download `parcelume-0.3.0-debug.apk` and install it on Android 8.0 or later.
2. Review the disclosure, select allowed sources, and confirm once.
3. Parcelume opens Android Accessibility settings; only the user can grant system access.
4. Open an order confirmation, order detail, or delivery page in a selected app. Recognized fields are processed on device.
5. Notification access is an optional backup and is not required for visible-page capture.

### Privacy and limitations

- The APK requests neither `INTERNET` nor `ACCESS_NETWORK_STATE`; there is no account, server, or cloud sync.
- Full pages, raw notifications, screenshots, and shopping credentials are not stored.
- Parcelume can only recognize supported pages that the user opens and Android exposes as readable text. It cannot access shopping apps' private databases or guarantee zero omissions.
- This version has no online carrier API; updates require another recognizable page or an optional notification.
- This APK is debug-signed for evaluation and is not a production app-store release.

**SHA-256**

`7e06b74ccbb19aa9bc5c33a756b342c042d23973b2876e2e2ff5d5b83bacf34f`
