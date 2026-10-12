package cn.xiaobai.acgfaka.user.core

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 全局 HTTP 层。
 * - 会话 Cookie 持久化到本地，App 重启后自动恢复，避免「会话过期」。
 * - 服务端 UserSession 拦截器校验 Referer host，App 不会自动带，这里统一补上。
 * - 测试环境走内网 IP + http，允许明文；生产建议 https。
 */
object Http {
    @Volatile var baseUrl: String = ""

    private lateinit var appContext: Context
    private const val COOKIE_PREF = "acg_faka_user_cookies"
    private const val COOKIE_KEY = "cookies"

    fun init(context: Context) {
        appContext = context.applicationContext
        loadCookies()
    }

    private val cookieJar = object : CookieJar {
        val store = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(store) {
                val list = store.getOrPut(url.host) { mutableListOf() }
                for (c in cookies) {
                    list.removeAll { it.name == c.name }
                    if (c.expiresAt >= System.currentTimeMillis()) list.add(c)
                }
                persist()
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(store) {
            store[url.host].orEmpty().filter { it.matches(url) && it.expiresAt >= System.currentTimeMillis() }
        }

        fun clear() {
            synchronized(store) {
                store.clear()
                if (::appContext.isInitialized) {
                    appContext.getSharedPreferences(COOKIE_PREF, Context.MODE_PRIVATE).edit().clear().apply()
                }
            }
        }

        private fun persist() {
            if (!::appContext.isInitialized) return
            try {
                val arr = JSONArray()
                store.values.flatten().forEach { c ->
                    val o = JSONObject()
                    o.put("name", c.name)
                    o.put("value", c.value)
                    o.put("domain", c.domain)
                    o.put("path", c.path)
                    o.put("expiresAt", c.expiresAt)
                    o.put("secure", c.secure)
                    o.put("httpOnly", c.httpOnly)
                    o.put("hostOnly", c.hostOnly)
                    o.put("persistent", c.persistent)
                    arr.put(o)
                }
                appContext.getSharedPreferences(COOKIE_PREF, Context.MODE_PRIVATE)
                    .edit().putString(COOKIE_KEY, arr.toString()).apply()
            } catch (_: Exception) {}
        }
    }

    private fun loadCookies() {
        try {
            val s = appContext.getSharedPreferences(COOKIE_PREF, Context.MODE_PRIVATE)
                .getString(COOKIE_KEY, null) ?: return
            val arr = JSONArray(s)
            val now = System.currentTimeMillis()
            synchronized(cookieJar.store) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val expiresAt = o.optLong("expiresAt", 0L)
                    if (expiresAt in 1 until now) continue // 已过期，丢弃
                    val host = o.optString("domain").removePrefix(".")
                    val b = Cookie.Builder()
                        .name(o.optString("name"))
                        .value(o.optString("value"))
                        .path(o.optString("path", "/"))
                    if (expiresAt > 0) b.expiresAt(expiresAt)
                    if (o.optBoolean("secure")) b.secure()
                    if (o.optBoolean("httpOnly")) b.httpOnly()
                    if (o.optBoolean("hostOnly")) b.hostOnlyDomain(host) else b.domain(host)
                    val c = b.build()
                    val list = cookieJar.store.getOrPut(host) { mutableListOf() }
                    list.removeAll { it.name == c.name }
                    list.add(c)
                }
            }
        } catch (_: Exception) {}
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    fun clearSession() = cookieJar.clear()

    fun normalize(input: String): String {
        var s = input.trim().trimEnd('/')
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
        return s
    }

    private fun url(path: String): String {
        val b = baseUrl.trimEnd('/')
        return if (path.startsWith("http")) path else b + (if (path.startsWith("/")) path else "/$path")
    }

    private fun referer(): String = baseUrl.trimEnd('/') + "/"

    private fun parse(text: String, httpCode: Int): JSONObject {
        val json = try { JSONObject(text) } catch (e: Exception) {
            throw ApiException("服务器返回非 JSON（HTTP $httpCode）。请检查地址是否为 ACG-Faka 站点。")
        }
        if (httpCode == 301 || httpCode == 302 || httpCode == 303) throw ApiException("会话失效或发生跳转，请重新登录")
        val code = json.optInt("code", 200)
        if (code != 200) throw ApiException(json.optString("msg", "请求失败（$code）"))
        return json
    }

    fun getSync(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val b = StringBuilder(url(path))
        if (params.isNotEmpty()) {
            b.append(if (b.contains("?")) "&" else "?")
            b.append(params.entries.joinToString("&") { "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}" })
        }
        val req = Request.Builder().url(b.toString())
            .header("Accept", "application/json")
            .header("Referer", referer())
            .header("X-Requested-With", "XMLHttpRequest")
            .get().build()
        client.newCall(req).execute().use { return parse(it.body?.string().orEmpty(), it.code) }
    }

    fun postSync(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val fb = FormBody.Builder()
        params.forEach { (k, v) -> fb.add(k, v) }
        val req = Request.Builder().url(url(path))
            .header("Accept", "application/json")
            .header("Referer", referer())
            .header("X-Requested-With", "XMLHttpRequest")
            .post(fb.build()).build()
        client.newCall(req).execute().use { return parse(it.body?.string().orEmpty(), it.code) }
    }

    fun getBytesSync(path: String): ByteArray {
        val req = Request.Builder().url(url(path))
            .header("Accept", "image/*")
            .header("Referer", referer())
            .get().build()
        client.newCall(req).execute().use {
            if (!it.isSuccessful) throw ApiException("HTTP ${it.code}")
            return it.body?.bytes() ?: throw ApiException("空响应")
        }
    }
}

class ApiException(message: String) : Exception(message)
