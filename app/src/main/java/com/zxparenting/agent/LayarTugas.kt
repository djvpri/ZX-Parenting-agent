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
import androidx.compose.ui.res.stringResource

@Composable
fun LayarTugas(jwt: String, onKlikTugas: (TugasAnak) -> Unit = {}) {
    var items by remember { mutableStateOf<List<TugasAnak>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val strFailLoad = stringResource(R.string.fail_load_tasks)
    LaunchedEffect(jwt) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF6FF)),
    ) {
        Text(
            stringResource(R.string.task_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_tasks), color = Color(0xFF94A3B8))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items) { t -> KartuTugas(t, onKlikTugas) }
            }
        }
    }
}

@Composable
private fun KartuTugas(t: TugasAnak, onKlik: (TugasAnak) -> Unit) {
    val isAiKuis = t.type == "ai_quiz" || t.type == "ai_scheduled"
    val bisaDikerjakan = isAiKuis && t.status != "SELESAI"
    val warnaStatus = when (t.status) {
        "SELESAI" -> Color(0xFFDCFCE7) to Color(0xFF166534)
        "DIVERIFIKASI" -> Color(0xFFDBEAFE) to Color(0xFF1E40AF)
        else -> Color(0xFFFEF3C7) to Color(0xFF92400E)
    }
    val labelStatus = when (t.status) {
        "SELESAI" -> stringResource(R.string.done)
        "DIVERIFIKASI" -> stringResource(R.string.verified)
        else -> stringResource(R.string.not_done)
    }

    Card(
        modifier = Modifier.fillMaxWidth().then(
            if (bisaDikerjakan) Modifier.clip(RoundedCornerShape(16.dp)) else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = { if (bisaDikerjakan) onKlik(t) },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Task, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (!t.deskripsi.isNullOrBlank()) {
                        Text(t.deskripsi, fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
                BadgePill("+${t.reward} ZX", Color(0xFFDCFCE7), Color(0xFF166534))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgePill(labelStatus, warnaStatus.first, warnaStatus.second)
                    if (isAiKuis) {
                        Spacer(Modifier.width(6.dp))
                        BadgePill(stringResource(R.string.ai_badge), Color(0xFFEDE9FE), Color(0xFF6D28D9))
                    }
                }
                if (bisaDikerjakan) {
                    Text("Kerjakan →", fontSize = 11.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Medium)
                } else if (t.deadline != null) {
                    Text("Deadline: ${t.deadline.take(10)}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}
