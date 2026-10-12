package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.CommodityDetail
import cn.xiaobai.acgfaka.user.core.HtmlText
import cn.xiaobai.acgfaka.user.core.PayMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 下单「对局」阶段，模拟游戏对局的实时状态机 */
enum class TradePhase { IDLE, PAYING, DELIVERING, DONE, FAILED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(nav: NavController, id: Int) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<CommodityDetail?>(null) }
    var pays by remember { mutableStateOf<List<PayMethod>>(emptyList()) }
    var payId by remember { mutableIntStateOf(0) }
    var num by remember { mutableIntStateOf(1) }
    var price by remember { mutableDoubleStateOf(0.0) }
    var stock by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var phase by remember { mutableStateOf(TradePhase.IDLE) }
    var tradeNo by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf<String?>(null) }
    var failMsg by remember { mutableStateOf<String?>(null) }
    var showPaySheet by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        scope.launch {
            loading = true; error = null
            try {
                val d = withContext(Dispatchers.IO) { Api.commodityDetail(id) }
                detail = d
                stock = d.stock
                pays = withContext(Dispatchers.IO) { runCatching { Api.payMethods() }.getOrDefault(emptyList()) }
                payId = pays.firstOrNull()?.id ?: 0
                price = withContext(Dispatchers.IO) { runCatching { Api.valuation(id, 1) }.getOrDefault(d.price) }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(num, id) {
        if (!loading && detail != null) {
            runCatching { withContext(Dispatchers.IO) { Api.valuation(id, num) } }.onSuccess { price = it }
        }
    }

    LaunchedEffect(phase, tradeNo) {
        if (phase == TradePhase.DELIVERING && tradeNo.isNotBlank()) {
            repeat(40) {
                delay(3000)
                val ok = runCatching {
                    withContext(Dispatchers.IO) { Api.orderState(tradeNo) }
                }.getOrNull()
                if (ok != null && ok.status == 1) {
                    val s = runCatching { withContext(Dispatchers.IO) { Api.orderSecret(tradeNo) } }.getOrNull()
                    secret = s?.secret
                    phase = TradePhase.DONE
                    return@repeat
                }
            }
            if (phase == TradePhase.DELIVERING) { phase = TradePhase.FAILED; failMsg = "支付超时，请到订单页查询" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.name ?: "商品详情") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        },
        bottomBar = {
            if (detail != null && phase == TradePhase.IDLE) {
                Surface(tonalElevation = 3.dp) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("合计", style = MaterialTheme.typography.labelSmall)
                            Text("¥${fmt(price)}", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                        Button(
                            enabled = payId != 0 && detail!!.stockState != 2,
                            onClick = { showPaySheet = true },
                            modifier = Modifier.height(48.dp)
                        ) { Icon(Icons.Default.Payments, null); Spacer(Modifier.width(6.dp)); Text(if (detail!!.stockState == 2) "已售罄" else "立即购买") }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error!!, color = MaterialTheme.colorScheme.error) }
                detail == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("商品不存在") }
                else -> {
                    val d = detail!!
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        RemoteImage(d.cover, modifier = Modifier.fillMaxWidth().height(220.dp))
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(d.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("¥${fmt(d.price.coerceAtLeast(d.userPrice))}", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(10.dp))
                                AssistChip(onClick = {}, label = { Text("库存 ${stockLabel(d.stockState, stock)}") })
                                if (d.draftStatus == 1) { Spacer(Modifier.width(6.dp)); AssistChip(onClick = {}, label = { Text("支持预选") }) }
                            }
                            Divider()
                            Text("购买数量", fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { if (num > 1) num-- }, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("-") }
                                Text("$num", Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.titleMedium)
                                OutlinedButton(onClick = { if (num < 999) num++ }, modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
                            }
                            Divider()
                            Text("商品说明", fontWeight = FontWeight.SemiBold)
                            val cleanDesc = HtmlText.clean(d.description)
                            Text(
                                cleanDesc.ifBlank { "暂无说明" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (phase != TradePhase.IDLE) {
                TradeOverlay(
                    phase = phase, secret = secret, failMsg = failMsg, tradeNo = tradeNo,
                    onClose = { phase = TradePhase.IDLE; secret = null; failMsg = null },
                    onQueryOrder = { nav.navigate("orders") }
                )
            }
        }
    }

    if (showPaySheet && detail != null) {
        ModalBottomSheet(onDismissRequest = { showPaySheet = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择支付方式", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (pays.isEmpty()) Text("未配置支付方式", color = MaterialTheme.colorScheme.error)
                pays.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = payId == p.id, onClick = { payId = p.id })
                        RemoteImage(p.icon, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(10.dp))
                        Text(p.name, Modifier.weight(1f))
                    }
                }
                Button(
                    onClick = {
                        showPaySheet = false
                        scope.launch {
                            phase = TradePhase.PAYING
                            try {
                                val r = withContext(Dispatchers.IO) { Api.trade(detail!!.id, num, payId) }
                                tradeNo = r.tradeNo
                                phase = TradePhase.DELIVERING
                            } catch (e: Exception) {
                                phase = TradePhase.FAILED
                                failMsg = e.message ?: "下单失败"
                            }
                        }
                    },
                    enabled = payId != 0,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("确认支付 ¥${fmt(price)}") }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun TradeOverlay(
    phase: TradePhase, secret: String?, failMsg: String?, tradeNo: String,
    onClose: () -> Unit, onQueryOrder: () -> Unit
) {
    Surface(color = Color(0xE6000000), modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (phase) {
                TradePhase.PAYING -> {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text("正在创建订单…", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                TradePhase.DELIVERING -> {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassTop, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("对局进行中 · 等待支付", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("订单号：$tradeNo", color = Color(0xCCFFFFFF), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("支付完成后将自动发货", color = Color(0x99FFFFFF), style = MaterialTheme.typography.bodySmall)
                }
                TradePhase.DONE -> {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("发货成功", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Surface(color = Color(0xFF1B1B1B), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            secret ?: "卡密已发放，请到订单页查看",
                            color = Color.White, modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("完成") }
                    TextButton(onClick = onQueryOrder) { Text("查看订单", color = Color.White) }
                }
                TradePhase.FAILED -> {
                    Text("⚠", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFFFC107))
                    Spacer(Modifier.height(12.dp))
                    Text(failMsg ?: "下单失败", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("返回") }
                    TextButton(onClick = onQueryOrder) { Text("去订单页查询", color = Color.White) }
                }
                else -> {}
            }
        }
    }
}
