package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoteScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val d = withContext(Dispatchers.IO) { Api.promoteData() }
                data = d
                val arr = d.opt("list")
                list = if (arr is JSONArray) (0 until arr.length()).mapNotNull { arr.optJSONObject(it) } else emptyList()
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("推广中心") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") } }
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val d = data
                if (d != null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("推广数据", style = MaterialTheme.typography.titleMedium)
                            val link = Json.str(d, "url", Json.str(d, "link"))
                            val count = Json.int(d, "count", Json.int(d, "total"))
                            val commission = Json.dbl(d, "commission", Json.dbl(d, "amount"))
                            if (link.isNotBlank()) {
                                Text("推广链接", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                                    Text(link, Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text("推广人数：$count")
                            Text("累计佣金：¥${fmt(commission)}")
                        }
                    }
                }
            }
            if (list.isEmpty() && !loading) {
                item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("暂无推广记录") } }
            }
            itemsIndexed(list) { _, o ->
                InfoCard(
                    title = Json.str(o, "username", "下级用户"),
                    badge = "ID ${Json.int(o, "id")}",
                    rows = buildList {
                        val amt = Json.dbl(o, "amount")
                        if (amt != 0.0) add("佣金" to "¥${fmt(amt)}")
                        val t = Json.str(o, "create_time")
                        if (t.isNotBlank()) add("时间" to t)
                    }
                )
            }
        }
    }
}
