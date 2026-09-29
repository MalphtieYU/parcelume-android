# Parcelume 0.3.3 Recognition Reliability

## Fixed

- Splits multiple order cards that Android exposes inside one parent accessibility container, preventing a later card's delivery state from being applied to an earlier parcel.
- Ignores partially visible cards that contain only an order identifier and no explicit status, tracking number, or pickup code.
- Keeps explicit pending states such as "order submitted" recognizable without treating every identifier-only fragment as a new pending parcel.

## Emulator verification

The six core package identities—Taobao, Tmall, JD, Pinduoduo, Cainiao, and SF Express—were exercised repeatedly with:

- multiple parcels on one page;
- one order split across two tracking numbers;
- delayed/asynchronous page rendering;
- repeated captures of the same parcel;
- status progression followed by an older status;
- payment, food/instant-delivery, refund/after-sales, and unrelated pages.

All synthetic positive cases were captured without duplicate rows. Status progressed from shipped to in transit and did not regress. All 24 negative-page checks were rejected without writing parcel data.

This verification uses controlled accessibility trees. It improves confidence but does not certify every current or future live shopping-app layout; testers should continue using privacy-safe diagnostics on real devices.

---

# Parcelume 0.3.3 识别稳定性版本

## 已修复

- 当 Android 把多张订单卡片合并进同一个辅助功能父节点时，先按订单/运单标识拆分后再解析，避免后一张卡片的物流状态串到前一件包裹。
- 页面滚动边缘若只露出订单编号、没有明确状态、运单号或取件码，不再误存为“待处理”包裹。
- “下单成功/订单提交成功”等明确待处理状态仍可正常识别，不会因为上述保护而漏掉。

## 模拟器验证

以淘宝、天猫、京东、拼多多、菜鸟和顺丰 6 个真实包名反复模拟了：

- 同页多包裹；
- 同一订单拆成两个运单；
- 页面延迟加载；
- 同一包裹重复识别；
- 状态前进后再次出现旧状态；
- 支付、外卖/即时配送、退款/售后和无关页面。

所有合成正向场景均正确写入且没有重复记录；状态从“已发货”升级到“运输中”后没有倒退；24 次负向页面测试均被拦截，没有写入包裹数据。

这些结果基于受控的辅助功能页面树，能够提高可信度，但不能代表已经覆盖购物应用当前及未来的所有真实布局；仍需继续收集真实设备上的脱敏诊断结果。
