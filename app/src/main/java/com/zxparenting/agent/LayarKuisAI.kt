package com.zxparenting.agent

import com.zxparenting.agent.tema.*

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource

@Composable
fun LayarKuisAI(jwt: String, onKembali: () -> Unit, topikAwal: String = "") {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val strTopicReq = stringResource(R.string.topic_required)
    val strFailCreate = stringResource(R.string.fail_create_quiz)
    val strFailSubmit = stringResource(R.string.fail_submit)
    var topik by remember { mutableStateOf(topikAwal) }
    var level by remember { mutableStateOf(3) }
    var jumlahSoal by remember { mutableStateOf(5) }
    var timerAktif by remember { mutableStateOf(false) }
    var durasiSoal by remember { mutableStateOf(30) } // detik per soal
    var fotoBase64 by remember { mutableStateOf("") }
    var fotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fase by remember { mutableStateOf("input") } // input | soal | hasil
    var soal by remember { mutableStateOf<List<Soal>>(emptyList()) }
    var jawaban by remember { mutableStateOf<MutableList<Int>>(mutableListOf()) }
    var attemptId by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var hasil by remember { mutableStateOf<SubmitKuisRes?>(null) }
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bmp ->
        if (bmp != null) {
            val baos = java.io.ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            fotoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            fotoBitmap = bmp
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val b64 = withContext(Dispatchers.IO) {
                    val input = ctx.contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(input)
                    input?.close()
                    if (bmp != null) {
                        // Resize max 1080px.
                        val scaled = if (bmp.width > 1080 || bmp.height > 1080) {
                            val ratio = 1080f / maxOf(bmp.width, bmp.height)
                            Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                        } else bmp
                        val baos = java.io.ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                        fotoBitmap = scaled
                        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    } else ""
                }
                if (b64.isNotEmpty()) fotoBase64 = b64
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onKembali) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text(stringResource(R.string.quiz_ai), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        when (fase) {
            "input" -> FormKuisAI(topik, level, jumlahSoal, timerAktif, durasiSoal, fotoBase64, fotoBitmap, loading, error,
                onTopik = { topik = it },
                onLevel = { level = it },
                onJumlah = { jumlahSoal = it },
                onTimerAktif = { timerAktif = it },
                onDurasi = { durasiSoal = it },
                onFoto = { fotoBase64 = ""; fotoBitmap = null },
                onCamera = { cameraLauncher.launch(null) },
                onGallery = { galleryLauncher.launch("image/*") },
                onMulai = {
                    if (topik.isBlank()) { error = strTopicReq; return@FormKuisAI }
                    loading = true; error = ""
                    if (jwt.isEmpty()) {
                        // Demo mode — generate soal statis berdasarkan topik
                        val demoSoalAi = (1..jumlahSoal).map { i ->
                            Soal("Soal $i tentang $topik?", listOf("Opsi A", "Opsi B", "Opsi C", "Opsi D"))
                        }
                        soal = demoSoalAi
                        jawaban = MutableList(demoSoalAi.size) { -1 }
                        attemptId = "demo-ai"
                        loading = false
                        fase = "soal"
                        return@FormKuisAI
                    }
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            runCatching {
                                if (fotoBase64.isNotEmpty()) {
                                    Klien.api.kuisFotoStart("Bearer $jwt", KuisFotoReq(topik.trim(), level, jumlahSoal, fotoBase64))
                                } else {
                                    Klien.api.kuisStart("Bearer $jwt", KuisAIReq(topik.trim(), level, jumlahSoal))
                                }
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
                            LaporError.kirim(ctx, "LayarKuisAI", "kuisStart fail", mapOf("topik" to topik, "foto" to (fotoBase64.isNotEmpty()), "code" to (res?.code() ?: -1)))
                        }
                    }
                },
            )
            "soal" -> {
                val totalDetik = durasiSoal * soal.size
                var sisaDetik by remember { mutableStateOf(totalDetik) }
                val submitAction: () -> Unit = {
                    if (jwt.isEmpty()) {
                        val kunci = (0 until soal.size).map { it % 4 }
                        val skor = jawaban.mapIndexed { i, ans -> if (i < kunci.size && ans == kunci[i]) 1 else 0 }.sum()
                        hasil = SubmitKuisRes(
                            skor = skor,
                            totalSoal = soal.size,
                            tokenDidapat = skor * 3,
                            challengeSelesai = false,
                            hasil = soal.mapIndexed { i, s ->
                                HasilSoal(
                                    q = s.q,
                                    jawabAnak = maxOf(0, jawaban.getOrElse(i) { -1 }),
                                    jawabBenar = kunci.getOrElse(i) { 0 },
                                    benar = jawaban.getOrElse(i) { -1 } == kunci.getOrElse(i) { 0 },
                                    explain = if (jawaban.getOrElse(i) { -1 } == kunci.getOrElse(i) { 0 }) "Benar!" else "Jawaban benar: ${s.options.getOrElse(kunci.getOrElse(i) { 0 }) { "" }}",
                                )
                            },
                        )
                        fase = "hasil"
                    } else {
                        scope.launch {
                            val res = withContext(Dispatchers.IO) {
                                runCatching {
                                    Klien.api.kuisSubmit("Bearer $jwt", attemptId, SubmitKuisReq(jawaban.map { maxOf(0, it) }.toList()))
                                }.getOrNull()
                            }
                            val r = res?.body()
                            if (res?.isSuccessful == true && r != null) {
                                hasil = r
                                fase = "hasil"
                            } else {
                                error = strFailSubmit
                                LaporError.kirim(ctx, "LayarKuisAI", "kuisSubmit fail", mapOf("attemptId" to attemptId, "code" to (res?.code() ?: -1)))
                            }
                        }
                    }
                    Unit
                }
                if (timerAktif) {
                    LaunchedEffect(soal.size) {
                        while (sisaDetik > 0 && fase == "soal") {
                            kotlinx.coroutines.delay(1000L)
                            sisaDetik--
                        }
                        if (sisaDetik <= 0 && fase == "soal") {
                            submitAction()
                        }
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (timerAktif) {
                        item {
                            val mnt = sisaDetik / 60
                            val dtk = sisaDetik % 60
                            val urgent = sisaDetik <= 10
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Surface(
                                    color = if (urgent) Merah.copy(alpha = 0.15f) else Biru.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (urgent) Merah else Biru,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "%02d:%02d".format(mnt, dtk),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (urgent) Merah else Biru,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    itemsIndexed(soal) { i, s ->
                        KartuSoalAI(i + 1, s, jawaban[i]) {
                            jawaban = jawaban.toMutableList().also { l -> l[i] = it }
                        }
                    }
                    item {
                        val semua = jawaban.all { it >= 0 }
                        Button(
                            onClick = submitAction,
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
    jumlahSoal: Int,
    timerAktif: Boolean,
    durasiSoal: Int,
    fotoBase64: String,
    fotoBitmap: Bitmap?,
    loading: Boolean,
    error: String,
    onTopik: (String) -> Unit,
    onLevel: (Int) -> Unit,
    onJumlah: (Int) -> Unit,
    onTimerAktif: (Boolean) -> Unit,
    onDurasi: (Int) -> Unit,
    onFoto: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onMulai: () -> Unit,
) {
    val pilihanSoal = listOf(5, 10, 15, 20)
    val pilihanDurasi = listOf(15, 30, 60, 90)
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
                Text(stringResource(R.string.number_of_questions), fontSize = 12.sp, color = MutedFg)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pilihanSoal.forEach { n ->
                        FilterChip(
                            selected = jumlahSoal == n,
                            onClick = { onJumlah(n) },
                            label = { Text("$n") },
                        )
                    }
                }
                Text("Tingkat kesulitan: $level/5", fontSize = 12.sp, color = MutedFg)
                Slider(
                    value = level.toFloat(),
                    onValueChange = { onLevel(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.timer_per_question), fontSize = 12.sp, color = MutedFg)
                    Switch(checked = timerAktif, onCheckedChange = onTimerAktif)
                }
                if (timerAktif) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pilihanDurasi.forEach { d ->
                            FilterChip(
                                selected = durasiSoal == d,
                                onClick = { onDurasi(d) },
                                label = { Text("${d}s") },
                            )
                        }
                    }
                }
                Text(stringResource(R.string.quiz_from_photo), fontSize = 12.sp, color = MutedFg)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCamera, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.camera), fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.gallery), fontSize = 12.sp)
                    }
                }
                if (fotoBitmap != null) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                        Image(
                            bitmap = fotoBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.photo_preview),
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Crop,
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            onClick = onFoto,
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_photo), tint = Color.White, modifier = Modifier.size(20.dp).padding(2.dp))
                        }
                    }
                }
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
