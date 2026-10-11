package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.OrderInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var list by remember { mutableStateOf<List<OrderInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var secretDialog by remember { mutableStateOf<Pair<String, String?>?>(null) }

    fun search() {
        if (keyword.isBlank()) { error = "请输入订单号或联系方式"; return }
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { Api.queryOrders(keyword) }
                list = res.list
                if (res.list.isEmpty()) error = "未查询到相关订单"
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    fun viewSecret(order: OrderInfo, password: String = "") {
        scope.launch {
            try {
                val s = withContext(Dispatchers.IO) { Api.orderSecret(order.tradeNo, password) }
                secretDialog = order.tradeNo to (s.secret ?: s.leaveMessage ?: "无卡密内容")
            } catch (e: Exception) {
                secretDialog = order.tradeNo to "查询失败：${e.message}"
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("订单查询") })
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            label = { Text("订单号(18位) 或 联系方式") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { TextButton(onClick = { search() }) { Text("查询") } }
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp))
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list, key = { it.tradeNo }) { o ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(o.tradeNo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            AssistChip(onClick = {}, label = { Text(statusLabel(o.status)) })
                        }
                        Row {
                            Text("金额 ¥${fmt(o.amount)}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Text(o.createTime ?: "", style = MaterialTheme.typography.labelSmall)
                        }
                        if (o.status == 1) {
                            Button(onClick = { viewSecret(o) }, modifier = Modifier.fillMaxWidth()) { Text("查看卡密") }
                        } else {
                            Text("订单未支付", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    secretDialog?.let { (no, content) ->
        AlertDialog(
            onDismissRequest = { secretDialog = null },
            title = { Text("卡密内容") },
            text = {
                Column {
                    Text(no, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(content ?: "无", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { secretDialog = null }) { Text("关闭") } }
        )
    }
}

private fun statusLabel(s: Int): String = when (s) {
    1 -> "已支付"
    0 -> "待支付"
    else -> "状态$s"
}
