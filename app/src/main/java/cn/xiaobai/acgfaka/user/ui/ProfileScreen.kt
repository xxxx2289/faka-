package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var qq by remember { mutableStateOf("") }
    var alipay by remember { mutableStateOf("") }
    var wechat by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf(0.0) }
    var loading by remember { mutableStateOf(true) }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true; msg = null
            try {
                val p = withContext(Dispatchers.IO) { Api.personal() }
                username = Json.str(p, "username")
                nickname = Json.str(p, "nickname")
                email = Json.str(p, "email")
                phone = Json.str(p, "phone")
                qq = Json.str(p, "qq")
                alipay = Json.str(p, "alipay")
                wechat = Json.str(p, "wechat")
                balance = Json.dbl(p, "balance")
            } catch (e: Exception) { msg = e.message; isError = true } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人资料") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) { CircularProgressIndicator(); return@Column }

            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(username.ifBlank { "用户" }, style = MaterialTheme.typography.titleLarge)
                    Text("余额 ¥${fmt(balance)}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(nickname, { nickname = it }, Modifier.fillMaxWidth(), label = { Text("昵称") }, singleLine = true)
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("邮箱") }, singleLine = true)
            OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("手机") }, singleLine = true)
            OutlinedTextField(qq, { qq = it }, Modifier.fillMaxWidth(), label = { Text("QQ") }, singleLine = true)
            OutlinedTextField(alipay, { alipay = it }, Modifier.fillMaxWidth(), label = { Text("支付宝账号（兑现用）") }, singleLine = true)
            OutlinedTextField(wechat, { wechat = it }, Modifier.fillMaxWidth(), label = { Text("微信号（兑现用）") }, singleLine = true)

            msg?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }

            Button(
                onClick = {
                    scope.launch {
                        busy = true; msg = null; isError = false
                        try {
                            val m = withContext(Dispatchers.IO) { Api.updatePersonal(nickname, qq, alipay, wechat) }
                            msg = m; isError = false
                        } catch (e: Exception) { msg = e.message ?: "保存失败"; isError = true } finally { busy = false }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (busy) "保存中…" else "保存") }

            Text("注意：邮箱、手机号是否可改取决于站点设置，部分字段需通过安全设置验证。", style = MaterialTheme.typography.labelSmall)
        }
    }
}
