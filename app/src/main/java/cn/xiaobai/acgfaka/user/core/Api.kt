package cn.xiaobai.acgfaka.user.core

import org.json.JSONArray
import org.json.JSONObject

/** ACG-Faka 前台接口封装。路径已按源码核对。 */
object Api {

    // ---------- 站点与首页 ----------
    data class SiteInfo(val shopName: String, val title: String, val notice: String?, val registeredState: Int)

    fun siteInfo(): SiteInfo {
        val d = Http.getSync("/user/api/site/info").optJSONObject("data") ?: JSONObject()
        return SiteInfo(
            shopName = Json.str(d, "shop_name", "异次元商城"),
            title = Json.str(d, "title"),
            notice = Json.optStringOrNull(d, "notice"),
            registeredState = Json.int(d, "registered_state")
        )
    }

    fun categories(): List<Category> {
        val data = Http.getSync("/user/api/index/data").opt("data")
        return when (data) {
            is JSONArray -> (0 until data.length()).mapNotNull { data.optJSONObject(it)?.let { Json.toCategory(it) } }
            else -> emptyList()
        }
    }

    data class CommodityPage(val list: List<Commodity>, val total: Int)

    fun commodities(keywords: String = "", categoryId: String = "0", page: Int = 1, limit: Int = 20): CommodityPage {
        val json = Http.getSync("/user/api/index/commodity", mapOf(
            "keywords" to keywords, "categoryId" to categoryId,
            "page" to page.toString(), "limit" to limit.toString()
        ))
        val arr = json.opt("data")
        val list = if (arr is JSONArray) (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { o -> Json.toCommodity(o) } } else emptyList()
        return CommodityPage(list, json.optInt("total", list.size))
    }

    fun commodityDetail(id: Int): CommodityDetail {
        val d = Http.getSync("/user/api/index/commodityDetail", mapOf("commodityId" to id.toString())).optJSONObject("data") ?: JSONObject()
        return Json.toDetail(d)
    }

    fun payMethods(): List<PayMethod> {
        val arr = Http.getSync("/user/api/index/pay").opt("data")
        return if (arr is JSONArray) (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { o -> Json.toPay(o) } } else emptyList()
    }

    fun valuation(itemId: Int, num: Int, race: String = "", sku: Map<String, String> = emptyMap(), cardId: Int = 0, coupon: String = ""): Double {
        val params = mutableMapOf(
            "item_id" to itemId.toString(), "num" to num.toString(), "race" to race,
            "card_id" to cardId.toString(), "coupon" to coupon
        )
        sku.forEach { (k, v) -> params["sku[$k]"] = v }
        val d = Http.postSync("/user/api/index/valuation", params).optJSONObject("data") ?: JSONObject()
        return Json.dbl(d, "price")
    }

    data class StockInfo(val stock: Int, val stockState: Int)

    fun stock(itemId: Int, race: String = "", sku: Map<String, String> = emptyMap()): StockInfo {
        val params = mutableMapOf("item_id" to itemId.toString(), "race" to race)
        sku.forEach { (k, v) -> params["sku[$k]"] = v }
        val d = Http.postSync("/user/api/index/stock", params).optJSONObject("data") ?: JSONObject()
        return StockInfo(Json.int(d, "stock"), Json.int(d, "stock_state"))
    }

    data class DraftCard(val id: Int, val draft: String, val premium: Double)
    data class DraftPage(val list: List<DraftCard>, val total: Int)

    fun draftCards(itemId: Int, page: Int = 1, limit: Int = 20, keyword: String = ""): DraftPage {
        val params = mutableMapOf(
            "item_id" to itemId.toString(), "page" to page.toString(), "limit" to limit.toString()
        )
        if (keyword.isNotBlank()) params["search-draft"] = keyword
        val json = Http.postSync("/user/api/index/card", params)
        val data = json.optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { o -> DraftCard(Json.int(o, "id"), Json.str(o, "draft"), Json.dbl(o, "draft_premium")) }
        }
        return DraftPage(list, data.optInt("total", list.size))
    }

    // ---------- 账户 ----------
    class NeedTotpException(message: String) : Exception(message)

    fun login(username: String, password: String, captcha: String, code: String = "", remember: Boolean = true): String {
        val params = mutableMapOf(
            "username" to username.trim(), "password" to password,
            "captcha" to captcha.trim(), "remember" to if (remember) "1" else "0"
        )
        if (code.isNotBlank()) params["code"] = code.trim()
        try {
            return Http.postSync("/user/api/authentication/login", params).optString("msg", "登录成功")
        } catch (e: ApiException) {
            val m = e.message ?: ""
            if (m.contains("谷歌") || m.contains("两步")) throw NeedTotpException(m)
            throw e
        }
    }

    fun loginCaptcha(): ByteArray = Http.getBytesSync("/user/captcha/image?action=login&t=${System.currentTimeMillis()}")
    fun registerCaptcha(): ByteArray = Http.getBytesSync("/user/captcha/image?action=register&t=${System.currentTimeMillis()}")
    /** 下单验证码，仅当站点开启 trade_verification 时需要 */
    fun tradeCaptcha(): ByteArray = Http.getBytesSync("/user/captcha/image?action=trade&t=${System.currentTimeMillis()}")

    fun sendEmailRegisterCaptcha(email: String): String =
        Http.postSync("/user/api/authentication/emailRegisterCaptcha", mapOf("email" to email.trim())).optString("msg", "发送成功")

    fun sendPhoneRegisterCaptcha(phone: String): String =
        Http.postSync("/user/api/authentication/phoneRegisterCaptcha", mapOf("phone" to phone.trim())).optString("msg", "发送成功")

    fun register(
        username: String, password: String, captcha: String,
        email: String = "", emailCaptcha: String = "", phone: String = "", phoneCaptcha: String = ""
    ): String {
        val params = mutableMapOf(
            "username" to username.trim(), "password" to password, "captcha" to captcha.trim()
        )
        if (email.isNotBlank()) params["email"] = email.trim()
        if (emailCaptcha.isNotBlank()) params["email_captcha"] = emailCaptcha.trim()
        if (phone.isNotBlank()) params["phone"] = phone.trim()
        if (phoneCaptcha.isNotBlank()) params["phone_captcha"] = phoneCaptcha.trim()
        return Http.postSync("/user/api/authentication/register", params).optString("msg", "注册成功")
    }

    // ---------- 下单 ----------
    data class TradeResult(val tradeNo: String, val amount: Double, val payUrl: String?, val payHtml: String?)

    fun trade(itemId: Int, num: Int, payId: Int, race: String = "", sku: Map<String, String> = emptyMap(), cardId: Int = 0, coupon: String = "", contact: String = "", password: String = "", captcha: String = ""): TradeResult {
        val params = mutableMapOf(
            "item_id" to itemId.toString(), "num" to num.toString(), "pay_id" to payId.toString(),
            "race" to race, "card_id" to cardId.toString(), "coupon" to coupon,
            "contact" to contact, "password" to password
        )
        if (captcha.isNotBlank()) params["captcha"] = captcha
        sku.forEach { (k, v) -> params["sku[$k]"] = v }
        val d = Http.postSync("/user/api/order/trade", params).optJSONObject("data") ?: JSONObject()
        return TradeResult(
            tradeNo = Json.str(d, "trade_no"),
            amount = Json.dbl(d, "amount"),
            payUrl = Json.optStringOrNull(d, "url") ?: Json.optStringOrNull(d, "pay_url"),
            payHtml = Json.optStringOrNull(d, "html") ?: Json.optStringOrNull(d, "pay_html")
        )
    }

    fun orderState(tradeNo: String): OrderInfo {
        val d = Http.postSync("/user/api/order/state", mapOf("tradeNo" to tradeNo)).optJSONObject("data") ?: JSONObject()
        return Json.toOrder(d)
    }

    // ---------- 订单查询 ----------
    data class OrderQueryPage(val list: List<OrderInfo>, val total: Int)

    fun queryOrders(keywords: String, page: Int = 1, limit: Int = 10): OrderQueryPage {
        val json = Http.postSync("/user/api/index/query", mapOf(
            "keywords" to keywords, "page" to page.toString(), "limit" to limit.toString()
        ))
        val data = json.optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { o -> Json.toOrder(o) } }
        return OrderQueryPage(list, data.optInt("total", list.size))
    }

    data class SecretResult(val secret: String?, val leaveMessage: String?, val widget: JSONObject?)

    fun orderSecret(tradeNo: String, password: String = ""): SecretResult {
        val d = Http.postSync("/user/api/index/secret", mapOf("tradeNo" to tradeNo, "password" to password)).optJSONObject("data") ?: JSONObject()
        return SecretResult(
            secret = Json.optStringOrNull(d, "secret"),
            leaveMessage = Json.optStringOrNull(d, "leave_message"),
            widget = d.optJSONObject("widget")
        )
    }

    // ---------- 用户中心 ----------
    data class Page(val list: List<JSONObject>, val total: Int)

    fun personal(): JSONObject = Http.getSync("/user/api/security/personal").optJSONObject("data") ?: JSONObject()

    fun updatePersonal(nickname: String? = null, qq: String? = null, alipay: String? = null, wechat: String? = null): String {
        val params = mutableMapOf<String, String>()
        nickname?.let { params["nickname"] = it }
        qq?.let { params["qq"] = it }
        alipay?.let { params["alipay"] = it }
        wechat?.let { params["wechat"] = it }
        return Http.postSync("/user/api/security/personal", params).optString("msg", "已保存")
    }

    fun changePassword(oldPassword: String, newPassword: String, code: String = ""): String {
        val params = mutableMapOf("old_password" to oldPassword, "new_password" to newPassword)
        if (code.isNotBlank()) params["code"] = code
        return Http.postSync("/user/api/security/password", params).optString("msg", "密码已修改")
    }

    fun totpStatus(): JSONObject = Http.postSync("/user/api/security/totpStatus").optJSONObject("data") ?: JSONObject()
    fun totpSecret(): JSONObject = Http.postSync("/user/api/security/totpSecret").optJSONObject("data") ?: JSONObject()
    fun totpEnable(code: String): String = Http.postSync("/user/api/security/totpEnable", mapOf("code" to code)).optString("msg", "已开启")
    fun totpDisable(code: String = "", password: String = ""): String {
        val params = mutableMapOf<String, String>()
        if (code.isNotBlank()) params["code"] = code
        if (password.isNotBlank()) params["password"] = password
        return Http.postSync("/user/api/security/totpDisable", params).optString("msg", "已关闭")
    }
    fun deviceSessions(): Page {
        val data = Http.postSync("/user/api/security/deviceSessions", emptyMap()).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }
    fun revokeAllDeviceSessions(): String = Http.postSync("/user/api/security/revokeAllDeviceSessions").optString("msg", "已下线全部设备")
    fun securityLogs(page: Int = 1, limit: Int = 15): Page {
        val data = Http.postSync("/user/api/security/logs", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    // ---------- 账单 / 购买记录 ----------
    fun bills(page: Int = 1, limit: Int = 15): Page {
        val data = Http.postSync("/user/api/bill/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    fun purchaseRecords(page: Int = 1, limit: Int = 15): Page {
        val data = Http.postSync("/user/api/purchaseRecord/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    // ---------- 充值 ----------
    fun rechargePayMethods(): List<PayMethod> {
        val arr = Http.postSync("/user/api/recharge/pay").opt("data")
        return if (arr is JSONArray) (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { o -> Json.toPay(o) } } else emptyList()
    }

    data class RechargeResult(val tradeNo: String, val payUrl: String?, val payHtml: String?)

    fun rechargeTrade(amount: Double, payId: Int): RechargeResult {
        val d = Http.postSync("/user/api/recharge/trade", mapOf("amount" to amount.toString(), "pay_id" to payId.toString())).optJSONObject("data") ?: JSONObject()
        return RechargeResult(
            tradeNo = Json.str(d, "trade_no"),
            payUrl = Json.optStringOrNull(d, "url") ?: Json.optStringOrNull(d, "pay_url"),
            payHtml = Json.optStringOrNull(d, "html") ?: Json.optStringOrNull(d, "pay_html")
        )
    }

    // ---------- 提现 ----------
    fun cashSubmit(type: Int, amount: Double): String {
        return Http.postSync("/user/api/cash/submit", mapOf("type" to type.toString(), "amount" to amount.toString())).optString("msg", "提现申请已提交")
    }

    fun cashRecords(page: Int = 1, limit: Int = 15): Page {
        val data = Http.postSync("/user/api/cash/record", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    // ---------- 推广 ----------
    fun promoteData(page: Int = 1, limit: Int = 15): JSONObject {
        return Http.postSync("/user/api/promote/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
    }

    // ---------- 我的下级 ----------
    fun agentMembers(page: Int = 1, limit: Int = 15, keyword: String = ""): Page {
        val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
        if (keyword.isNotBlank()) params["username"] = keyword
        val data = Http.postSync("/user/api/agentMember/data", params).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    fun transferToMember(userId: Int, amount: Double): String {
        return Http.postSync("/user/api/agentMember/transfer", mapOf("id" to userId.toString(), "amount" to amount.toString())).optString("msg", "转账成功")
    }

    // ---------- 优惠券 ----------
    fun coupons(page: Int = 1, limit: Int = 15): Page {
        val data = Http.postSync("/user/api/coupon/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    // ---------- 工单 ----------
    fun tickets(page: Int = 1, limit: Int = 15): Page {
        val data = Http.postSync("/user/api/ticket/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    fun ticketDetail(id: Int): JSONObject = Http.postSync("/user/api/ticket/detail", mapOf("id" to id.toString())).optJSONObject("data") ?: JSONObject()
    fun ticketMessages(id: Int): List<JSONObject> {
        val arr = Http.postSync("/user/api/ticket/messages", mapOf("id" to id.toString())).opt("data")
        return if (arr is JSONArray) (0 until arr.length()).mapNotNull { arr.optJSONObject(it) } else emptyList()
    }
    fun ticketCreate(title: String, content: String, commodityId: Int = 0, orderId: Int = 0): String {
        val params = mutableMapOf("title" to title, "content" to content)
        if (commodityId > 0) params["commodity_id"] = commodityId.toString()
        if (orderId > 0) params["order_id"] = orderId.toString()
        return Http.postSync("/user/api/ticket/create", params).optString("msg", "提交成功")
    }
    fun ticketReply(id: Int, content: String): String =
        Http.postSync("/user/api/ticket/reply", mapOf("id" to id.toString(), "content" to content)).optString("msg", "已回复")

    // ---------- 消息 ----------
    fun messages(page: Int = 1, limit: Int = 15): Page {
        val data = Http.postSync("/user/api/message/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }
    fun messageDetail(id: Int): JSONObject = Http.postSync("/user/api/message/detail", mapOf("id" to id.toString())).optJSONObject("data") ?: JSONObject()
    fun messageClear(): String = Http.postSync("/user/api/message/clear").optString("msg", "已清空")
}
