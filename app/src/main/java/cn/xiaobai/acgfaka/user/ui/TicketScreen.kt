package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

/** 工单列表 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的工单") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, "新建工单") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            androidx.compose.runtime.key(reloadKey) {
                SimpleListScreen(
                    nav = nav,
                    title = "",
                    loader = { p, l -> Api.tickets(p, l) },
                    emptyText = "暂无工单",
                    showTopBar = false
                ) { o ->
                    val id = Json.int(o, "id")
                    val title = Json.str(o, "title", "工单")
                    val status = Json.int(o, "status")
                    val time = Json.str(o, "create_time")
                    Card(onClick = { nav.navigate("ticketdetail/$id") }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                                AssistChip(onClick = {}, label = { Text(ticketStatus(status)) })
                            }
                            Text("#${id} · $time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateTicketDialog(
            onDismiss = { showCreate = false },
            onConfirm = { title, content ->
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { Api.ticketCreate(title, content) } }
                    showCreate = false
                    reloadKey++
                }
            }
        )
    }
}

private fun ticketStatus(s: Int): String = when (s) {
    0 -> "待处理"
    1 -> "处理中"
    2 -> "已回复"
    3 -> "已关闭"
    else -> "状态$s"
}

@Composable
private fun CreateTicketDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建工单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(content, { content = it }, label = { Text("问题描述") }, modifier = Modifier.fillMaxWidth().height(120.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, content) }, enabled = title.isNotBlank() && content.isNotBlank()) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

/** 工单详情与回复 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(nav: NavController, id: Int) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var messages by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var reply by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                detail = withContext(Dispatchers.IO) { Api.ticketDetail(id) }
                messages = withContext(Dispatchers.IO) { Api.ticketMessages(id) }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(id) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("工单详情") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = reply, onValueChange = { reply = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入回复") }, maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (reply.isBlank()) return@Button
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { Api.ticketReply(id, reply) } }
                                reply = ""
                                load()
                            }
                        },
                        enabled = reply.isNotBlank()
                    ) { Text("发送") }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            if (loading) { CircularProgressIndicator(); return@Column }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            detail?.let { d ->
                Text(Json.str(d, "title", "工单"), style = MaterialTheme.typography.titleLarge)
                Text("#${id} · ${Json.str(d, "create_time")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
            }
            messages.forEach { m ->
                val mine = Json.int(m, "is_admin") == 0
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(if (mine) "我" else "客服", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Json.str(m, "content"), style = MaterialTheme.typography.bodyMedium)
                        Text(Json.str(m, "create_time"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
