# 异次元商城 · 用户前台 App

ACG-Faka 发卡系统的原生 Android 用户端。Kotlin + Jetpack Compose。

## 功能
- 服务器地址配置（测试环境支持 http 内网 IP）
- 首页商品瀑布流、关键词搜索、分类筛选、加载更多
- 商品详情、数量选择、实时估价
- 支付方式选择、下单、订单状态轮询
- 「对局式」下单体验：支付中 → 对局进行中 → 发货成功弹出卡密
- 订单查询（订单号 / 联系方式）与卡密查看
- 登录（含图形验证码、谷歌验证码）、个人中心、退出

## 对接的真实接口（已按 acg-faka 3.8.3 源码核对）
- GET  /user/api/site/info
- GET  /user/api/index/data
- GET  /user/api/index/commodity
- GET  /user/api/index/commodityDetail
- GET  /user/api/index/pay
- POST /user/api/index/valuation
- POST /user/api/index/stock
- POST /user/api/index/card
- POST /user/api/index/query
- POST /user/api/index/secret
- POST /user/api/authentication/login
- POST /user/api/order/trade
- POST /user/api/order/state
- GET  /user/captcha/image?action=userLogin

## 构建
GitHub Actions 工作流会构建 debug APK，产物名 acg-faka-user-debug-apk。

## 说明
- 会话用内存 CookieJar 维持，退出即清除，不落盘密码。
- 明文 HTTP 通过 network_security_config 放行，仅用于内网测试。
