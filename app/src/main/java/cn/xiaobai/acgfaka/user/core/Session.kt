package cn.xiaobai.acgfaka.user.core

import android.content.Context

/** 本地持久化：服务器地址、登录态标记。密码和卡密不入库。 */
object Session {
    private const val PREF = "acg_faka_user"
    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun server(ctx: Context): String = sp(ctx).getString("server", "") ?: ""
    fun saveServer(ctx: Context, url: String) = sp(ctx).edit().putString("server", url).apply()

    fun isLoggedIn(ctx: Context): Boolean = sp(ctx).getBoolean("logged_in", false)
    fun setLoggedIn(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("logged_in", v).apply()

    fun username(ctx: Context): String = sp(ctx).getString("username", "") ?: ""
    fun saveUsername(ctx: Context, u: String) = sp(ctx).edit().putString("username", u).apply()
}
