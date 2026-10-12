package cn.xiaobai.acgfaka.user.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import cn.xiaobai.acgfaka.user.core.Http
import cn.xiaobai.acgfaka.user.core.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var captchaBytes by remember { mutableStateOf<ByteArray?>(null) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var needTotp by remember { mutableStateOf(false) }

    fun loadCaptcha(register: Boolean) {
        scope.launch {
            try {
                captchaBytes = withContext(Dispatchers.IO) {
                    if (register) Api.registerCaptcha() else Api.loginCaptcha()
                }
            } catch (e: Exception) { msg = "验证码加载失败：${e.message}"; isError = true }
        }
    }

    // 切换登录/注册时刷新对应验证码
    LaunchedEffect(isRegister) {
        captcha = ""
        loadCaptcha(isRegister)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRegister) "注册" else "登录") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 登录/注册切换
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isRegister = false },
                    enabled = isRegister,
                    modifier = Modifier.weight(1f)
                ) { Text("登录") }
                OutlinedButton(
                    onClick = { isRegister = true },
                    enabled = !isRegister,
                    modifier = Modifier.weight(1f)
                ) { Text("注册") }
            }

            OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("用户名") }, singleLine = true)
            if (isRegister) {
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("邮箱（如站点要求）") }, singleLine = true)
            }
            OutlinedTextField(
                password, { password = it }, Modifier.fillMaxWidth(),
                label = { Text("密码") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedTextField(captcha, { captcha = it }, Modifier.fillMaxWidth(), label = { Text("图形验证码") }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val bmp = remember(captchaBytes) { captchaBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
                if (bmp != null) Image(bmp, "验证码", Modifier.height(48.dp).width(140.dp))
                else Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.height(48.dp).width(140.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("点击刷新", style = MaterialTheme.typography.labelSmall) }
                }
                OutlinedButton(onClick = { loadCaptcha(isRegister) }, enabled = !busy) { Text("换一张") }
            }
            if (!isRegister && needTotp) {
                OutlinedTextField(totp, { totp = it }, Modifier.fillMaxWidth(), label = { Text("谷歌验证码") }, singleLine = true)
            }
            msg?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }

            Button(
                onClick = {
                    scope.launch {
                        busy = true; msg = null; isError = false
                        try {
                            if (Http.baseUrl.isBlank()) throw Exception("请先在「服务器设置」配置地址")
                            if (isRegister) {
                                val m = withContext(Dispatchers.IO) { Api.register(username, password, captcha, email) }
                                msg = m; isError = false
                                // 注册成功切回登录
                                isRegister = false
                                captcha = ""
                            } else {
                                val m = withContext(Dispatchers.IO) { Api.login(username, password, captcha, totp) }
                                Session.setLoggedIn(ctx, true)
                                Session.saveUsername(ctx, username)
                                msg = m; isError = false
                                kotlinx.coroutines.delay(400)
                                nav.popBackStack()
                            }
                        } catch (e: Exception) {
                            msg = e.message ?: (if (isRegister) "注册失败" else "登录失败")
                            isError = true
                            if (!isRegister && (e.message?.contains("谷歌") == true || e.message?.contains("两步") == true)) needTotp = true
                            captcha = ""
                            loadCaptcha(isRegister)
                        } finally { busy = false }
                    }
                },
                enabled = !busy && username.isNotBlank() && password.isNotBlank() && captcha.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (busy) "处理中…" else if (isRegister) "注册" else "登录") }

            Text(
                if (isRegister) "注册使用 ACG-Faka 原有注册接口，验证码 action=register。"
                else "登录使用 ACG-Faka 原有认证接口，验证码 action=login。",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
