package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var type by remember { mutableIntStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("硬币兑现") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("兑现方式", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = type == 0, onClick = { type = 0 }, label = { Text("支付宝") })
                FilterChip(selected = type == 1, onClick = { type = 1 }, label = { Text("微信") })
            }
            Text("兑现金额", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("金额") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            msg?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            Button(
                onClick = {
                    scope.launch {
                        busy = true; msg = null; isError = false
                        try {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt <= 0) throw Exception("请输入正确金额")
                            val m = withContext(Dispatchers.IO) { Api.cashSubmit(type, amt) }
                            msg = m; isError = false; amount = ""
                        } catch (e: Exception) { msg = e.message ?: "提交失败"; isError = true } finally { busy = false }
                    }
                },
                enabled = !busy && amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (busy) "提交中…" else "提交兑现申请") }
            OutlinedButton(
                onClick = { nav.navigate("cashrecords") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("查看兑现记录") }
            Text("提示：兑现需先在个人资料绑定支付宝/微信，且满足站点最低兑现额。若开启了两步验证，提交前需验证。", style = MaterialTheme.typography.labelSmall)
        }
    }
}
