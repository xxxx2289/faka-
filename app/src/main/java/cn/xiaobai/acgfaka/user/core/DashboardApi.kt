package cn.xiaobai.acgfaka.user.core

import org.json.JSONArray
import org.json.JSONObject

/** 看板接口，对应服务端 /user/api/dashboard/*（需部署 Dashboard.php） */
object DashboardApi {

    data class Asset(
        val balance: Double, val coin: Double, val integral: Int,
        val recharge: Double, val totalCoin: Double
    )

    data class Consume(
        val buyMonth: Double, val buyTotal: Double, val buyCount: Int
    )

    data class Level(
        val name: String, val recharge: Double,
        val nextName: String, val nextRecharge: Double
    )

    data class Business(
        val todayIncome: Double, val yesterdayIncome: Double, val weekIncome: Double, val monthIncome: Double,
        val tradeTotal: Double, val todayOrders: Int, val pendingDelivery: Int,
        val cardUnsold: Int, val commodityOnline: Int, val commodityCount: Int
    )

    data class Overview(
        val username: String, val avatar: String,
        val asset: Asset, val consume: Consume, val level: Level,
        val isBusiness: Boolean, val business: Business?
    )

    fun overview(): Overview {
        val d = Http.postSync("/user/api/dashboard/index").optJSONObject("data") ?: JSONObject()
        val a = d.optJSONObject("asset") ?: JSONObject()
        val c = d.optJSONObject("consume") ?: JSONObject()
        val l = d.optJSONObject("level") ?: JSONObject()
        val asset = Asset(
            Json.dbl(a, "balance"), Json.dbl(a, "coin"), Json.int(a, "integral"),
            Json.dbl(a, "recharge"), Json.dbl(a, "total_coin")
        )
        val consume = Consume(Json.dbl(c, "buy_month"), Json.dbl(c, "buy_total"), Json.int(c, "buy_count"))
        val level = Level(
            Json.str(l, "name"), Json.dbl(l, "recharge"),
            Json.str(l, "next_name"), Json.dbl(l, "next_recharge")
        )
        val bizObj = d.optJSONObject("business")
        val business = bizObj?.let {
            Business(
                Json.dbl(it, "today_income"), Json.dbl(it, "yesterday_income"),
                Json.dbl(it, "week_income"), Json.dbl(it, "month_income"),
                Json.dbl(it, "trade_total"), Json.int(it, "today_orders"),
                Json.int(it, "pending_delivery"), Json.int(it, "card_unsold"),
                Json.int(it, "commodity_online"), Json.int(it, "commodity_count")
            )
        }
        return Overview(
            Json.str(d, "username"), Json.str(d, "avatar"),
            asset, consume, level,
            Json.int(d, "is_business") == 1 || it_bool(d, "is_business"),
            business
        )
    }

    private fun it_bool(o: JSONObject, k: String): Boolean =
        if (o.has(k) && !o.isNull(k)) o.optBoolean(k, false) else false

    data class LevelItem(val id: Int, val name: String, val icon: String, val price: Double,
                         val substation: Int, val topDomain: Int, val supplier: Int, val cost: Double)
    data class LevelsResult(val list: List<LevelItem>, val currentLevel: Int)

    fun levels(): LevelsResult {
        val d = Http.postSync("/user/api/dashboard/levels").optJSONObject("data") ?: JSONObject()
        val arr = d.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { o ->
                LevelItem(
                    Json.int(o, "id"), Json.str(o, "name"), Json.str(o, "icon"),
                    Json.dbl(o, "price"), Json.int(o, "substation"),
                    Json.int(o, "top_domain"), Json.int(o, "supplier"), Json.dbl(o, "cost")
                )
            }
        }
        return LevelsResult(list, Json.int(d, "current_level"))
    }

    data class ShopInfo(val hasShop: Boolean, val shopName: String, val subdomain: String, val topdomain: String,
                        val levelId: Int, val levelName: String)

    fun shop(): ShopInfo {
        val d = Http.postSync("/user/api/dashboard/shop").optJSONObject("data") ?: JSONObject()
        val lv = d.optJSONObject("level")
        return ShopInfo(
            Json.int(d, "has_shop") == 1 || (d.has("has_shop") && d.optBoolean("has_shop", false)),
            Json.str(d, "shop_name"), Json.str(d, "subdomain"), Json.str(d, "topdomain"),
            lv?.let { Json.int(it, "id") } ?: 0,
            lv?.let { Json.str(it, "name") } ?: ""
        )
    }

    /** 购买套餐 */
    fun purchaseLevel(levelId: Int): String {
        return Http.postSync("/user/api/business/purchase", mapOf("levelId" to levelId.toString())).optString("msg", "开通成功")
    }
}
