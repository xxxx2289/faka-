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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 通用分页列表页。render 必须放在最后一个参数，尾随 lambda 才能绑定。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleListScreen(
    nav: NavController,
    title: String,
    loader: (Int, Int) -> Api.Page,
    emptyText: String = "暂无数据",
    pageSize: Int = 15,
    showTopBar: Boolean = true,
    render: @Composable (JSONObject) -> Unit
) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { loader(p, pageSize) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load(1) }

    Column(Modifier.fillMaxSize()) {
        if (showTopBar) {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        if (list.isEmpty() && !loading && error == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(emptyText) }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(list) { _, item -> render(item) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("第 $page 页 · 共 $total 条", style = MaterialTheme.typography.labelMedium)
            Row {
                OutlinedButton(onClick = { load(page - 1) }, enabled = !loading && page > 1) { Text("上一页") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { load(page + 1) }, enabled = !loading && page * pageSize < total) { Text("下一页") }
            }
        }
    }
}

/** 通用信息卡：标题行 + 若干键值对 */
@Composable
fun InfoCard(title: String, badge: String? = null, rows: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                if (!badge.isNullOrBlank()) AssistChip(onClick = {}, label = { Text(badge) })
            }
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(84.dp))
                    Text(v, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
