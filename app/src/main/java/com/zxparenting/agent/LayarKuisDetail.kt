package com.zxparenting.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
fun LayarKuisDetail(jwt: String, kuisId: String, onKembali: () -> Unit) {
    val strFailLoad = stringResource(R.string.fail_load_quiz)
    val strFailSubmit = stringResource(R.string.fail_submit)
    val strQuizAi = stringResource(R.string.quiz_ai)
    var detail by remember { mutableStateOf<OfficialDetail?>(null) }
    var jawaban by remember { mutableStateOf<MutableList<Int>>(mutableListOf()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var fase by remember { mutableStateOf("soal") } // soal | hasil | selesai
    var hasil by remember { mutableStateOf<CompleteRes?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(kuisId) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching { Klien.api.officialDetail("Bearer $jwt", kuisId) }.getOrNull()
            }
            loading = false
            val d = res?.body()
            if (res?.isSuccessful == true && d != null) {
                detail = d
                jawaban = MutableList(d.konten?.size ?: 0) { -1 }
                if (d.selesai) fase = "selesai"
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKembali) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                detail?.judul ?: strQuizAi,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (detail != null) {
                BadgePill("+${detail!!.tokenReward} ZX", Color(0xFFDCFCE7), Color(0xFF166534))
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error.isNotEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            fase == "selesai" -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.already_done), fontWeight = FontWeight.Bold)
                    }
                }
            }
            detail != null -> {
                val soal = detail!!.konten ?: emptyList()
                if (fase == "soal") {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(soal) { i, s ->
                            KartuSoal(
                                no = i + 1,
                                soal = s,
                                dipilih = jawaban[i],
                                onPilih = { jawaban[i] = it },
                            )
                        }
                        item {
                            val semuaTerjawab = jawaban.all { it >= 0 }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val res = withContext(Dispatchers.IO) {
                                            runCatching {
                                                Klien.api.officialComplete("Bearer $jwt", kuisId, CompleteReq(jawaban.toList()))
                                            }.getOrNull()
                                        }
                                        val r = res?.body()
                                        if (res?.isSuccessful == true && r != null) {
                                            hasil = r
                                            fase = "hasil"
                                        } else {
                                            error = strFailSubmit
                                        }
                                    }
                                },
                                enabled = semuaTerjawab,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(stringResource(R.string.submit_answers))
                            }
                        }
                    }
                } else if (fase == "hasil") {
                    HasilKuis(hasil, onKembali)
                }
            }
        }
    }
}

@Composable
private fun KartuSoal(no: Int, soal: Soal, dipilih: Int, onPilih: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$no. ${soal.q}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            soal.options.forEachIndexed { idx, opt ->
                val terpilih = dipilih == idx
                Surface(
                    onClick = { onPilih(idx) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (terpilih) Color(0xFF3B82F6) else Color(0xFFF8FAFC),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (terpilih) Color.White else Color(0xFF3B82F6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                ('A' + idx).toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (terpilih) Color(0xFF3B82F6) else Color(0xFF3B82F6),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(opt, fontSize = 13.sp, color = if (terpilih) Color.White else Color(0xFF1E293B))
                    }
                }
            }
        }
    }
}

@Composable
private fun HasilKuis(hasil: CompleteRes?, onKembali: () -> Unit) {
    val skor = hasil?.skor ?: 0
    val total = hasil?.totalSoal ?: 0
    val token = hasil?.tokenDidapat ?: 0
    val persen = if (total > 0) (skor * 100 / total) else 0
    val emoji = when {
        persen == 100 -> stringResource(R.string.perfect)
        persen >= 70 -> stringResource(R.string.great)
        persen >= 50 -> stringResource(R.string.okay)
        else -> stringResource(R.string.keep_practicing)
    }
    val warna = when {
        persen >= 70 -> Color(0xFF10B981)
        persen >= 50 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(warna.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$persen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = warna)
                }
                Spacer(Modifier.height(12.dp))
                Text(emoji, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("$skor dari $total benar", fontSize = 14.sp, color = Color(0xFF64748B))
                Spacer(Modifier.height(16.dp))
                BadgePill("+$token ZX", Color(0xFFDCFCE7), Color(0xFF166534))
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onKembali,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(stringResource(R.string.done)) }
            }
        }
    }
}
