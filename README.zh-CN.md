# Parcelume

> 一款隐私优先、完全本地运行的 Android 包裹收件箱。

简体中文 · [English](README.md)

Parcelume 会把用户选定的购物与物流应用所产生的快递通知，整理成一个安静、清晰、可搜索的包裹时间线。它不要求注册账号、不连接服务器，也不会导入购物历史。

> [!IMPORTANT]
> Parcelume 目前是 Android 早期测试版本。它只能在完成设置后识别**之后出现的受支持通知**。仅仅安装应用不会自动获得历史订单；如果购物或物流应用没有发出有效通知，Parcelume 也无法提供实时物流更新。

## 核心特点

- **自动整理**：从受支持的通知中提取必要的包裹信息，不需要手工记账。
- **完全本地**：结构化记录只保存在手机本地 SQLite 数据库中。
- **没有联网权限**：当前版本无法上传通知或包裹数据。
- **来源可控**：没有被用户选中的应用会在解析前直接忽略。
- **仅浏览模式**：拒绝授权后仍可查看界面，同时持续显示权限提醒。
- **保留时间可选**：已完成记录可保留一周、一个月、一年或永久。
- **中英文界面**：只切换语言，不改变功能和设计。

## 界面预览

| 包裹首页 | 权限说明 | Android 系统授权 |
| --- | --- | --- |
| <img src="docs/images/home.png" width="240" alt="Parcelume 包裹首页"> | <img src="docs/images/permission-setup.png" width="240" alt="Parcelume 权限说明"> | <img src="docs/images/system-access.png" width="240" alt="Parcelume 的 Android 系统通知访问页面"> |

## 工作原理

```text
用户选定应用的通知
        ↓
Android 通知监听服务
        ↓
手机本地规则解析器
        ↓
SQLite 中的必要结构化字段
        ↓
Parcelume 包裹与待取页面
```

Parcelume 可能提取来源、物流状态、运单号、取件码和更新时间。商品名称只有在通知本身包含时才能识别。完整通知正文只在内存中处理，不会写入数据库。

## 下载测试版

可在预览版本页面下载 [`parcelume-0.1.0-debug.apk`](https://github.com/MalphtieYU/parcelume-android/releases/tag/v0.1.0-preview)。这是使用 Debug 签名的功能测试包，不是正式发行版。安装时 Android 可能要求允许浏览器或文件管理器“安装未知应用”；请只对你信任的安装来源临时开启，安装后可再关闭。

## 使用方法

1. 在 Android 8.0 或更高版本的设备上安装 APK。
2. 打开 Parcelume，阅读首次使用隐私说明。
3. 只选择你希望 Parcelume 处理的购物或物流应用。
4. 点击“开启通知访问”。
5. Parcelume 会打开手机真正的 Android 系统设置页；请在系统界面亲自授权，然后返回应用。
6. 之后出现的匹配物流通知会自动进入 Parcelume。

如果选择“暂不开启，仅浏览界面”，应用仍可正常进入；在通知权限和来源没有配置完整之前，首页会一直显示提醒。

## 系统权限说明

通知访问权限完全由 Android 系统控制，Parcelume 无法静默授权。

- Android 11 及以上：设备支持时，优先打开 Parcelume 对应的系统通知访问详情页。
- 旧版或深度定制系统：自动回退到系统“通知访问”应用列表。
- 以上入口都不存在：回退到系统设置首页。

三星、小米、OPPO、vivo、华为、荣耀、一加、Google Pixel 等系统的菜单名称可能不同。部分厂商还可能要求用户在电池或自启动设置中允许后台运行。Parcelume 不会为了绕过厂商限制而申请与功能无关的权限。

## 隐私边界

| 项目 | Parcelume 的处理方式 |
| --- | --- |
| 网络 | 不申请 `INTERNET` 或 `ACCESS_NETWORK_STATE` 权限 |
| 账号 | 没有 Parcelume 账号、登录和云同步 |
| 通知范围 | Android 的通知监听授权本身较宽；Parcelume 会先按用户选中的应用包名过滤，再读取通知字段 |
| 本地存储 | 只保存提取后的包裹字段，不保存完整通知、截图或附件 |
| 其他个人数据 | 不读取联系人、短信、照片、位置，不使用无障碍服务，不索取购物平台密码 |
| 删除方式 | 用户可设置保留时间，也可以一键删除全部本地数据 |

完整边界请查看[隐私说明](docs/PRIVACY.zh-CN.md)。

## 当前支持来源

MVP 已包含淘宝、京东、拼多多、菜鸟、顺丰和 Amazon Shopping 通知的本地识别规则。

“支持”表示 Parcelume 可以识别已知通知格式，并不表示本项目与这些公司存在合作或代表关系。来源应用修改通知文案后，识别准确率可能暂时下降。

## 注意事项与限制

- 不会导入过去的订单。
- 被关闭、隐藏、延迟或内容过于简单的通知无法识别。
- 只有来源应用发出有效通知时，包裹状态才会更新。
- 通知不包含商品名时，Parcelume 也无法知道购买了什么。
- 不同地区和应用版本的通知文案、包名可能不同。
- 当前 Debug APK 仅供测试，不是正式签名发行版。

## 从源码构建

需要：带 Android SDK 37 的 Android Studio、JDK 17，以及 Android 8.0 及以上设备或模拟器。

```bash
git clone https://github.com/MalphtieYU/parcelume-android.git
cd parcelume-android
./gradlew assembleDebug
```

APK 生成位置：`app/build/outputs/apk/debug/app-debug.apk`。

运行测试与检查：

```bash
./gradlew testDebugUnitTest lintDebug
```

## 项目状态

当前 MVP 已完成 Android 15 模拟器界面与权限流程测试、“通知 → 本地记录”端到端测试、通知解析器单元测试、Android Lint 和 Manifest 无联网权限检查。在发布稳定版本前，仍需收集不同厂商真机上的脱敏通知样例并完成兼容性验证。

## 参与贡献

欢迎提交 Issue 和 Pull Request，尤其是脱敏后的通知格式说明、解析器测试和设备兼容性反馈。请勿公开真实运单号、取件码、地址、电话号码或完整通知内容。

## 开源许可

本项目采用 [Apache License 2.0](LICENSE)。
