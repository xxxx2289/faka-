package cn.xiaobai.acgfaka.user.core

import org.json.JSONArray
import org.json.JSONObject

/** 对接白名单接口，对应 /user/api/security/ipWhitelist* */
object WhitelistApi {

    data class WhitelistData(
        val list: List<JSONObject>,
        val limit: Int,
        val count: Int,
        val bound: Boolean
    )

    fun overview(): WhitelistData {
        val d = Http.postSync("/user/api/security/ipWhitelist").optJSONObject("data") ?: JSONObject()
        val arr = d.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return WhitelistData(
            list = list,
            limit = Json.int(d, "limit", 20),
            count = list.size,
            bound = d.has("bound") && d.optBoolean("bound", false)
        )
    }

    /** 添加 IP。开了两步验证传 code，没开传 password。 */
    fun add(ip: String, note: String = "", code: String = "", password: String = ""): String {
        val params = mutableMapOf("ip" to ip.trim())
        if (note.isNotBlank()) params["note"] = note
        if (code.isNotBlank()) params["code"] = code
        if (password.isNotBlank()) params["password"] = password
        return Http.postSync("/user/api/security/ipWhitelistAdd", params).optString("msg", "已加入白名单")
    }

    fun remove(id: Int): String {
        return Http.postSync("/user/api/security/ipWhitelistRemove", mapOf("id" to id.toString())).optString("msg", "已移出白名单")
    }
}
