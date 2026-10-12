package cn.xiaobai.acgfaka.user.core

import android.content.Context
import cn.xiaobai.acgfaka.user.BuildConfig

/** 本地持久化：服务器地址、登录态标记。密码和卡密不入库。 */
object Session {
    private const val PREF = "acg_faka_user"
    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** 服务器地址：优先本地保存值，没有则用构建时固定的默认值 */
    fun server(ctx: Context): String {
        val saved = sp(ctx).getString("server", "") ?: ""
        return saved.ifBlank { BuildConfig.DEFAULT_SERVER }
    }

    fun saveServer(ctx: Context, url: String) = sp(ctx).edit().putString("server", url).apply()

    fun isLoggedIn(ctx: Context): Boolean = sp(ctx).getBoolean("logged_in", false)
    fun setLoggedIn(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("logged_in", v).apply()

    fun username(ctx: Context): String = sp(ctx).getString("username", "") ?: ""
    fun saveUsername(ctx: Context, u: String) = sp(ctx).edit().putString("username", u).apply()
}
