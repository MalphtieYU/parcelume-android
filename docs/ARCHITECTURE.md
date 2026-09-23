# 本地架构

```text
用户选定的 App 通知
        │
        ▼
NotificationListenerService
        │
        ▼
本机规则解析器
  ├─ 相关性判断
  ├─ 状态识别
  ├─ 运单号提取
  └─ 取件码提取
        │
        ▼
SQLite 本地数据库
        │
        ├─ Compose 极简列表
        └─ WorkManager 自动清理
```

## 组件职责

- `ParcelNotificationListener`：只接收并转交用户启用来源的通知。
- `ParcelNotificationParser`：纯本地、确定性规则，不调用网络服务。
- `ParcelDatabase`：保存结构化结果并执行安全清理。
- `ParcelRepository`：向 UI 暴露当前包裹列表。
- `CleanupWorker`：每天清理超过保留期的已完成记录。
- `MainActivity`：权限提示、列表、详情和本地设置。

## 后续扩展边界

如果未来接入物流 API，应新增独立的 provider 接口，并把联网能力做成用户主动开启的选项。API 密钥不能直接写入公开客户端仓库。
