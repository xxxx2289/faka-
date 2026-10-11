package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Category
import cn.xiaobai.acgfaka.user.core.Commodity
import cn.xiaobai.acgfaka.user.core.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var cats by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selected by remember { mutableStateOf<Category?>(null) }
    var goods by remember { mutableStateOf<List<Commodity>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun loadGoods(catId: String) {
        scope.launch {
            loading = true; error = null
            try {
                val res = withContext(Dispatchers.IO) { Api.commodities(categoryId = catId, page = 1, limit = 50) }
                goods = res.list
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; return@launch }
                val res = withContext(Dispatchers.IO) { Api.categories() }
                cats = res
                if (res.isNotEmpty()) { selected = res.first(); loadGoods(res.first().id.toString()) }
            } catch (e: Exception) { error = e.message }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("分类") })
        if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error!!, color = MaterialTheme.colorScheme.error) }
            return@Column
        }
        Row(Modifier.fillMaxSize()) {
            Surface(Modifier.width(110.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceVariant) {
                LazyColumn {
                    items(cats, key = { it.id }) { c ->
                        val sel = selected?.id == c.id
                        Surface(
                            color = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { selected = c; loadGoods(c.id.toString()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(c.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            }
                        }
                        Divider()
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    goods.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("该分类暂无商品") }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(goods, key = { it.id }) { c -> CommodityCard(c) { nav.navigate("detail/${c.id}") } }
                    }
                }
            }
        }
    }
}
