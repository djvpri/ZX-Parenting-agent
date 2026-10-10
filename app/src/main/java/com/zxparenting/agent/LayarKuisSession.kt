package com.zxparenting.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun LayarKuisSession(jwt: String, tugasId: String, onKembali: () -> Unit) {
    var fase by remember { mutableStateOf("loading") }
    var sessionId by remember { mutableStateOf("") }
    var soal by remember { mutableStateOf<List<SoalSession>>(emptyList()) }
    val jawaban = remember { mutableStateMapOf<Int, Int>() }
    var submitRes by remember { mutableStateOf<KuisSessionSubmitRes?>(null) }
    var reviewRes by remember { mutableStateOf<KuisSessionReviewRes?>(null) }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Start session on mount
    val strFailStart = stringResource(R.string.fail_start_quiz)
    LaunchedEffect(tugasId) {
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching { Klien.api.kuisSessionStart("Bearer $jwt", KuisSessionStartReq(tugasId)) }.getOrNull()
            }
            val r = res?.body()
            if (res?.isSuccessful == true && r != null) {
                sessionId = r.sessionId
                soal = r.soal
                jawaban.clear()
                fase = "soal"
            } else {
                error = strFailStart
                fase = "error"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFEFF6FF)),
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
            "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            "error" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            "soal" -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(soal) { i, s ->
                        KartuSoalLockable(
                            no = i + 1,
                            soal = s,
                            dipilih = jawaban[s.posisi],
                            onPilih = { jawaban[s.posisi] = it },
                        )
                    }
                    item {
                        val semuaTerjawab = soal.all { jawaban[it.posisi] != null }
                        val terkunci = jawaban.size
                        Text(
                            "$terkunci/${soal.size} soal terjawab",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                loading = true
                                scope.launch {
                                    val req = KuisSessionSubmitReq(
                                        sessionId = sessionId,
                                        jawaban = jawaban.entries.map { JawabanLocked(it.key, it.value) },
                                    )
                                    val res = withContext(Dispatchers.IO) {
                                        runCatching { Klien.api.kuisSessionSubmit("Bearer $jwt", req) }.getOrNull()
                                    }
                                    loading = false
                                    val r = res?.body()
                                    if (res?.isSuccessful == true && r != null) {
                                        submitRes = r
                                        fase = "hasil"
                                    } else {
                                        error = strFailSubmit
                                        fase = "error"
                                    }
                                }
                            },
                            enabled = semuaTerjawab && !loading,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.sending))
                            } else {
                                Text(stringResource(R.string.submit_answers))
                            }
                        }
                        if (!semuaTerjawab) {
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.answer_all_first), fontSize = 11.sp, color = Color(0xFFEF4444))
                        }
                    }
                }
            }
            "hasil" -> HasilKuisSession(
                hasil = submitRes,
                onReview = {
                    loading = true
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            runCatching { Klien.api.kuisSessionReview("Bearer $jwt", sessionId) }.getOrNull()
                        }
                        loading = false
                        val r = res?.body()
                        if (res?.isSuccessful == true && r != null) {
                            reviewRes = r
                            fase = "review"
                        } else {
                            error = strFailLoadExp
                            fase = "error"
                        }
                    }
                },
                onRetry = {
                    fase = "loading"
                    sessionId = ""
                    soal = emptyList()
                    jawaban.clear()
                    submitRes = null
                    reviewRes = null
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            runCatching { Klien.api.kuisSessionStart("Bearer $jwt", KuisSessionStartReq(tugasId)) }.getOrNull()
                        }
                        val r = res?.body()
                        if (res?.isSuccessful == true && r != null) {
                            sessionId = r.sessionId
                            soal = r.soal
                            jawaban.clear()
                            fase = "soal"
                        } else {
                            error = strFailRetry
                            fase = "error"
                        }
                    }
                },
                onSelesai = onKembali,
                loading = loading,
            )
            "review" -> ReviewKuisSession(reviewRes, onKembali, onRetry = {
                fase = "loading"
                sessionId = ""
                soal = emptyList()
                jawaban.clear()
                submitRes = null
                reviewRes = null
                scope.launch {
                    val res = withContext(Dispatchers.IO) {
                        runCatching { Klien.api.kuisSessionStart("Bearer $jwt", KuisSessionStartReq(tugasId)) }.getOrNull()
                    }
                    val r = res?.body()
                    if (res?.isSuccessful == true && r != null) {
                        sessionId = r.sessionId
                        soal = r.soal
                        jawaban.clear()
                        fase = "soal"
                    } else {
                        error = strFailRetry
                        fase = "error"
                    }
                }
            })
        }
    }
}

@Composable
private fun KartuSoalLockable(no: Int, soal: SoalSession, dipilih: Int?, onPilih: (Int) -> Unit) {
    val terkunci = dipilih != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$no.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                Spacer(Modifier.width(6.dp))
                Text(soal.question, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            soal.options.forEachIndexed { idx, opt ->
                val terpilih = dipilih == idx
                Surface(
                    onClick = { if (!terkunci) onPilih(idx) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        terpilih -> Color(0xFF3B82F6)
                        terkunci -> Color(0xFFF1F5F9)
                        else -> Color(0xFFF8FAFC)
                    },
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        terpilih -> Color.White
                                        terkunci -> Color(0xFFCBD5E1)
                                        else -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                ('A' + idx).toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    terpilih -> Color.White
                                    terkunci -> Color(0xFF94A3B8)
                                    else -> Color(0xFF3B82F6)
                                },
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            opt,
                            fontSize = 13.sp,
                            color = when {
                                terpilih -> Color.White
                                terkunci -> Color(0xFF94A3B8)
                                else -> Color(0xFF1E293B)
                            },
                        )
                        if (terkunci && !terpilih) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            if (terkunci) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.answers_locked), fontSize = 10.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}

@Composable
private fun HasilKuisSession(
    hasil: KuisSessionSubmitRes?,
    onReview: () -> Unit,
    onRetry: () -> Unit,
    onSelesai: () -> Unit,
    loading: Boolean,
) {
    val skor = hasil?.skor ?: 0
    val benar = hasil?.benarCount ?: 0
    val total = hasil?.totalSoal ?: 0
    val token = hasil?.token ?: 0
    val persen = if (total > 0) (benar * 100 / total) else 0
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(36.dp)).background(warna.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("$persen", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = warna) }
                    Spacer(Modifier.height(12.dp))
                    Text(emoji, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("$benar dari $total benar", fontSize = 14.sp, color = Color(0xFF64748B))
                    Spacer(Modifier.height(16.dp))
                    BadgePill("+$token ZX", Color(0xFFDCFCE7), Color(0xFF166534))
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onReview,
                            enabled = !loading,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.explanation))
                        }
                        Button(
                            onClick = onRetry,
                            enabled = !loading,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.retry))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onSelesai) { Text(stringResource(R.string.done)) }
                }
            }
        }
    }
}

@Composable
private fun ReviewKuisSession(
    review: KuisSessionReviewRes?,
    onKembali: () -> Unit,
    onRetry: () -> Unit,
) {
    val strFailSubmit = stringResource(R.string.fail_submit)
    val strFailLoadExp = stringResource(R.string.fail_load_explanation)
    val strFailRetry = stringResource(R.string.fail_retry_quiz)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.explanation), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (review != null) {
                    val persen = if ((review.totalSoal ?: 0) > 0) ((review.benarCount ?: 0) * 100 / review.totalSoal!!) else 0
                    BadgePill("$persen%", Color(0xFFDBEAFE), Color(0xFF1E40AF))
                }
            }
        }
        itemsIndexed(review?.review ?: emptyList()) { i, r ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (r.benar) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                ),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (r.benar) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (r.benar) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Soal ${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(r.question, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    r.options.forEachIndexed { idx, opt ->
                        val benarIdx = idx == r.jawabanBenar
                        val anakIdx = r.jawabanAnak != null && idx == r.jawabanAnak
                        val bg = when {
                            benarIdx -> Color(0xFFDCFCE7)
                            anakIdx -> Color(0xFFFEE2E2)
                            else -> Color.Transparent
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${('A' + idx)}.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (benarIdx) Color(0xFF166534) else if (anakIdx) Color(0xFF991B1B) else Color(0xFF64748B))
                            Spacer(Modifier.width(6.dp))
                            Text(opt, fontSize = 12.sp, color = if (benarIdx) Color(0xFF166534) else if (anakIdx) Color(0xFF991B1B) else Color(0xFF1E293B))
                            if (benarIdx) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (r.explanation.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Divider(color = Color(0xFFE2E8F0))
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(r.explanation, fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.try_again))
                }
                OutlinedButton(
                    onClick = onKembali,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) { Text(stringResource(R.string.done)) }
            }
        }
    }
}
