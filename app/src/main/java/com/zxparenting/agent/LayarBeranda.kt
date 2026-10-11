package com.zxparenting.agent

import com.zxparenting.agent.tema.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource

@Composable
fun LayarBeranda(jwt: String?, onBukaKuis: (String) -> Unit = {}) {
    var items by remember { mutableStateOf<List<OfficialItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val strFailLoad = stringResource(R.string.fail_load_content)
    LaunchedEffect(jwt) {
        if (jwt.isNullOrEmpty()) {
            // Demo mode — sample data
            items = DataDemo.officialItems
            loading = false
            return@LaunchedEffect
        }
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching { Klien.api.feedOfficial("Bearer $jwt") }.getOrNull()
            }
            loading = false
            if (res?.isSuccessful == true) {
                items = res.body()?.items ?: emptyList()
            } else {
                error = strFailLoad
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.home), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            BadgePill("ZX Official", Hijau.copy(alpha = .12f), Hijau)
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        } else if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_content), color = MutedFg)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items) { item ->
                    KartuOfficial(item, onClick = {
                        if (item.tipe == "KUIS_HARIAN" || item.tipe == "TANTANGAN_MINGGUAN") {
                            onBukaKuis(item.id)
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun KartuOfficial(item: OfficialItem, onClick: () -> Unit = {}) {
    val meta = item.meta
    val tipeLabel = when (item.tipe) {
        "KUIS_HARIAN" -> stringResource(R.string.daily_quiz)
        "TANTANGAN_MINGGUAN" -> stringResource(R.string.challenge)
        "ARTIKEL" -> stringResource(R.string.article)
        "VIDEO_PENDEK" -> stringResource(R.string.video)
        else -> item.tipe
    }
    val warnaTipe = when (item.tipe) {
        "KUIS_HARIAN" -> Biru
        "TANTANGAN_MINGGUAN" -> Amber
        "ARTIKEL" -> Ungu
        else -> Hijau
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(warnaTipe.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = when (item.tipe) {
                        "KUIS_HARIAN" -> Icons.Default.Quiz
                        "TANTANGAN_MINGGUAN" -> Icons.Default.EmojiEvents
                        "ARTIKEL" -> Icons.Default.Article
                        else -> Icons.Default.PlayCircle
                    }
                    Icon(icon, contentDescription = null, tint = warnaTipe, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(tipeLabel, fontSize = 10.sp, color = MutedFg)
                }
                if (meta?.premium == true) {
                    BadgePill(stringResource(R.string.pro_badge), Merah.copy(alpha = .12f), Merah)
                }
            }
            if (!item.deskripsi.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(item.deskripsi, fontSize = 12.sp, color = MutedFg)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (meta?.kategori != null) {
                        BadgePill(meta.kategori, Muted, MutedFg)
                    }
                    if (meta != null) {
                        BadgePill("+${meta.tokenReward} ZX", Hijau.copy(alpha = .12f), Hijau)
                    }
                }
                Text("${meta?.views ?: 0}x dilihat", fontSize = 10.sp, color = MutedFg)
            }
        }
    }
}

@Composable
fun BadgePill(teks: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(teks, fontSize = 9.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}
