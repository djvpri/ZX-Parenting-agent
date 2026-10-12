package com.zxparenting.agent

import com.zxparenting.agent.tema.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource

@Composable
fun LayarMasuk(onMasuk: () -> Unit, onDemo: () -> Unit) {
    val strReq = stringResource(R.string.username_pin_required)
    val strWrong = stringResource(R.string.username_pin_wrong)
    val strAnak = stringResource(R.string.nav_child)
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ZX Parenting", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Teks)
        Text(stringResource(R.string.login_as_child), fontSize = 14.sp, color = MutedFg)
        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text(stringResource(R.string.pin)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        if (username.isBlank() || pin.isBlank()) {
                            error = strReq
                            return@Button
                        }
                        loading = true
                        error = ""
                        scope.launch {
                            val res = withContext(Dispatchers.IO) {
                                runCatching { Klien.api.loginAnak(LoginAnakReq(username.trim(), pin)) }.getOrNull()
                            }
                            loading = false
                            val body = res?.body()
                            if (res?.isSuccessful == true && body?.token != null) {
                                val a = body.anak
                                Simpanan.simpanAnak(
                                    Ctx.get(),
                                    body.token,
                                    a?.id ?: "",
                                    a?.nama ?: strAnak,
                                    a?.umur ?: 0,
                                    username.trim(),
                                )
                                onMasuk()
                            } else {
                                error = strWrong
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.login))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.demo_mode_desc),
            fontSize = 12.sp,
            color = MutedFg,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        OutlinedButton(
            onClick = onDemo,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.demo_mode))
        }
    }
}
