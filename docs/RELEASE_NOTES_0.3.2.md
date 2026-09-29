# Parcelume 0.3.2 China Compatibility Track

## Scope

This build keeps platform expansion paused and focuses on Taobao, Tmall, JD, Pinduoduo, Cainiao, and SF Express.

## Recognition changes

- Each core China app now has its own package-scoped evidence, order labels, tracking labels, and explicit status labels.
- JD delivery numbers, Pinduoduo group-order numbers, Cainiao package numbers, and SF-specific waybill labels are recognized explicitly.
- Food delivery and instant-retail pages are classified and ignored instead of entering the parcel inbox.
- Refund, cancellation, return, and after-sales pages are classified and ignored.
- A refunded card in a mixed order list does not hide unrelated active parcel cards.
- Notification backup applies the same food/instant-delivery and after-sales exclusions.

## Device compatibility

- Adds on-device guidance for Xiaomi/Redmi, Huawei, Honor, and Samsung background/accessibility behavior.
- Optional diagnostics record the device model, Android version, selected shopping-app version, parser version, result code, page kind, and counts.
- A user-initiated Android share sheet exports only this privacy-safe metadata.
- The report never contains raw page text, item names, order numbers, tracking numbers, pickup codes, addresses, phone numbers, account data, or payment information.

## Test boundary

Automated fixtures cover the parser decisions above, but they cannot certify every live page variant. The next evidence must come from privacy-safe reports generated on the target Redmi, Huawei, Honor, and Samsung devices while testers open real order and tracking pages.

---

# Parcelume 0.3.2 中国区兼容性测试版

## 本轮范围

继续暂停扩展平台，集中处理淘宝、天猫、京东、拼多多、菜鸟和顺丰。

## 识别改进

- 六个核心应用分别维护平台专属的页面证据、订单标签、运单标签和当前状态标签。
- 明确支持京东配送单号、拼多多拼单编号、菜鸟包裹单号和顺丰专属运单标签。
- 外卖和即时零售页面会被分类并忽略，不进入包裹列表。
- 退款、取消、退货和售后页面会被分类并忽略。
- 混合订单列表中的退款卡片不会遮蔽其他正常快递卡片。
- 通知辅助识别也应用相同的外卖与售后排除规则。

## 设备兼容

- 增加小米/红米、华为、荣耀和三星的辅助功能及后台限制提示。
- 可选诊断只记录设备型号、Android 版本、购物应用版本、解析器版本、结果代码、页面类型和数量。
- 只有用户主动点击后才会打开 Android 分享面板。
- 报告绝不包含页面原文、商品名称、订单号、运单号、取件码、地址、电话、账号或支付信息。

## 测试边界

自动化样本可以验证上述规则，但不能证明所有真实页面版本都已通过。下一阶段需要朋友在红米、华为、荣耀和三星设备上打开真实订单与物流页面，并只发送应用生成的脱敏报告。
