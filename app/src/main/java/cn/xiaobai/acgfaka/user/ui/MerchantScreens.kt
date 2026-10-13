package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.Json
import cn.xiaobai.acgfaka.user.core.MerchantApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDashboardScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var d by remember { mutableStateOf<MerchantApi.Dashboard?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                d = withContext(Dispatchers.IO) { MerchantApi.dashboard() }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("经营看板") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            if (loading) { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Column }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val data = d ?: return@Column
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(data.username.ifBlank { "商家" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("余额 ¥${fmt(data.balance)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MerchantStat("订单总数", data.orderTotal.toString(), Modifier.weight(1f))
                MerchantStat("待发货", data.orderPending.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MerchantStat("商品数", data.commodityTotal.toString(), Modifier.weight(1f))
                MerchantStat("卡密库存", data.cardStock.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("快捷入口", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MerchantEntry(Icons.Default.ReceiptLong, "订单", Modifier.weight(1f)) { nav.navigate("m_orders") }
                MerchantEntry(Icons.Default.Inventory2, "商品", Modifier.weight(1f)) { nav.navigate("m_commodities") }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MerchantEntry(Icons.Default.Key, "卡密", Modifier.weight(1f)) { nav.navigate("m_cards") }
                MerchantEntry(Icons.Default.Category, "分类", Modifier.weight(1f)) { nav.navigate("m_categories") }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { load() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("刷新数据") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MerchantStat(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MerchantEntry(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---------- 商家订单（发货）----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantOrdersScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf<Int?>(null) }
    var keyword by remember { mutableStateOf("") }
    val pageSize = 15

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { MerchantApi.orders(p, pageSize, keyword, filter) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("商家订单") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = keyword, onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                label = { Text("订单号") }, singleLine = true,
                trailingIcon = { TextButton(onClick = { load(1) }) { Text("搜索") } }
            )
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == null, onClick = { filter = null; load(1) }, label = { Text("全部") })
                FilterChip(selected = filter == 0, onClick = { filter = 0; load(1) }, label = { Text("待发货") })
                FilterChip(selected = filter == 1, onClick = { filter = 1; load(1) }, label = { Text("已发货") })
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
            if (list.isEmpty() && !loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无订单") }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(list) { _, o ->
                        val id = Json.int(o, "id")
                        val tradeNo = Json.str(o, "trade_no")
                        val amount = Json.dbl(o, "amount")
                        val status = Json.int(o, "status")
                        val delivery = Json.int(o, "delivery_status")
                        val commodity = o.optJSONObject("commodity")?.let { Json.str(it, "name") } ?: "商品"
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(commodity, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                                    AssistChip(onClick = {}, label = { Text(if (status == 1) "已支付" else "待支付") })
                                }
                                Text(tradeNo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("¥${fmt(amount)} · 发货：${if (delivery == 1) "已发" else "未发"}", style = MaterialTheme.typography.bodySmall)
                                if (status == 1 && delivery != 1) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try { withContext(Dispatchers.IO) { MerchantApi.deliver(id) }; load(page) } catch (e: Exception) { error = e.message }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("手动发货") }
                                }
                            }
                        }
                    }
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
}

// ---------- 商家商品管理 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantCommoditiesScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var keyword by remember { mutableStateOf("") }
    val pageSize = 15

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { MerchantApi.commodities(p, pageSize, keyword) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("商品管理") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = keyword, onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                label = { Text("商品名称") }, singleLine = true,
                trailingIcon = { TextButton(onClick = { load(1) }) { Text("搜索") } }
            )
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
            if (list.isEmpty() && !loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无商品") }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(list) { _, o ->
                        val id = Json.int(o, "id")
                        val name = Json.str(o, "name")
                        val price = Json.dbl(o, "price")
                        val stock = Json.int(o, "stock")
                        val status = Json.int(o, "status")
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                                    AssistChip(onClick = {}, label = { Text(if (status == 1) "上架" else "下架") })
                                }
                                Text("#${id} · ¥${fmt(price)} · 库存 $stock", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                try { withContext(Dispatchers.IO) { MerchantApi.setCommodityStatus(id, if (status == 1) 0 else 1) }; load(page) } catch (e: Exception) { error = e.message }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(if (status == 1) "下架" else "上架") }
                                    OutlinedButton(
                                        onClick = { nav.navigate("m_cards") },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("管理卡密") }
                                }
                            }
                        }
                    }
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
}

// ---------- 商家卡密管理 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantCardsScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val pageSize = 15

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { MerchantApi.cards(p, pageSize) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("卡密管理") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") }
                    IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "添加") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
            if (list.isEmpty() && !loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无卡密") }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(list) { _, o ->
                        val id = Json.int(o, "id")
                        val draft = Json.str(o, "draft")
                        val status = Json.int(o, "status")
                        val premium = Json.dbl(o, "draft_premium")
                        val cid = Json.int(o, "commodity_id")
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("#${id}  商品ID $cid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(draft, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                Row {
                                    AssistChip(onClick = {}, label = { Text(if (status == 0) "未售" else "已售") })
                                    if (premium > 0) { Spacer(Modifier.width(6.dp)); AssistChip(onClick = {}, label = { Text("+¥${fmt(premium)}") }) }
                                    Spacer(Modifier.weight(1f))
                                    if (status == 0) {
                                        TextButton(onClick = {
                                            scope.launch {
                                                try { withContext(Dispatchers.IO) { MerchantApi.deleteCard(id) }; load(page) } catch (e: Exception) { error = e.message }
                                            }
                                        }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                    }
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

    if (showAdd) {
        var commodityId by remember { mutableStateOf("") }
        var draft by remember { mutableStateOf("") }
        var premium by remember { mutableStateOf("0") }
        var race by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加卡密") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(commodityId, { commodityId = it }, label = { Text("商品 ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(draft, { draft = it }, label = { Text("卡密内容（每行一条）") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                    OutlinedTextField(premium, { premium = it }, label = { Text("加价（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(race, { race = it }, label = { Text("规格/种族（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            loading = true; error = null
                            try {
                                withContext(Dispatchers.IO) { MerchantApi.addCards(commodityId.toIntOrNull() ?: 0, draft, premium.toDoubleOrNull() ?: 0.0, race) }
                                showAdd = false; load(1)
                            } catch (e: Exception) { error = e.message; showAdd = false } finally { loading = false }
                        }
                    },
                    enabled = commodityId.toIntOrNull() != null && draft.isNotBlank()
                ) { Text("提交") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }
}

// ---------- 商家分类管理 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantCategoriesScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                list = withContext(Dispatchers.IO) { MerchantApi.categories().list }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
            if (list.isEmpty() && !loading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无分类") }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(list) { _, o ->
                        ListItem(
                            headlineContent = { Text(Json.str(o, "name")) },
                            supportingContent = { val sid = Json.int(o, "id"); Text("#" + sid, style = MaterialTheme.typography.labelSmall) },
                            trailingContent = { AssistChip(onClick = {}, label = { Text(if (Json.int(o, "status") == 1) "显示" else "隐藏") }) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
