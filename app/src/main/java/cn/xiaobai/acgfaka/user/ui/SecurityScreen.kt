package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("两步验证", "登录设备", "安全日志")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("安全设置") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            when (tab) {
                0 -> TotpPanel(scope)
                1 -> DevicePanel(scope)
                else -> LogsPanel(scope)
            }
        }
    }
}

@Composable
private fun TotpPanel(scope: kotlinx.coroutines.CoroutineScope) {
    var status by remember { mutableStateOf<JSONObject?>(null) }
    var secret by remember { mutableStateOf<JSONObject?>(null) }
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            loading = true
            status = withContext(Dispatchers.IO) { runCatching { Api.totpStatus() }.getOrNull() }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    val enabled = status?.let { Json.int(it, "status", Json.int(it, "enabled")) == 1 } ?: false

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (loading) { CircularProgressIndicator(); return@Column }
        Surface(color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Text(if (enabled) "两步验证已开启" else "两步验证未开启", Modifier.padding(14.dp), style = MaterialTheme.typography.titleMedium)
        }
        if (!enabled) {
            Button(onClick = {
                scope.launch {
                    secret = withContext(Dispatchers.IO) { runCatching { Api.totpSecret() }.getOrNull() }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("获取密钥") }
            secret?.let { s ->
                val sec = Json.str(s, "secret")
                val qr = Json.str(s, "qr", Json.str(s, "url"))
                if (sec.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("密钥（在验证器 App 中手动添加）", style = MaterialTheme.typography.labelSmall)
                            Text(sec, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (qr.isNotBlank()) {
                    Text("二维码地址：$qr", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("输入验证码启用") }, singleLine = true)
                Button(onClick = {
                    scope.launch {
                        try {
                            val m = withContext(Dispatchers.IO) { Api.totpEnable(code) }
                            msg = m; isError = false; code = ""; load()
                        } catch (e: Exception) { msg = e.message; isError = true }
                    }
                }, enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("启用") }
            }
        } else {
            OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("输入验证码关闭") }, singleLine = true)
            Button(
                onClick = {
                    scope.launch {
                        try { withContext(Dispatchers.IO) { Api.totpDisable(code) }; msg = "已关闭"; isError = false; code = ""; load() }
                        catch (e: Exception) { msg = e.message; isError = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = code.isNotBlank(), modifier = Modifier.fillMaxWidth()
            ) { Text("关闭两步验证") }
        }
        msg?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun DevicePanel(scope: kotlinx.coroutines.CoroutineScope) {
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            loading = true
            list = withContext(Dispatchers.IO) { runCatching { Api.deviceSessions().list }.getOrDefault(emptyList()) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = {
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { Api.revokeAllDeviceSessions() } }
                    load()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) { Text("下线全部其他设备") }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (list.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无登录设备记录") }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(list) { _, o ->
                    InfoCard(
                        title = Json.str(o, "device", Json.str(o, "platform", "设备")),
                        badge = Json.str(o, "ip"),
                        rows = listOf(
                            "登录时间" to Json.str(o, "create_time"),
                            "位置" to Json.str(o, "location", "未知")
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LogsPanel(scope: kotlinx.coroutines.CoroutineScope) {
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        list = withContext(Dispatchers.IO) { runCatching { Api.securityLogs().list }.getOrDefault(emptyList()) }
        loading = false
    }

    if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); return }
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无安全日志") }
    } else {
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(list) { _, o ->
                InfoCard(
                    title = Json.str(o, "action", Json.str(o, "type", "操作")),
                    badge = Json.str(o, "ip"),
                    rows = buildList {
                        val d = Json.str(o, "description", Json.str(o, "content"))
                        if (d.isNotBlank()) add("详情" to d)
                        add("时间" to Json.str(o, "create_time"))
                    }
                )
            }
        }
    }
}
