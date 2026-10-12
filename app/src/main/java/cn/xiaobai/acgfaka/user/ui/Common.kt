package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.xiaobai.acgfaka.user.core.Commodity
import cn.xiaobai.acgfaka.user.core.Http
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** 拼接图片完整地址，兼容相对路径 /favicon.ico */
fun fullImage(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http")) return path
    val base = Http.baseUrl.trimEnd('/')
    return if (path.startsWith("/")) base + path else "$base/$path"
}

@Composable
fun RemoteImage(url: String?, modifier: Modifier = Modifier, fallbackIcon: Boolean = true) {
    val full = fullImage(url)
    if (full == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(full).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

fun stockLabel(state: Int, stock: Int): String = when (state) {
    0 -> "充足"
    1 -> "紧张"
    2 -> "售罄"
    else -> "库存 $stock"
}

@Composable
fun CommodityCard(c: Commodity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp)) {
            RemoteImage(
                c.cover,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).heightIn(min = 96.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    if (c.tags.isNotEmpty()) {
                        Text(c.tags.joinToString("  "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¥${fmt(c.userPrice.coerceAtLeast(c.price))}", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    if (c.seckillActive) Badge { Text("秒杀") }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stockLabel(c.stockState, c.stock),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (c.stockState) { 2 -> Color.Gray; 1 -> Color(0xFFFB8C00); else -> Color(0xFF43A047) }
                    )
                }
            }
        }
    }
}

fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)
