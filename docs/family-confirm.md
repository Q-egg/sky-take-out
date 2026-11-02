# 家庭模拟付款

原支付页与跳转路径保留。点击“确认支付”打开自定义密码弹层，调用新增的
`PUT /user/order/familyConfirm`。成功后显示订单表中的金额，明确提示未实际扣款，
确认弹窗后进入原成功页。不调用微信预支付、收银台或支付通知接口。

## 使用

1. 重新启动后端，并在微信开发者工具中重新编译当前小程序。
2. 正常下单，在原支付页点击“确认支付”。默认六位测试密码为 `123456`。
3. 密码仅为本项目的模拟确认码，不是微信支付密码。

后端配置默认值位于新增的 `FamilyConfirmServiceImpl`，无需修改原 YAML：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `sky.family.enabled` | `true` | 可设为 `false` 关闭新接口 |
| `sky.family.password` | `123456` | 六位数字测试密码 |

也可通过 `SKY_FAMILY_ENABLED`、`SKY_FAMILY_PASSWORD` 环境变量覆盖。
该接口沿用 `/user/**` 登录校验，仅允许确认当前用户自己的订单。
本次未新增家庭成员名单管理。

## 数据变化

首次确认只更新尚未支付、未超时（15 分钟）的订单：

- `status = 2`：待接单。
- `pay_method = 3`：家庭确认。
- `pay_status = 3`：无需支付。
- `amount` 不修改，仍不产生真实扣款。
- 首次家庭确认成功时写入 `checkout_time`，记录本次模拟确认时间，供订单详情中的支付时间展示。
- 确认接口通过现有 `checkOutTime` 属性返回该时间；重复确认保留首次时间，旧订单的空时间不自动补填。

新增取值仅在新 Mapper/Service 中使用，原实体常量、下单、微信支付和通知代码没有修改。
随课程资料提供的表结构为数值字段，可容纳上述取值；没有执行数据库结构变更。
后续编写退款、统计时需要识别家庭订单，不应把它们计入真实收款。
重复确认不会让已接单、配送中或已完成的家庭订单退回待接单。

## 小程序修改位置

当前直接修改的是已经编译的小程序：

`E:\JavaAgent\苍穹外卖\mp-weixin\mp-weixin\pages\pay\`

仅修改 `index.js`、`index.wxml`、`index.wxss`；未修改公共请求封装及其他页面。
请求复用当前构建中的公共 request 模块，保留原服务地址和登录令牌。
这些文件不在后端 Git 仓库中，提交后端不会自动包含小程序改动。
若以后从原始 uni-app 工程重新编译，需要把相同改动同步到 `pages/pay/index.vue`，
否则编译产物会被覆盖。

## 恢复微信支付

1. 将 `sky.family.enabled` 设为 `false`。
2. 在支付页 `index.js` 中取消 `handleWechatPay()` 外层块注释。该方法完整保留了原预支付请求和 `wx.requestPayment()` 调用。
3. 把当前 `handleSave()` 的方法体改为 `this.handleWechatPay();`。原按钮绑定无需修改。
4. 使用新建的普通订单验证真实支付，不要把家庭确认订单再次用于扣款。

原 `/user/order/payment`、`WeChatPayUtil`、`/notify/paySuccess` 均未改动。
原真实支付金额仍固定为 `0.01` 元；正式接入商户号时还需要检查金额、商户配置和支付回调，
本次保留原代码不代表真实支付已完成联调。

### 无商户号时的启动配置

`application.yml` 和本地 `application-dev.yml` 中未使用的 `mchid`、`mchSerialNo`、
`privateKeyFilePath`、`apiV3Key`、`weChatPayCertFilePath` 已保留原文并注释。
原来的 `${sky.wechat.xxx}` 自引用会在未填写商户资料时造成循环占位符异常，阻止应用启动。
微信登录使用的 `appid` 和 `secret` 保持有效。以后接入真实支付时，在开发配置中填写实际值后，
再恢复主配置对应项目；不要直接恢复两份文件中的自引用而不提供实际值。

## 验证

前端行为测试：`node scripts/test-family-confirm.cjs`。

后端定向测试：

`mvn -o -pl sky-server -am test -Dtest=FamilyConfirm*Test -Dsurefire.failIfNoSpecifiedTests=false -Dnet.bytebuddy.experimental=true`

Java 测试在 `sky-server/src/test/java`，现有 `.gitignore` 忽略该目录；未修改忽略规则。
测试不连接真实支付平台，不修改真实订单。微信真机交互与实际数据库写入仍需本地联调。
原后端尚未实现的订单详情、取消等功能不在本次范围内。
