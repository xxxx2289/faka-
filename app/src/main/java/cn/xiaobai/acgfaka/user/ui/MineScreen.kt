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
        // 用户信息卡
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

        MineItem(Icons.Default.ReceiptLong, "购买记录") { nav.navigate("orders") }
        MineItem(Icons.Default.AccountBalanceWallet, "我的账单") { }
        MineItem(Icons.Default.ConfirmationNumber, "我的优惠券") { }
        MineItem(Icons.Default.Mail, "站内消息") { }
        MineItem(Icons.Default.SupportAgent, "工单支持") { }
        MineItem(Icons.Default.Settings, "服务器设置") { nav.navigate("server") }

        if (loggedIn) {
            Spacer(Modifier.height(12.dp))
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
private fun MineItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider(Modifier.padding(start = 56.dp))
}
