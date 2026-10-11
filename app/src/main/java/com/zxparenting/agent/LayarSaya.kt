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
import com.zxparenting.agent.tema.*
import com.zxparenting.agent.tema.WarnaAktif

@Composable
fun LayarSaya(nama: String, umur: Int, onKeluar: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bahasaAktif = ManajerBahasa.bahasaAktif(ctx)
    var showLangDialog by remember { mutableStateOf(false) }
    var showTemaDialog by remember { mutableStateOf(false) }
    val temaAktif = WarnaAktif.keyAktif(ctx)
    var statusUpdate by remember { mutableStateOf<String?>(null) }
    var sedangCek by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
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
                        .background(Biru.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Biru, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(nama, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Umur $umur tahun", fontSize = 12.sp, color = MutedFg)
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
                Icon(Icons.Default.Shield, contentDescription = null, tint = Hijau, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.parental_control), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.device_admin_active), fontSize = 11.sp, color = MutedFg)
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

        // Theme switcher
        OutlinedButton(
            onClick = { showTemaDialog = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            val namaTema = when (temaAktif) {
                "gelap" -> stringResource(R.string.tema_malam)
                "laut" -> stringResource(R.string.tema_laut)
                else -> stringResource(R.string.tema_default)
            }
            Text("${stringResource(R.string.tema)}: $namaTema")
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

        // Cek Pembaruan card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Biru,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.cek_pembaruan),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (sedangCek && statusUpdate == null) stringResource(R.string.update_checking)
                        else if (statusUpdate != null) "${stringResource(R.string.update_available)} $statusUpdate"
                        else stringResource(R.string.up_to_date),
                        fontSize = 11.sp,
                        color = if (statusUpdate != null) Amber else MutedFg,
                    )
                }
                TextButton(
                    onClick = {
                        sedangCek = true
                        statusUpdate = null
                        PemeriksaPembaruan.periksa(ctx) { tag ->
                            sedangCek = false
                            statusUpdate = tag
                        }
                    },
                    enabled = !sedangCek,
                ) {
                    Text(stringResource(R.string.check))
                }
            }
        }

        Text(
            "Versi ${BuildConfig.VERSI_NAMA} (${BuildConfig.VERSI_KODE})",
            fontSize = 11.sp,
            color = MutedFg,
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
                                Icon(Icons.Default.Check, contentDescription = null, tint = Biru)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLangDialog = false }) { Text(stringResource(R.string.done)) } },
        )
    }

    if (showTemaDialog) {
        AlertDialog(
            onDismissRequest = { showTemaDialog = false },
            title = { Text(stringResource(R.string.tema)) },
            text = {
                Column {
                    listOf(
                        "default" to stringResource(R.string.tema_default),
                        "gelap" to stringResource(R.string.tema_malam),
                        "laut" to stringResource(R.string.tema_laut),
                    ).forEach { (key, label) ->
                        TextButton(
                            onClick = {
                                WarnaAktif.gantiByKey(ctx, key)
                                showTemaDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            if (temaAktif == key) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Biru)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showTemaDialog = false }) { Text(stringResource(R.string.done)) } },
        )
    }
}
