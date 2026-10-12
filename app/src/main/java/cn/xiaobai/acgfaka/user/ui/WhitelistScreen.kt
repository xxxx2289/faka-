package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.Json
import cn.xiaobai.acgfaka.user.core.WhitelistApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<WhitelistApi.WhitelistData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                data = withContext(Dispatchers.IO) { WhitelistApi.overview() }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("对接白名单") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "添加") }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp)) }

            data?.let { d ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("已登记 ${d.count} / ${d.limit} 条", Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            val list = data?.list ?: emptyList()
            if (list.isEmpty() && !loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("还没有登记任何 IP，点右下角添加")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(list) { _, o ->
                        val id = Json.int(o, "id")
                        val ip = Json.str(o, "ip")
                        val note = Json.str(o, "note")
                        InfoCard(
                            title = ip,
                            badge = null,
                            rows = buildList {
                                if (note.isNotBlank()) add("备注" to note)
                                val t = Json.str(o, "create_time")
                                if (t.isNotBlank()) add("添加时间" to t)
                            }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        val m = withContext(Dispatchers.IO) { WhitelistApi.remove(id) }
                                        msg = m; load()
                                    } catch (e: Exception) { error = e.message }
                                }
                            }) { Text("移除", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddWhitelistDialog(
            needPassword = data?.bound != true,
            onDismiss = { showAdd = false },
            onConfirm = { ip, note, code, pwd ->
                scope.launch {
                    try {
                        val m = withContext(Dispatchers.IO) { WhitelistApi.add(ip, note, code, pwd) }
                        msg = m; showAdd = false; load()
                    } catch (e: Exception) { msg = null; error = e.message; showAdd = false }
                }
            }
        )
    }
}

@Composable
private fun AddWhitelistDialog(
    needPassword: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加白名单 IP") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(ip, { ip = it }, label = { Text("IP 或网段") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (needPassword) {
                    OutlinedTextField(password, { password = it }, label = { Text("账号密码") }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
                } else {
                    OutlinedTextField(code, { code = it }, label = { Text("两步验证码") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Text("提示：单个 IPv4/IPv6 地址或网段，最多 20 条。", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(ip, note, code, password) },
                enabled = ip.isNotBlank() && (if (needPassword) password.isNotBlank() else code.isNotBlank())
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
