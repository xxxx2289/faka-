package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.HtmlText
import cn.xiaobai.acgfaka.user.core.Json
import org.json.JSONObject

/** 购买记录 */
@Composable
fun PurchaseScreen(nav: NavController) {
    SimpleListScreen(nav, "购买记录", { p, l -> Api.purchaseRecords(p, l) }, emptyText = "暂无购买记录") { o ->
        val commodity = o.optJSONObject("commodity")
        val name = commodity?.let { Json.str(it, "name") } ?: "商品"
        val cover = commodity?.let { Json.str(it, "cover") }
        val amount = Json.dbl(o, "amount")
        val status = Json.int(o, "status")
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp)) {
                RemoteImage(cover, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text("订单号：${Json.str(o, "trade_no")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        Text("¥${fmt(amount)}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Text(if (status == 1) "已支付" else "待支付", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(Json.str(o, "create_time"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 账单 */
@Composable
fun BillsScreen(nav: NavController) {
    SimpleListScreen(nav, "我的账单", { p, l -> Api.bills(p, l) }, emptyText = "暂无账单") { o ->
        val amount = Json.dbl(o, "amount")
        val type = Json.str(o, "type", Json.str(o, "remark"))
        val time = Json.str(o, "create_time")
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(type.ifBlank { "交易" }, style = MaterialTheme.typography.bodyMedium)
                    Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    (if (amount >= 0) "+" else "") + fmt(amount),
                    color = if (amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/** 优惠券 */
@Composable
fun CouponsScreen(nav: NavController) {
    SimpleListScreen(nav, "我的优惠券", { p, l -> Api.coupons(p, l) }, emptyText = "暂无优惠券") { o ->
        val name = Json.str(o, "name", "优惠券")
        val code = Json.str(o, "code")
        val amount = Json.dbl(o, "amount")
        val status = Json.int(o, "status")
        val expire = Json.str(o, "expire_time")
        InfoCard(
            title = name,
            badge = if (status == 0) "可用" else "已用",
            rows = buildList {
                if (code.isNotBlank()) add("券码" to code)
                if (amount > 0) add("面额" to "¥${fmt(amount)}")
                if (expire.isNotBlank()) add("有效期" to expire)
            }
        )
    }
}

/** 提现记录 */
@Composable
fun CashRecordsScreen(nav: NavController) {
    SimpleListScreen(nav, "兑现记录", { p, l -> Api.cashRecords(p, l) }, emptyText = "暂无兑现记录") { o ->
        val amount = Json.dbl(o, "amount")
        val status = Json.int(o, "status")
        val type = Json.int(o, "type")
        val time = Json.str(o, "create_time")
        InfoCard(
            title = "¥${fmt(amount)}",
            badge = when (status) { 0 -> "待处理"; 1 -> "已完成"; 2 -> "已拒绝"; else -> "状态$status" },
            rows = listOf(
                "方式" to if (type == 0) "支付宝" else if (type == 1) "微信" else "其他",
                "时间" to time
            )
        )
    }
}

/** 我的下级 */
@Composable
fun AgentMembersScreen(nav: NavController) {
    SimpleListScreen(nav, "我的下级", { p, l -> Api.agentMembers(p, l) }, emptyText = "暂无下级") { o ->
        val name = Json.str(o, "username", "用户")
        val id = Json.int(o, "id")
        val balance = Json.dbl(o, "balance")
        val recharge = Json.dbl(o, "recharge")
        val time = Json.str(o, "create_time")
        InfoCard(
            title = name,
            badge = "ID $id",
            rows = listOf(
                "余额" to "¥${fmt(balance)}",
                "累计充值" to "¥${fmt(recharge)}",
                "注册时间" to time
            )
        )
    }
}

/** 消息中心 */
@Composable
fun MessagesScreen(nav: NavController) {
    SimpleListScreen(nav, "消息中心", { p, l -> Api.messages(p, l) }, emptyText = "暂无消息") { o ->
        val title = Json.str(o, "title", "消息")
        val content = HtmlText.oneLine(Json.str(o, "content"))
        val time = Json.str(o, "create_time")
        val read = Json.int(o, "status")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                    if (read == 0) Badge { Text("未读") }
                }
                if (content.isNotBlank()) Text(content, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
