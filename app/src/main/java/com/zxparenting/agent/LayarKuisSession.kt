package com.zxparenting.agent

import com.zxparenting.agent.tema.*

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
    val strFailSubmit = stringResource(R.string.fail_submit)
    val strFailLoadExp = stringResource(R.string.fail_load_explanation)
    val strFailRetry = stringResource(R.string.fail_retry_quiz)
    LaunchedEffect(tugasId) {
        if (jwt.isEmpty()) {
            // Demo mode (Play Console reviewer)
            val demoTugas = DataDemo.tugas.find { it.id == tugasId }
            val soalDemo = DataDemo.demoSoal[tugasId] ?: DataDemo.demoSoal["demo1"]!!
            soal = soalDemo.mapIndexed { i, s -> SoalSession(posisi = i, question = s.q, options = s.options) }
            sessionId = "demo-session"
            jawaban.clear()
            fase = "soal"
            return@LaunchedEffect
        }
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
        modifier = Modifier.fillMaxSize().background(Bg),
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
                            color = MutedFg,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                loading = true
                                if (jwt.isEmpty()) {
                                    val kunci = DataDemo.demoJawaban[tugasId] ?: DataDemo.demoJawaban["demo1"]!!
                                    val benarCount = soal.count { s -> jawaban[s.posisi] == kunci.getOrNull(s.posisi) }
                                    submitRes = KuisSessionSubmitRes(skor = benarCount, benarCount = benarCount, totalSoal = soal.size, token = benarCount * 3)
                                    loading = false
                                    fase = "hasil"
                                } else {
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
                            Text(stringResource(R.string.answer_all_first), fontSize = 11.sp, color = Merah)
                        }
                    }
                }
            }
            "hasil" -> HasilKuisSession(
                hasil = submitRes,
                onReview = {
                    if (jwt.isEmpty()) {
                        val kunci = DataDemo.demoJawaban[tugasId] ?: DataDemo.demoJawaban["demo1"]!!
                        reviewRes = KuisSessionReviewRes(
                            sessionId = "demo-session",
                            skor = submitRes?.skor,
                            benarCount = submitRes?.benarCount,
                            totalSoal = submitRes?.totalSoal,
                            durasiDetik = null,
                            review = soal.mapIndexed { i, s ->
                                val jawabAnak = jawaban[s.posisi]
                                val jawabBenar = kunci.getOrNull(i) ?: 0
                                ReviewSoal(
                                    posisi = i,
                                    question = s.question,
                                    options = s.options,
                                    jawabanAnak = jawabAnak,
                                    jawabanBenar = jawabBenar,
                                    benar = jawabAnak == jawabBenar,
                                    explanation = if (jawabAnak == jawabBenar) "Benar!" else "Jawaban benar: ${s.options.getOrElse(jawabBenar) { "" }}",
                                )
                            },
                        )
                        fase = "review"
                    } else {
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
                    }
                },
                onRetry = {
                    fase = "loading"
                    sessionId = ""
                    soal = emptyList()
                    jawaban.clear()
                    submitRes = null
                    reviewRes = null
                    if (jwt.isEmpty()) {
                        val soalDemo = DataDemo.demoSoal[tugasId] ?: DataDemo.demoSoal["demo1"]!!
                        soal = soalDemo.mapIndexed { i, s -> SoalSession(posisi = i, question = s.q, options = s.options) }
                        sessionId = "demo-session"
                        jawaban.clear()
                        fase = "soal"
                    } else {
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
                if (jwt.isEmpty()) {
                    val soalDemo = DataDemo.demoSoal[tugasId] ?: DataDemo.demoSoal["demo1"]!!
                    soal = soalDemo.mapIndexed { i, s -> SoalSession(posisi = i, question = s.q, options = s.options) }
                    sessionId = "demo-session"
                    jawaban.clear()
                    fase = "soal"
                } else {
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
                Text("$no.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Biru)
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
                        terpilih -> Biru
                        terkunci -> Muted
                        else -> Kartu
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
                                        terkunci -> Border
                                        else -> Biru.copy(alpha = 0.15f)
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
                                    terkunci -> MutedFg
                                    else -> Biru
                                },
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            opt,
                            fontSize = 13.sp,
                            color = when {
                                terpilih -> Color.White
                                terkunci -> MutedFg
                                else -> Teks
                            },
                        )
                        if (terkunci && !terpilih) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Border, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            if (terkunci) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.answers_locked), fontSize = 10.sp, color = MutedFg)
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
        persen >= 70 -> Hijau
        persen >= 50 -> Amber
        else -> Merah
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
                    Text("$benar dari $total benar", fontSize = 14.sp, color = MutedFg)
                    Spacer(Modifier.height(16.dp))
                    BadgePill("+$token ZX", Hijau.copy(alpha = .12f), Hijau)
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
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.explanation), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (review != null) {
                    val persen = if ((review.totalSoal ?: 0) > 0) ((review.benarCount ?: 0) * 100 / review.totalSoal!!) else 0
                    BadgePill("$persen%", Biru.copy(alpha = .12f), BiruGelap)
                }
            }
        }
        itemsIndexed(review?.review ?: emptyList()) { i, r ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (r.benar) Hijau.copy(alpha = .08f) else Merah.copy(alpha = .05f),
                ),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (r.benar) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (r.benar) Hijau else Merah,
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
                            benarIdx -> Hijau.copy(alpha = .12f)
                            anakIdx -> Merah.copy(alpha = .12f)
                            else -> Color.Transparent
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${('A' + idx)}.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (benarIdx) Hijau else if (anakIdx) Merah else MutedFg)
                            Spacer(Modifier.width(6.dp))
                            Text(opt, fontSize = 12.sp, color = if (benarIdx) Hijau else if (anakIdx) Merah else Teks)
                            if (benarIdx) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, contentDescription = null, tint = Hijau, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (r.explanation.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Divider(color = Border)
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Amber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(r.explanation, fontSize = 11.sp, color = MutedFg)
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
