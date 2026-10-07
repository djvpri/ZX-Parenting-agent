package com.zxparenting.agent

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

@Composable
fun LayarBeranda(jwt: String) {
    var items by remember { mutableStateOf<List<OfficialItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(jwt) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching { Klien.api.feedOfficial("Bearer $jwt") }.getOrNull()
            }
            loading = false
            if (res?.isSuccessful == true) {
                items = res.body()?.items ?: emptyList()
            } else {
                error = "Gagal muat konten"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Beranda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            BadgePill("ZX Official", Color(0xFFDCFCE7), Color(0xFF166534))
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
                Text("Belum ada konten.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items) { item -> KartuOfficial(item) }
            }
        }
    }
}

@Composable
private fun KartuOfficial(item: OfficialItem) {
    val meta = item.meta
    val tipeLabel = when (item.tipe) {
        "KUIS_HARIAN" -> "Kuis Harian"
        "TANTANGAN_MINGGUAN" -> "Tantangan"
        "ARTIKEL" -> "Artikel"
        "VIDEO_PENDEK" -> "Video"
        else -> item.tipe
    }
    val warnaTipe = when (item.tipe) {
        "KUIS_HARIAN" -> Color(0xFF3B82F6)
        "TANTANGAN_MINGGUAN" -> Color(0xFFF59E0B)
        "ARTIKEL" -> Color(0xFF8B5CF6)
        else -> Color(0xFF10B981)
    }

    Card(
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
                    Text(tipeLabel, fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
                if (meta?.premium == true) {
                    BadgePill("PRO", Color(0xFFFEE2E2), Color(0xFF991B1B))
                }
            }
            if (!item.deskripsi.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(item.deskripsi, fontSize = 12.sp, color = Color(0xFF475569))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (meta?.kategori != null) {
                        BadgePill(meta.kategori, Color(0xFFF1F5F9), Color(0xFF64748B))
                    }
                    if (meta != null) {
                        BadgePill("+${meta.tokenReward} ZX", Color(0xFFDCFCE7), Color(0xFF166534))
                    }
                }
                Text("${meta?.views ?: 0}x dilihat", fontSize = 10.sp, color = Color(0xFF94A3B8))
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
