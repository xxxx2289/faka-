package cn.xiaobai.acgfaka.user.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.user.core.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var oldPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改密码") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(oldPwd, { oldPwd = it }, Modifier.fillMaxWidth(), label = { Text("当前密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(newPwd, { newPwd = it }, Modifier.fillMaxWidth(), label = { Text("新密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(confirmPwd, { confirmPwd = it }, Modifier.fillMaxWidth(), label = { Text("确认新密码") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("谷歌验证码（如已开启两步验证）") }, singleLine = true)
            msg?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            Button(
                onClick = {
                    scope.launch {
                        busy = true; msg = null; isError = false
                        try {
                            if (newPwd != confirmPwd) throw Exception("两次输入的新密码不一致")
                            if (newPwd.length < 6) throw Exception("新密码至少6位")
                            val m = withContext(Dispatchers.IO) { Api.changePassword(oldPwd, newPwd, code) }
                            msg = m; isError = false
                            oldPwd = ""; newPwd = ""; confirmPwd = ""; code = ""
                        } catch (e: Exception) { msg = e.message ?: "修改失败"; isError = true } finally { busy = false }
                    }
                },
                enabled = !busy && oldPwd.isNotBlank() && newPwd.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (busy) "提交中…" else "确认修改") }
        }
    }
}
