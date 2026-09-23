# 本地架构

```text
所选购物 App 当前可见页面 ──→ ParcelAccessibilityService ─┐
                                                          ├─→ 本机规则解析器
所选购物 App 的可选通知 ────→ ParcelNotificationListener ─┘
                                                                  │
                                                                  ▼
                                                必要结构化字段 + 来源标记
                                                                  │
                                                                  ▼
                                                       AES-GCM 加密 SQLite 本地数据库
                                                                  │
                                              ┌───────────────────┴──────────┐
                                              ▼                              ▼
                                      Compose 包裹界面              WorkManager 自动清理
```

## 组件职责

- `ParcelAccessibilityService`：只接收固定支持包名且由用户开启来源的窗口事件；不执行任何页面操作。
- `ParcelScreenParser`：要求订单或物流证据，提取商品标题、订单号、运单号、取件码和状态。
- `ParcelNotificationListener`：可选辅助来源，只转交用户启用来源的通知。
- `ParcelNotificationParser`：本地解析通知中的必要物流字段。
- `ParcelDatabase`：保存结构化结果，按运单号或同一来源订单号合并更新，并执行安全清理。
- `ParcelRepository`：向 UI 暴露当前包裹列表。
- `CleanupWorker`：每天清理超过保留期的已完成记录。
- `MainActivity`：拉起 Android 自己的辅助功能与通知权限页面。

## 安全边界

- 页面与通知原文只存在于解析调用期间，不写入数据库。
- 页面服务不点击、不滚动、不输入、不执行手势、不截屏。
- 来源默认全部关闭，页面识别还要求版本化的应用内明确同意。
- 0.3 Manifest 不申请网络权限。
- 商品名称、订单号、运单号与取件码使用 Android Keystore 密钥加密；匹配索引使用带密钥指纹。
- 付款与身份验证页面拒绝解析，敏感行先过滤，密码节点不读取。

## 后续扩展边界

实时物流需要联网查询承运商或合规聚合服务。未来应新增独立 provider 接口，将联网追踪做成用户主动开启的能力，只传递必要的承运商和运单号。API 密钥不能直接写入公开客户端仓库。
