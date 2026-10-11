package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(Http.baseUrl.ifBlank { Session.server(ctx) }.ifBlank { "http://192.168.1.100" }) }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务器设置") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("ACG-Faka 服务器地址", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地址") },
                placeholder = { Text("http://192.168.1.100 或 https://faka.example.com") },
                singleLine = true
            )
            Text("测试环境支持 http 内网地址；生产环境建议使用 https。", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true; msg = null; isError = false
                            try {
                                val norm = Http.normalize(url)
                                Http.baseUrl = norm
                                val info = withContext(Dispatchers.IO) { Api.siteInfo() }
                                Session.saveServer(ctx, norm)
                                msg = "连接成功：${info.shopName}"
                                isError = false
                            } catch (e: Exception) {
                                msg = "连接失败：${e.message}"; isError = true
                            } finally { busy = false }
                        }
                    },
                    enabled = !busy && url.isNotBlank()
                ) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(6.dp)); Text(if (busy) "测试中…" else "保存并测试") }
                OutlinedButton(
                    onClick = {
                        val norm = Http.normalize(url)
                        Http.baseUrl = norm
                        Session.saveServer(ctx, norm)
                        msg = "已保存"; isError = false
                    },
                    enabled = !busy
                ) { Text("仅保存") }
            }
            msg?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            Divider()
            Text("提示：内网测试时请确保手机与服务器在同一局域网，且服务器防火墙开放对应端口。", style = MaterialTheme.typography.labelSmall)
        }
    }
}
