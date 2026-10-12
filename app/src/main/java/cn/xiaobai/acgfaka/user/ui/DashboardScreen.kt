package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 资产 */
data class DashAsset(val balance: Double, val coin: Double, val integral: Int, val recharge: Double, val totalCoin: Double)

/** 消费 */
data class DashConsume(val buyMonth: Double, val buyTotal: Double, val buyCount: Int)

/** 等级 */
data class DashLevel(val name: String, val recharge: Double, val nextName: String, val nextRecharge: Double)

/** 经营数据 */
data class DashBusiness(
    val todayIncome: Double, val yesterdayIncome: Double, val weekIncome: Double, val monthIncome: Double,
    val tradeTotal: Double, val todayOrders: Int, val pendingDelivery: Int,
    val cardUnsold: Int, val commodityOnline: Int, val commodityCount: Int
)

/** 看板总览 */
data class DashOverview(
    val username: String, val avatar: String,
    val asset: DashAsset, val consume: DashConsume, val level: DashLevel,
    val isBusiness: Boolean, val business: DashBusiness?
)

fun fetchOverview(): DashOverview {
    val d = Http.postSync("/user/api/dashboard/index").optJSONObject("data") ?: JSONObject()
    val a = d.optJSONObject("asset") ?: JSONObject()
    val c = d.optJSONObject("consume") ?: JSONObject()
    val l = d.optJSONObject("level") ?: JSONObject()
    val asset = DashAsset(
        Json.dbl(a, "balance"), Json.dbl(a, "coin"), Json.int(a, "integral"),
        Json.dbl(a, "recharge"), Json.dbl(a, "total_coin")
    )
    val consume = DashConsume(Json.dbl(c, "buy_month"), Json.dbl(c, "buy_total"), Json.int(c, "buy_count"))
    val level = DashLevel(
        Json.str(l, "name"), Json.dbl(l, "recharge"),
        Json.str(l, "next_name"), Json.dbl(l, "next_recharge")
    )
    val bizObj = d.optJSONObject("business")
    val business: DashBusiness? = if (bizObj != null) {
        DashBusiness(
            Json.dbl(bizObj, "today_income"), Json.dbl(bizObj, "yesterday_income"),
            Json.dbl(bizObj, "week_income"), Json.dbl(bizObj, "month_income"),
            Json.dbl(bizObj, "trade_total"), Json.int(bizObj, "today_orders"),
            Json.int(bizObj, "pending_delivery"), Json.int(bizObj, "card_unsold"),
            Json.int(bizObj, "commodity_online"), Json.int(bizObj, "commodity_count")
        )
    } else null
    val isBiz = Json.int(d, "is_business") == 1 || (d.has("is_business") && d.optBoolean("is_business", false))
    return DashOverview(Json.str(d, "username"), Json.str(d, "avatar"), asset, consume, level, isBiz, business)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<DashOverview?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                data = withContext(Dispatchers.IO) { fetchOverview() }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的主页") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
            val d = data ?: return@Column

            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(d.username.ifBlank { "用户" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (d.level.name.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("等级：${d.level.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            SectionHeader("资产数据")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("余额", "¥${fmt(d.asset.balance)}", Modifier.weight(1f))
                StatBox("硬币", "¥${fmt(d.asset.coin)}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("元气（总充值）", fmt(d.asset.recharge), Modifier.weight(1f))
                StatBox("总硬币收入", "¥${fmt(d.asset.totalCoin)}", Modifier.weight(1f))
            }

            if (d.level.nextName.isNotBlank() && d.level.nextRecharge > 0) {
                SectionHeader("会员成长")
                val cur = d.asset.recharge
                val next = d.level.nextRecharge
                val progress = if (next > 0) (cur / next).coerceIn(0.0, 1.0).toFloat() else 0f
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("当前：${d.level.name}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        Text("距 ${d.level.nextName} 还需 ${fmt((next - cur).coerceAtLeast(0.0))} 元气", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            SectionHeader("消费数据")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("本月购物", "¥${fmt(d.consume.buyMonth)}", Modifier.weight(1f))
                StatBox("累计购物", "¥${fmt(d.consume.buyTotal)}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("累计订单", "${d.consume.buyCount} 单", Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }

            val b = d.business
            if (b != null) {
                SectionHeader("经营数据")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("今日收入", "¥${fmt(b.todayIncome)}", Modifier.weight(1f))
                    StatBox("昨日收入", "¥${fmt(b.yesterdayIncome)}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("本月收入", "¥${fmt(b.monthIncome)}", Modifier.weight(1f))
                    StatBox("总交易", "¥${fmt(b.tradeTotal)}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("今日订单", "${b.todayOrders}", Modifier.weight(1f))
                    StatBox("待发货", "${b.pendingDelivery}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("未售卡密", "${b.cardUnsold}", Modifier.weight(1f))
                    StatBox("在售商品", "${b.commodityOnline}", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun StatBox(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
