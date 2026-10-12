package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(Session.isLoggedIn(ctx)) }
    var username by remember { mutableStateOf(Session.username(ctx)) }
    var personal by remember { mutableStateOf<JSONObject?>(null) }
    var serverUrl by remember { mutableStateOf(Http.baseUrl.ifBlank { Session.server(ctx) }) }

    fun refreshProfile() {
        if (!loggedIn) return
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { Api.personal() } }.onSuccess { personal = it }
        }
    }

    LaunchedEffect(loggedIn) { refreshProfile() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopAppBar(title = { Text("我的") })

        Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (loggedIn) (username.ifBlank { "已登录用户" }) else "未登录",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                    )
                    personal?.let { p ->
                        val balance = if (p.has("balance")) p.optDouble("balance") else null
                        val email = if (p.has("email")) p.optString("email") else ""
                        Text(
                            buildString {
                                if (email.isNotBlank()) append(email)
                                if (balance != null) { if (isNotEmpty()) append(" · "); append("余额 ¥${fmt(balance)}") }
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (!loggedIn) Button(onClick = { nav.navigate("login") }) { Text("登录") }
            }
        }

        Spacer(Modifier.height(8.dp))

        SectionTitle("我的账户")
        MineItem(Icons.Default.Dashboard, "我的主页") { nav.navigate("dashboard") }
        MineItem(Icons.Default.Storefront, "我的店铺") { nav.navigate("shop") }

        SectionTitle("店铺")
        MineItem(Icons.Default.ShoppingCart, "购买商品") { nav.navigate("home") }
        MineItem(Icons.Default.ReceiptLong, "购买记录") { nav.navigate("purchase") }
        MineItem(Icons.Default.Search, "订单查询") { nav.navigate("orders") }
        MineItem(Icons.Default.ConfirmationNumber, "我的优惠券") { nav.navigate("coupons") }

        SectionTitle("财务")
        MineItem(Icons.Default.AddCard, "充值中心") { nav.navigate("recharge") }
        MineItem(Icons.Default.AccountBalanceWallet, "硬币兑现") { nav.navigate("cash") }
        MineItem(Icons.Default.Receipt, "我的账单") { nav.navigate("bills") }

        SectionTitle("推广")
        MineItem(Icons.Default.Share, "推广中心") { nav.navigate("promote") }
        MineItem(Icons.Default.People, "我的下级") { nav.navigate("agentmembers") }

        SectionTitle("客服与消息")
        MineItem(Icons.Default.SupportAgent, "我的工单") { nav.navigate("tickets") }
        MineItem(Icons.Default.Mail, "消息中心") { nav.navigate("messages") }

        SectionTitle("账户安全")
        MineItem(Icons.Default.Person, "个人资料") { nav.navigate("profile") }
        MineItem(Icons.Default.Lock, "修改密码") { nav.navigate("password") }
        MineItem(Icons.Default.Security, "安全设置") { nav.navigate("security") }
        MineItem(Icons.Default.ListAlt, "对接白名单") { nav.navigate("whitelist") }
        MineItem(Icons.Default.Settings, "服务器设置") { nav.navigate("server") }

        if (loggedIn) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    Http.clearSession()
                    Session.setLoggedIn(ctx, false)
                    Session.saveUsername(ctx, "")
                    loggedIn = false; username = ""; personal = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { Text("退出登录") }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "当前服务器：${serverUrl.ifBlank { "未配置" }}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun MineItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider(Modifier.padding(start = 56.dp))
}
