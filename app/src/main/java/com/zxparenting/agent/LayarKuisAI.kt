package com.zxparenting.agent

import com.zxparenting.agent.tema.*

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource

@Composable
fun LayarKuisAI(jwt: String, onKembali: () -> Unit) {
    val strTopicReq = stringResource(R.string.topic_required)
    val strFailCreate = stringResource(R.string.fail_create_quiz)
    val strFailSubmit = stringResource(R.string.fail_submit)
    var topik by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(3) }
    var fase by remember { mutableStateOf("input") } // input | soal | hasil
    var soal by remember { mutableStateOf<List<Soal>>(emptyList()) }
    var jawaban by remember { mutableStateOf<MutableList<Int>>(mutableListOf()) }
    var attemptId by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var hasil by remember { mutableStateOf<SubmitKuisRes?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKembali) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text(stringResource(R.string.quiz_ai), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        when (fase) {
            "input" -> FormKuisAI(topik, level, loading, error,
                onTopik = { topik = it },
                onLevel = { level = it },
                onMulai = {
                    if (topik.isBlank()) { error = strTopicReq; return@FormKuisAI }
                    loading = true; error = ""
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            runCatching {
                                Klien.api.kuisStart("Bearer $jwt", KuisAIReq(topik.trim(), level, 5))
                            }.getOrNull()
                        }
                        loading = false
                        val r = res?.body()
                        if (res?.isSuccessful == true && r != null) {
                            soal = r.soal
                            jawaban = MutableList(r.soal.size) { -1 }
                            attemptId = r.attemptId
                            fase = "soal"
                        } else {
                            error = strFailCreate
                        }
                    }
                },
            )
            "soal" -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(soal) { i, s ->
                        KartuSoalAI(i + 1, s, jawaban[i]) { jawaban[i] = it }
                    }
                    item {
                        val semua = jawaban.all { it >= 0 }
                        Button(
                            onClick = {
                                scope.launch {
                                    val res = withContext(Dispatchers.IO) {
                                        runCatching {
                                            Klien.api.kuisSubmit("Bearer $jwt", attemptId, SubmitKuisReq(jawaban.toList()))
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
                            enabled = semua,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(stringResource(R.string.submit_answers)) }
                    }
                }
            }
            "hasil" -> HasilKuisAI(hasil, soal, jawaban, onKembali)
        }
    }
}

@Composable
private fun FormKuisAI(
    topik: String,
    level: Int,
    loading: Boolean,
    error: String,
    onTopik: (String) -> Unit,
    onLevel: (Int) -> Unit,
    onMulai: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.create_own_quiz), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.ai_quiz_desc), fontSize = 12.sp, color = MutedFg)
                OutlinedTextField(
                    value = topik,
                    onValueChange = onTopik,
                    label = { Text(stringResource(R.string.topic_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Text("Tingkat kesulitan: $level/5", fontSize = 12.sp, color = MutedFg)
                Slider(
                    value = level.toFloat(),
                    onValueChange = { onLevel(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                )
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Button(
                    onClick = onMulai,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.creating_quiz))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.start_quiz))
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuSoalAI(no: Int, soal: Soal, dipilih: Int, onPilih: (Int) -> Unit) {
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
                    color = if (terpilih) Biru else Kartu,
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (terpilih) Color.White else Biru.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(('A' + idx).toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Biru) }
                        Spacer(Modifier.width(10.dp))
                        Text(opt, fontSize = 13.sp, color = if (terpilih) Color.White else Teks)
                    }
                }
            }
        }
    }
}

@Composable
private fun HasilKuisAI(hasil: SubmitKuisRes?, soal: List<Soal>, jawaban: List<Int>, onKembali: () -> Unit) {
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
        persen >= 70 -> Hijau
        persen >= 50 -> Amber
        else -> Merah
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ringkasan
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(warna.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("$persen", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = warna) }
                    Spacer(Modifier.height(8.dp))
                    Text(emoji, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("$skor/$total benar", fontSize = 13.sp, color = MutedFg)
                    Spacer(Modifier.height(12.dp))
                    BadgePill("+$token ZX", Hijau.copy(alpha = .12f), Hijau)
                }
            }
        }
        // Detail per soal
        itemsIndexed(hasil?.hasil ?: emptyList()) { i, h ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (h.benar) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (h.benar) Hijau else Merah,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Soal ${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(h.q, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Jawabanmu: ${'A' + h.jawabAnak}", fontSize = 11.sp, color = if (h.benar) Hijau else Merah)
                    if (!h.benar) {
                        Text("Jawaban benar: ${'A' + h.jawabBenar}", fontSize = 11.sp, color = Hijau)
                    }
                    if (h.explain.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(h.explain, fontSize = 11.sp, color = MutedFg)
                    }
                }
            }
        }
        item {
            Button(
                onClick = onKembali,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.done)) }
        }
    }
}
