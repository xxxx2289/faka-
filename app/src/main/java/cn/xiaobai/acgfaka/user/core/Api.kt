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
            "keywords" to keywords,
            "categoryId" to categoryId,
            "page" to page.toString(),
            "limit" to limit.toString()
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
            "item_id" to itemId.toString(),
            "num" to num.toString(),
            "race" to race,
            "card_id" to cardId.toString(),
            "coupon" to coupon
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
            "item_id" to itemId.toString(),
            "page" to page.toString(),
            "limit" to limit.toString()
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

    /** 登录：验证码 action 必须是 login */
    fun login(username: String, password: String, captcha: String, code: String = "", remember: Boolean = true): String {
        val params = mutableMapOf(
            "username" to username.trim(),
            "password" to password,
            "captcha" to captcha.trim(),
            "remember" to if (remember) "1" else "0"
        )
        if (code.isNotBlank()) params["code"] = code.trim()
        try {
            val json = Http.postSync("/user/api/authentication/login", params)
            return json.optString("msg", "登录成功")
        } catch (e: ApiException) {
            val m = e.message ?: ""
            if (m.contains("谷歌") || m.contains("两步")) throw NeedTotpException(m)
            throw e
        }
    }

    /** 登录图形验证码：action=login */
    fun loginCaptcha(): ByteArray =
        Http.getBytesSync("/user/captcha/image?action=login&t=${System.currentTimeMillis()}")

    /** 注册图形验证码：action=register */
    fun registerCaptcha(): ByteArray =
        Http.getBytesSync("/user/captcha/image?action=register&t=${System.currentTimeMillis()}")

    /** 发送邮箱注册验证码 */
    fun sendEmailRegisterCaptcha(email: String): String {
        return Http.postSync("/user/api/authentication/emailRegisterCaptcha", mapOf("email" to email.trim())).optString("msg", "发送成功")
    }

    /** 发送手机注册验证码 */
    fun sendPhoneRegisterCaptcha(phone: String): String {
        return Http.postSync("/user/api/authentication/phoneRegisterCaptcha", mapOf("phone" to phone.trim())).optString("msg", "发送成功")
    }

    /**
     * 注册。registered_type: 0=仅用户名 1=手机 2=邮箱。
     * 验证码 action=register。
     */
    fun register(
        username: String,
        password: String,
        captcha: String,
        email: String = "",
        emailCaptcha: String = "",
        phone: String = "",
        phoneCaptcha: String = ""
    ): String {
        val params = mutableMapOf(
            "username" to username.trim(),
            "password" to password,
            "captcha" to captcha.trim()
        )
        if (email.isNotBlank()) params["email"] = email.trim()
        if (emailCaptcha.isNotBlank()) params["email_captcha"] = emailCaptcha.trim()
        if (phone.isNotBlank()) params["phone"] = phone.trim()
        if (phoneCaptcha.isNotBlank()) params["phone_captcha"] = phoneCaptcha.trim()
        return Http.postSync("/user/api/authentication/register", params).optString("msg", "注册成功")
    }

    // ---------- 下单 ----------

    data class TradeResult(val tradeNo: String, val amount: Double, val payUrl: String?, val payHtml: String?)

    fun trade(
        itemId: Int,
        num: Int,
        payId: Int,
        race: String = "",
        sku: Map<String, String> = emptyMap(),
        cardId: Int = 0,
        coupon: String = "",
        contact: String = "",
        password: String = ""
    ): TradeResult {
        val params = mutableMapOf(
            "item_id" to itemId.toString(),
            "num" to num.toString(),
            "pay_id" to payId.toString(),
            "race" to race,
            "card_id" to cardId.toString(),
            "coupon" to coupon,
            "contact" to contact,
            "password" to password
        )
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
            "keywords" to keywords,
            "page" to page.toString(),
            "limit" to limit.toString()
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
    fun personal(): JSONObject = Http.getSync("/user/api/security/personal").optJSONObject("data") ?: JSONObject()
    fun bills(page: Int = 1, limit: Int = 15): JSONObject = Http.postSync("/user/api/bill/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
    fun purchaseRecords(page: Int = 1, limit: Int = 15): JSONObject = Http.postSync("/user/api/purchaseRecord/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
    fun coupons(page: Int = 1, limit: Int = 15): JSONObject = Http.postSync("/user/api/coupon/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
    fun messages(page: Int = 1, limit: Int = 15): JSONObject = Http.postSync("/user/api/message/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
}
