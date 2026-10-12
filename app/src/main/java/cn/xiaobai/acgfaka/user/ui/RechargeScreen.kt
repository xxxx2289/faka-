package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.PayMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var pays by remember { mutableStateOf<List<PayMethod>>(emptyList()) }
    var payId by remember { mutableIntStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            pays = withContext(Dispatchers.IO) { runCatching { Api.rechargePayMethods() }.getOrDefault(emptyList()) }
            payId = pays.firstOrNull()?.id ?: 0
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("充值中心") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (loading) { CircularProgressIndicator(); return@Column }

            Text("充值金额", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("金额") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            // 快捷金额
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10", "50", "100", "200").forEach { v ->
                    OutlinedButton(onClick = { amount = v }) { Text("¥$v") }
                }
            }

            Text("支付方式", style = MaterialTheme.typography.titleMedium)
            if (pays.isEmpty()) Text("未配置充值支付方式", color = MaterialTheme.colorScheme.error)
            pays.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = payId == p.id, onClick = { payId = p.id })
                    RemoteImage(p.icon, modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text(p.name, Modifier.weight(1f))
                }
            }

            msg?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }

            Button(
                onClick = {
                    scope.launch {
                        busy = true; msg = null; isError = false
                        try {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt <= 0) throw Exception("请输入正确金额")
                            if (payId == 0) throw Exception("请选择支付方式")
                            val r = withContext(Dispatchers.IO) { Api.rechargeTrade(amt, payId) }
                            msg = "充值订单已创建：${r.tradeNo}" + (r.payUrl?.let { "\n支付链接：$it" } ?: "\n请在浏览器完成支付")
                            isError = false
                        } catch (e: Exception) { msg = e.message ?: "充值失败"; isError = true } finally { busy = false }
                    }
                },
                enabled = !busy && amount.isNotBlank() && payId != 0,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (busy) "处理中…" else "立即充值") }

            Text("提示：充值订单创建后，在浏览器打开支付链接或按站点方式完成支付。", style = MaterialTheme.typography.labelSmall)
        }
    }
}
