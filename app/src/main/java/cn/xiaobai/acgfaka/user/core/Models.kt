package cn.xiaobai.acgfaka.user.core

import org.json.JSONArray
import org.json.JSONObject

/** 分类节点 */
data class Category(val id: Int, val name: String, val icon: String?, val children: List<Category>)

/** 商品卡片 */
data class Commodity(
    val id: Int,
    val name: String,
    val cover: String,
    val price: Double,
    val userPrice: Double,
    val stock: Int,
    val stockState: Int,
    val categoryId: Int,
    val deliveryWay: Int,
    val tags: List<String>,
    val recommend: Boolean,
    val seckillActive: Boolean,
    val hasWholesale: Boolean,
    val raw: JSONObject
)

/** 商品详情 */
data class CommodityDetail(
    val id: Int,
    val name: String,
    val cover: String,
    val price: Double,
    val userPrice: Double,
    val stock: Int,
    val stockState: Int,
    val description: String,
    val deliveryWay: Int,
    val draftStatus: Int,
    val passwordStatus: Int,
    val raw: JSONObject
)

/** 支付方式 */
data class PayMethod(val id: Int, val name: String, val icon: String?, val handle: String?)

/** 订单查询结果 */
data class OrderInfo(
    val tradeNo: String,
    val amount: Double,
    val status: Int,
    val deliveryStatus: Int,
    val secret: String?,
    val password: String?,
    val createTime: String?,
    val payTime: String?,
    val contact: String?,
    val raw: JSONObject
)

object Json {
    fun optStringOrNull(o: JSONObject, k: String): String? {
        if (!o.has(k) || o.isNull(k)) return null
        val v = o.optString(k, "")
        return v.ifBlank { null }
    }

    fun str(o: JSONObject, k: String, def: String = ""): String = if (o.has(k) && !o.isNull(k)) o.optString(k, def) else def
    fun int(o: JSONObject, k: String, def: Int = 0): Int = if (o.has(k) && !o.isNull(k)) o.optInt(k, def) else def
    fun dbl(o: JSONObject, k: String, def: Double = 0.0): Double = if (o.has(k) && !o.isNull(k)) o.optDouble(k, def) else def
    fun bool(o: JSONObject, k: String): Boolean = if (o.has(k) && !o.isNull(k)) o.optBoolean(k, false) else false

    fun parseTags(o: JSONObject): List<String> {
        val arr = o.optJSONArray("tags") ?: return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            when (val v = arr.opt(i)) {
                is String -> list.add(v)
                is JSONObject -> list.add(v.optString("name", v.optString("text", "")))
            }
        }
        return list.filter { it.isNotBlank() }
    }

    fun toCategory(o: JSONObject): Category {
        val arr = o.optJSONArray("children") ?: JSONArray()
        val children = (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let { toCategory(it) } }
        return Category(int(o, "id"), str(o, "name"), optStringOrNull(o, "icon"), children)
    }

    fun toCommodity(o: JSONObject): Commodity = Commodity(
        id = int(o, "id"),
        name = str(o, "name"),
        cover = str(o, "cover"),
        price = dbl(o, "price"),
        userPrice = dbl(o, "user_price"),
        stock = int(o, "stock"),
        stockState = int(o, "stock_state"),
        categoryId = int(o, "category_id"),
        deliveryWay = int(o, "delivery_way"),
        tags = parseTags(o),
        recommend = int(o, "recommend") == 1,
        seckillActive = bool(o, "seckill_active"),
        hasWholesale = bool(o, "has_wholesale"),
        raw = o
    )

    fun toDetail(o: JSONObject): CommodityDetail = CommodityDetail(
        id = int(o, "id"),
        name = str(o, "name"),
        cover = str(o, "cover"),
        price = dbl(o, "price"),
        userPrice = dbl(o, "user_price"),
        stock = int(o, "stock"),
        stockState = int(o, "stock_state"),
        description = str(o, "description"),
        deliveryWay = int(o, "delivery_way"),
        draftStatus = int(o, "draft_status"),
        passwordStatus = int(o, "password_status"),
        raw = o
    )

    fun toPay(o: JSONObject): PayMethod = PayMethod(int(o, "id"), str(o, "name"), optStringOrNull(o, "icon"), optStringOrNull(o, "handle"))

    fun toOrder(o: JSONObject): OrderInfo = OrderInfo(
        tradeNo = str(o, "trade_no"),
        amount = dbl(o, "amount"),
        status = int(o, "status"),
        deliveryStatus = int(o, "delivery_status"),
        secret = optStringOrNull(o, "secret"),
        password = optStringOrNull(o, "password"),
        createTime = optStringOrNull(o, "create_time"),
        payTime = optStringOrNull(o, "pay_time"),
        contact = optStringOrNull(o, "contact"),
        raw = o
    )
}
