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
        if (jwt.isEmpty()) {
            // Demo mode (Play Console reviewer)
            val demoSoalList = DataDemo.demoSoal[kuisId]
            val demoArtikel = DataDemo.demoDetail[kuisId]
            if (demoSoalList != null) {
                val item = DataDemo.officialItems.find { it.id == kuisId }
                detail = OfficialDetail(
                    id = kuisId,
                    tipe = item?.tipe ?: "KUIS_HARIAN",
                    judul = item?.judul ?: DataDemo.challenge.judul ?: "Quiz",
                    deskripsi = item?.deskripsi,
                    konten = demoSoalList,
                    kategori = item?.meta?.kategori,
                    tokenReward = item?.meta?.tokenReward ?: 15,
                    premium = false,
                    selesai = false,
                )
                jawaban = MutableList(demoSoalList.size) { -1 }
            } else if (demoArtikel != null) {
                detail = demoArtikel
                jawaban = MutableList(0) { -1 }
            }
            loading = false
            if (detail == null) error = strFailLoad
            return@LaunchedEffect
        }
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
            .background(Bg),
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
                BadgePill("+${detail!!.tokenReward} ZX", Hijau.copy(alpha = .12f), Hijau)
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
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Hijau, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.already_done), fontWeight = FontWeight.Bold)
                    }
                }
            }
            detail != null -> {
                val soal = detail!!.konten ?: emptyList()
                if (soal.isEmpty()) {
                    // Artikel/video reader
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(detail!!.deskripsi ?: "", fontSize = 15.sp, color = Teks, lineHeight = 24.sp)
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onKembali,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(stringResource(R.string.done)) }
                    }
                } else if (fase == "soal") {
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
                                    if (jwt.isEmpty()) {
                                        val kunci = DataDemo.demoJawaban[kuisId] ?: emptyList()
                                        val skor = jawaban.mapIndexed { i, ans -> if (i < kunci.size && ans == kunci[i]) 1 else 0 }.sum()
                                        hasil = CompleteRes(ok = true, skor = skor, totalSoal = jawaban.size, tokenDidapat = skor * 3)
                                        fase = "hasil"
                                    } else {
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
                    color = if (terpilih) Biru else Kartu,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (terpilih) Color.White else Biru.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                ('A' + idx).toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (terpilih) Biru else Biru,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(opt, fontSize = 13.sp, color = if (terpilih) Color.White else Teks)
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
        persen >= 70 -> Hijau
        persen >= 50 -> Amber
        else -> Merah
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
                Text("$skor dari $total benar", fontSize = 14.sp, color = MutedFg)
                Spacer(Modifier.height(16.dp))
                BadgePill("+$token ZX", Hijau.copy(alpha = .12f), Hijau)
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
