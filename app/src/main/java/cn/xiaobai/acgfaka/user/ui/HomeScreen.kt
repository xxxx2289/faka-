package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Commodity
import cn.xiaobai.acgfaka.user.core.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<Commodity>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var keyword by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("异次元商城") }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load(p: Int, append: Boolean) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先在「我的」里配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { Api.commodities(keywords = search, categoryId = "0", page = p, limit = 20) }
                list = if (append) list + res.list else res.list
                total = res.total; page = p
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                if (Http.baseUrl.isNotBlank()) {
                    val info = withContext(Dispatchers.IO) { Api.siteInfo() }
                    shopName = info.shopName.ifBlank { "异次元商城" }
                    notice = info.notice
                }
            } catch (_: Exception) {}
            load(1, false)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(shopName) },
            actions = {
                IconButton(onClick = { nav.navigate("server") }) { Icon(Icons.Default.Settings, "服务器") }
            }
        )
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            placeholder = { Text("搜索商品") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            trailingIcon = { TextButton(onClick = { search = keyword; load(1, false) }) { Text("搜索") } }
        )
        if (!notice.isNullOrBlank()) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Text("公告：$notice", Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        when {
            loading && list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null && list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { load(1, false) }) { Text("重试") }
                }
            }
            list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无商品") }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.id }) { c ->
                    CommodityCard(c) { nav.navigate("detail/${c.id}") }
                }
                item {
                    if (list.size < total) {
                        OutlinedButton(
                            onClick = { load(page + 1, true) },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (loading) "加载中…" else "加载更多 (${list.size}/$total)") }
                    }
                }
            }
        }
    }
}
