package com.zxparenting.agent

import com.zxparenting.agent.tema.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource

@Composable
fun LayarTugas(jwt: String?, onKlikTugas: (TugasAnak) -> Unit = {}) {
    var items by remember { mutableStateOf<List<TugasAnak>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var selesaiExpanded by remember { mutableStateOf(false) }

    val strFailLoad = stringResource(R.string.fail_load_tasks)
    LaunchedEffect(jwt) {
        if (jwt.isNullOrEmpty()) {
            items = DataDemo.tugas
            loading = false
            return@LaunchedEffect
        }
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching { Klien.api.tugasList("Bearer $jwt") }.getOrNull()
            }
            loading = false
            if (res?.isSuccessful == true) {
                items = res.body() ?: emptyList()
            } else {
                error = strFailLoad
            }
        }
    }

    val aktif = items.filter { it.status == "PENDING" && (it.type == "ai_quiz" || it.type == "ai_scheduled") }
    val ditolak = items.filter { it.status == "DITOLAK" }
    val selesai = items.filter { it.status == "SELESAI" }
    val lainnya = items.filter { it !in aktif && it !in ditolak && it !in selesai }
    val total = items.size
    val persen = if (total > 0) (selesai.size * 100 / total) else 0

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        Text(
            stringResource(R.string.task_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp, 20.dp, 16.dp, 8.dp),
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Progress ring card
                item { KartuProgres(persen, aktif.size, selesai.size, ditolak.size) }

                // Aktif
                if (aktif.isNotEmpty()) {
                    item { LabelSeksi(Icons.Default.Bolt, stringResource(R.string.not_done), aktif.size, Biru) }
                    items(aktif) { t -> KartuTugasBaru(t, bisaKlik = true, onKlik = onKlikTugas) }
                }

                // Ditolak
                if (ditolak.isNotEmpty()) {
                    item { LabelSeksi(Icons.Default.Cancel, stringResource(R.string.rejected), ditolak.size, Merah) }
                    items(ditolak) { t -> KartuTugasBaru(t, bisaKlik = false, onKlik = onKlikTugas) }
                }

                // Lainnya (EXPIRED, DIKUMPULKAN, dll)
                if (lainnya.isNotEmpty()) {
                    item { LabelSeksi(Icons.Default.Schedule, stringResource(R.string.verified), lainnya.size, Amber) }
                    items(lainnya) { t -> KartuTugasBaru(t, bisaKlik = false, onKlik = onKlikTugas) }
                }

                // Selesai (collapsible)
                if (selesai.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selesaiExpanded = !selesaiExpanded }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Hijau, modifier = Modifier.size(16.dp))
                                Text(
                                    "${stringResource(R.string.done)} (${selesai.size})",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MutedFg,
                                )
                            }
                            Icon(
                                if (selesaiExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MutedFg,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = selesaiExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            selesai.forEach { t ->
                                KartuTugasBaru(t, bisaKlik = false, onKlik = onKlikTugas, redup = true)
                            }
                        }
                    }
                }

                // Empty state
                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Task,
                                    contentDescription = null,
                                    tint = MutedFg,
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Muted).padding(12.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.no_tasks), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MutedFg)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tugas baru dari ortu akan muncul di sini",
                                    fontSize = 12.sp, color = MutedFg.copy(alpha = .6f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuProgres(persen: Int, aktif: Int, selesai: Int, ditolak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Kartu),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Progress ring
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Muted))
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(
                        if (persen > 0)
                            Brush.sweepGradient(listOf(Biru, Ungu, Biru))
                        else
                            Brush.sweepGradient(listOf(Muted, Muted))
                    )
                )
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Kartu))
                Text("$persen%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Biru)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatItem(aktif, "Aktif", Biru)
                StatItem(selesai, "Selesai", Hijau)
                StatItem(ditolak, "Ditolak", Merah)
            }
        }
    }
}

@Composable
private fun StatItem(jumlah: Int, label: String, warna: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$jumlah", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = warna)
        Text(label, fontSize = 10.sp, color = MutedFg)
    }
}

@Composable
private fun LabelSeksi(ikon: ImageVector, teks: String, count: Int, warnaIkon: Color) {
    Row(
        modifier = Modifier.padding(14.dp, 12.dp, 16.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(ikon, contentDescription = null, tint = warnaIkon, modifier = Modifier.size(14.dp))
        Text(teks, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedFg, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Muted).padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MutedFg)
        }
    }
}

@Composable
private fun KartuTugasBaru(
    t: TugasAnak,
    bisaKlik: Boolean,
    onKlik: (TugasAnak) -> Unit,
    redup: Boolean = false,
) {
    val isAi = t.type == "ai_quiz" || t.type == "ai_scheduled"
    val warnaStatus = when (t.status) {
        "SELESAI" -> Hijau
        "DITOLAK" -> Merah
        "PENDING" -> Amber
        else -> Biru
    }
    val labelStatus = when (t.status) {
        "SELESAI" -> stringResource(R.string.done)
        "DITOLAK" -> stringResource(R.string.rejected)
        "PENDING" -> stringResource(R.string.not_done)
        else -> t.status
    }
    val opacity = if (redup) 0.5f else 1f

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Kartu),
        elevation = CardDefaults.cardElevation(defaultElevation = if (bisaKlik) 2.dp else 1.dp),
        onClick = { if (bisaKlik) onKlik(t) },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isAi) Pink.copy(alpha = .12f) else Biru.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isAi) Icons.Default.AutoAwesome else Icons.Default.Task,
                        contentDescription = null,
                        tint = if (isAi) Pink else Biru,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                // Title + desc
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teks.copy(alpha = opacity))
                    if (!t.deskripsi.isNullOrBlank()) {
                        Text(t.deskripsi, fontSize = 12.sp, color = MutedFg.copy(alpha = opacity))
                    }
                }
                // Reward badge
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Hijau.copy(alpha = .12f)).padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("+${t.reward}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Hijau)
                }
            }
            // Footer
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    BadgeStatus(labelStatus, warnaStatus)
                    if (isAi) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Pink.copy(alpha = .12f)).padding(horizontal = 9.dp, vertical = 3.dp),
                        ) {
                            Text("AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Pink)
                        }
                    }
                }
                if (bisaKlik) {
                    Text("Kerjakan \u2192", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Biru)
                }
            }
        }
    }
}

@Composable
private fun BadgeStatus(teks: String, warna: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(warna.copy(alpha = .12f)).padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(teks, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = warna)
    }
}
