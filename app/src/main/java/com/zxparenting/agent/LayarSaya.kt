package com.zxparenting.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zxparenting.agent.BuildConfig
import androidx.compose.ui.res.stringResource

@Composable
fun LayarSaya(nama: String, umur: Int, onKeluar: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bahasaAktif = ManajerBahasa.bahasaAktif(ctx)
    var showLangDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.nav_profile), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(nama, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Umur $umur tahun", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        // Status device admin
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.parental_control), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.device_admin_active), fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Language switcher
        OutlinedButton(
            onClick = { showLangDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("${stringResource(R.string.language)}: ${bahasaAktif ?: stringResource(R.string.lang_system)}")
        }

        OutlinedButton(
            onClick = onKeluar,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.logout))
        }

        Text(
            "Versi ${BuildConfig.VERSI_NAMA} (${BuildConfig.VERSI_KODE})",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    listOf(
                        null to stringResource(R.string.lang_system),
                        "en" to stringResource(R.string.lang_english),
                        "in" to stringResource(R.string.lang_indonesian),
                    ).forEach { (code, label) ->
                        TextButton(
                            onClick = {
                                ManajerBahasa.setBahasa(ctx, code)
                                showLangDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            if (bahasaAktif == code) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF3B82F6))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLangDialog = false }) { Text(stringResource(R.string.done)) } },
        )
    }
}
