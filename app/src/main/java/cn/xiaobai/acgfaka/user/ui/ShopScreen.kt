package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** 套餐项 */
data class ShopLevel(
    val id: Int,
    val name: String,
    val price: Double,
    val topDomain: Boolean,
    val supplier: Boolean
)

/** 店铺信息 */
data class MyShop(
    val hasShop: Boolean,
    val shopName: String,
    val subdomain: String,
    val levelName: String
)

/** 套餐列表 + 当前等级 */
data class ShopLevelsState(val list: List<ShopLevel>, val currentLevel: Int)

private fun fetchLevels(): ShopLevelsState {
    val d = Http.postSync("/user/api/dashboard/levels").optJSONObject("data") ?: JSONObject()
    val arr = d.optJSONArray("list") ?: JSONArray()
    val list = (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        ShopLevel(
            id = Json.int(o, "id"),
            name = Json.str(o, "name"),
            price = Json.dbl(o, "price"),
            topDomain = Json.int(o, "top_domain") == 1,
            supplier = Json.int(o, "supplier") == 1
        )
    }
    return ShopLevelsState(list, Json.int(d, "current_level"))
}

private fun fetchShop(): MyShop {
    val d = Http.postSync("/user/api/dashboard/shop").optJSONObject("data") ?: JSONObject()
    val lv = d.optJSONObject("level")
    val hasShop = Json.int(d, "has_shop") == 1 || (d.has("has_shop") && d.optBoolean("has_shop", false))
    return MyShop(
        hasShop = hasShop,
        shopName = Json.str(d, "shop_name"),
        subdomain = Json.str(d, "subdomain"),
        levelName = if (lv != null) Json.str(lv, "name") else ""
    )
}

private fun purchaseLevel(levelId: Int): String {
    return Http.postSync("/user/api/business/purchase", mapOf("levelId" to levelId.toString())).optString("msg", "开通成功")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var levelsState by remember { mutableStateOf<ShopLevelsState?>(null) }
    var shop by remember { mutableStateOf<MyShop?>(null) }
    var selected by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var buying by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val lv = withContext(Dispatchers.IO) { fetchLevels() }
                levelsState = lv
                shop = withContext(Dispatchers.IO) { runCatching { fetchShop() }.getOrNull() }
                selected = lv.currentLevel
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的店铺") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            val s = shop
            if (s != null && s.hasShop) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("店铺已开通", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        if (s.levelName.isNotBlank()) Text("当前套餐：${s.levelName}", style = MaterialTheme.typography.bodyMedium)
                        if (s.subdomain.isNotBlank()) Text("子域名：${s.subdomain}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                if (s != null && s.hasShop) "升级套餐" else "开通店铺",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text("选择套餐，一次性开通，永久有效", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))

            val lv = levelsState
            if (lv != null) {
                for (item in lv.list) {
                    val isCurrent = item.id == lv.currentLevel
                    val isSelected = item.id == selected
                    Card(
                        onClick = { if (!isCurrent) selected = item.id },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (isCurrent) AssistChip(onClick = {}, label = { Text("当前") })
                                else RadioButton(selected = isSelected, onClick = { selected = item.id })
                            }
                            Text("¥${fmt(item.price)}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            FeatureRow("分站功能", true)
                            FeatureRow("独立域名", item.topDomain)
                            FeatureRow("供货权限", item.supplier)
                        }
                    }
                }
            }

            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }

            Spacer(Modifier.height(16.dp))
            if (lv != null && selected != 0 && selected != lv.currentLevel) {
                Button(
                    onClick = {
                        scope.launch {
                            buying = true; msg = null
                            try {
                                val m = withContext(Dispatchers.IO) { purchaseLevel(selected) }
                                msg = m
                                load()
                            } catch (e: Exception) { msg = e.message ?: "开通失败" } finally { buying = false }
                        }
                    },
                    enabled = !buying,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text(if (buying) "处理中…" else "立即开通") }
            } else if (lv != null && lv.currentLevel == 0) {
                Text("请选择要开通的套餐", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureRow(label: String, enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            if (enabled) Icons.Default.Check else Icons.Default.Close,
            null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
