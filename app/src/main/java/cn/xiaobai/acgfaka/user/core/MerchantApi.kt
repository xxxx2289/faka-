package cn.xiaobai.acgfaka.user.core

import org.json.JSONArray
import org.json.JSONObject

/** 商家经营管理接口，走 /user/api/* 的商家权限。与前台购物共用同一登录会话。 */
object MerchantApi {

    data class Page(val list: List<JSONObject>, val total: Int)

    private fun page(path: String, p: Int, l: Int, extra: Map<String, String> = emptyMap()): Page {
        val params = mutableMapOf("page" to p.toString(), "limit" to l.toString())
        params.putAll(extra)
        val data = Http.postSync(path, params).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    // 经营看板
    data class Dashboard(
        val orderTotal: Int, val orderPending: Int,
        val commodityTotal: Int, val cardStock: Int,
        val balance: Double, val username: String
    )

    fun dashboard(): Dashboard {
        var orderTotal = 0; var orderPending = 0
        var commodityTotal = 0; var cardStock = 0
        var balance = 0.0; var username = ""
        runCatching {
            val d = Http.postSync("/user/api/commodityOrder/data", mapOf("page" to "1", "limit" to "1"))
            orderTotal = (d.optJSONObject("data") ?: JSONObject()).optInt("total", 0)
        }
        runCatching {
            val d = Http.postSync("/user/api/commodityOrder/data", mapOf("page" to "1", "limit" to "1", "delivery_status" to "0"))
            orderPending = (d.optJSONObject("data") ?: JSONObject()).optInt("total", 0)
        }
        runCatching {
            val d = Http.postSync("/user/api/commodity/data", mapOf("page" to "1", "limit" to "1"))
            commodityTotal = (d.optJSONObject("data") ?: JSONObject()).optInt("total", 0)
        }
        runCatching {
            val d = Http.postSync("/user/api/card/data", mapOf("page" to "1", "limit" to "1"))
            cardStock = (d.optJSONObject("data") ?: JSONObject()).optInt("total", 0)
        }
        runCatching {
            val p = Http.getSync("/user/api/security/personal").optJSONObject("data") ?: JSONObject()
            balance = Json.dbl(p, "balance")
            username = Json.str(p, "username")
        }
        return Dashboard(orderTotal, orderPending, commodityTotal, cardStock, balance, username)
    }

    // 商品
    fun commodities(p: Int = 1, l: Int = 15, keyword: String = ""): Page {
        val extra = if (keyword.isNotBlank()) mapOf("name" to keyword) else emptyMap()
        return page("/user/api/commodity/data", p, l, extra)
    }
    fun setCommodityStatus(id: Int, status: Int): String =
        Http.postSync("/user/api/commodity/save", mapOf("id" to id.toString(), "status" to status.toString())).optString("msg", "已更新")
    fun deleteCommodity(id: Int): String =
        Http.postSync("/user/api/commodity/del", mapOf("id" to id.toString())).optString("msg", "已删除")

    // 订单
    fun orders(p: Int = 1, l: Int = 15, keyword: String = "", deliveryStatus: Int? = null): Page {
        val extra = mutableMapOf<String, String>()
        if (keyword.isNotBlank()) extra["trade_no"] = keyword
        if (deliveryStatus != null) extra["delivery_status"] = deliveryStatus.toString()
        return page("/user/api/commodityOrder/data", p, l, extra)
    }
    fun deliver(orderId: Int): String =
        Http.postSync("/user/api/commodityOrder/delivery", mapOf("id" to orderId.toString())).optString("msg", "发货成功")

    // 卡密
    fun cards(p: Int = 1, l: Int = 15, keyword: String = "", commodityId: Int? = null): Page {
        val extra = mutableMapOf<String, String>()
        if (keyword.isNotBlank()) extra["draft"] = keyword
        if (commodityId != null) extra["commodity_id"] = commodityId.toString()
        return page("/user/api/card/data", p, l, extra)
    }
    fun addCards(commodityId: Int, draft: String, premium: Double = 0.0, race: String = ""): String {
        val params = mutableMapOf(
            "commodity_id" to commodityId.toString(),
            "draft" to draft,
            "premium" to premium.toString()
        )
        if (race.isNotBlank()) params["race"] = race
        return Http.postSync("/user/api/card/save", params).optString("msg", "添加成功")
    }
    fun deleteCard(id: Int): String =
        Http.postSync("/user/api/card/del", mapOf("id" to id.toString())).optString("msg", "已删除")

    // 分类
    fun categories(p: Int = 1, l: Int = 50): Page = page("/user/api/category/data", p, l)
}
