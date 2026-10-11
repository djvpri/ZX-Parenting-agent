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
fun LayarHadiah(jwt: String?) {
    val scope = rememberCoroutineScope()
    val demo = jwt == null

    var saldo by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf<StreakRes?>(null) }
    var challenge by remember { mutableStateOf<ChallengeRes?>(null) }
    var katalog by remember { mutableStateOf<List<HadiahItem>>(emptyList()) }
    var quests by remember { mutableStateOf<List<QuestAnak>>(emptyList()) }
    var ledger by remember { mutableStateOf<List<LedgerItem>>(emptyList()) }
    var hutang by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var tukarMenit by remember { mutableStateOf(30) }
    var tukarMsg by remember { mutableStateOf("") }

    val strTukar = stringResource(R.string.tukar_btn)
    val strMenit = stringResource(R.string.minutes)
    val strMinta = stringResource(R.string.btn_minta)
    val strSudahMinta = stringResource(R.string.btn_sudah_minta)
    val strHabis = stringResource(R.string.btn_habis)
    val strHutang = stringResource(R.string.hutang_aktif)
    val strStreak = stringResource(R.string.streak_hari)
    val strTerbaik = stringResource(R.string.streak_terbaik)

    val menitOptions = listOf(5, 15, 30, 60, 120)

    LaunchedEffect(jwt) {
        if (demo) {
            saldo = DataDemo.saldo
            streak = DataDemo.streak
            challenge = DataDemo.challenge
            katalog = DataDemo.katalog
            quests = DataDemo.quests
            ledger = DataDemo.ledger
            hutang = DataDemo.pinjam.hutang
            loading = false
            return@LaunchedEffect
        }
        scope.launch {
            val bearer = "Bearer $jwt"
            val s = withContext(Dispatchers.IO) { runCatching { Klien.api.saldo(bearer) }.getOrNull() }
            val st = withContext(Dispatchers.IO) { runCatching { Klien.api.streak(bearer) }.getOrNull() }
            val ch = withContext(Dispatchers.IO) { runCatching { Klien.api.challenge(bearer) }.getOrNull() }
            val ka = withContext(Dispatchers.IO) { runCatching { Klien.api.katalogHadiah(bearer) }.getOrNull() }
            val qs = withContext(Dispatchers.IO) { runCatching { Klien.api.questsSaya(bearer) }.getOrNull() }
            val le = withContext(Dispatchers.IO) { runCatching { Klien.api.riwayatToken(bearer) }.getOrNull() }
            val pj = withContext(Dispatchers.IO) { runCatching { Klien.api.pinjamStatus(bearer) }.getOrNull() }

            saldo = s?.body()?.saldo ?: 0
            streak = st?.body()
            challenge = ch?.body()
            katalog = ka?.body()?.katalog ?: emptyList()
            quests = qs?.body() ?: emptyList()
            ledger = le?.body()?.ledger ?: emptyList()
            hutang = pj?.body()?.hutang ?: 0
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(Color(0xFFEFF6FF)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFEFF6FF)),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: saldo + streak
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2563EB)),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.token_balance), fontSize = 13.sp, color = Color.White.copy(alpha = .8f))
                    Text("$saldo", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    streak?.let {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${it.current} $strStreak", fontSize = 12.sp, color = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text("$strTerbaik: ${it.terbaik}", fontSize = 12.sp, color = Color.White.copy(alpha = .7f))
                        }
                    }
                }
            }
        }

        // Challenge harian
        challenge?.let { ch ->
            if (ch.id != null) {
                item {
                    SectionLabel(stringResource(R.string.challenge_harian))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = .12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ch.judul ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                ch.topik?.let { Text(it, fontSize = 12.sp, color = Color(0xFF94A3B8)) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+${ch.tokenReward}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Tukar token
        item {
            SectionLabel(stringResource(R.string.tukar_token))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text("$tukarMenit $strMenit") },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (demo) {
                                    tukarMsg = "Demo: $tukarMenit menit ditukar"
                                    saldo -= tukarMenit
                                    return@Button
                                }
                                scope.launch {
                                    val bearer = "Bearer $jwt"
                                    val res = withContext(Dispatchers.IO) {
                                        runCatching { Klien.api.tukarToken(bearer, TukarTokenReq(tukarMenit)) }.getOrNull()
                                    }
                                    val body = res?.body()
                                    if (res?.isSuccessful == true && body?.ok == true) {
                                        saldo = body.saldo
                                        tukarMsg = "+${body.menit} $strMenit"
                                    } else {
                                        tukarMsg = "Saldo tidak cukup"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        ) { Text(strTukar) }
                    }
                    // Quick select chips
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        menitOptions.forEach { m ->
                            FilterChip(
                                selected = tukarMenit == m,
                                onClick = { tukarMenit = m },
                                label = { Text("$m") },
                            )
                        }
                    }
                    if (tukarMsg.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(tukarMsg, fontSize = 11.sp, color = Color(0xFF16A34A))
                    }
                }
            }
        }

        // Quest keluarga
        if (quests.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.quest_family)) }
            items(quests) { q ->
                val selesai = q.anggota?.any { it.selesai } == true
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (selesai) Icons.Default.CheckCircle else Icons.Default.Pending,
                            contentDescription = null,
                            tint = if (selesai) Color(0xFF16A34A) else Color(0xFF14B8A6),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(q.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("+${q.tokenReward} tokens", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }

        // Katalog hadiah
        if (katalog.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.katalog_hadiah)) }
            items(katalog) { h ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEC4899).copy(alpha = .12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.judul, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${h.hargaCoin} coins", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }
                        when {
                            h.sudahMinta -> Text(strSudahMinta, fontSize = 12.sp, color = Color(0xFF94A3B8))
                            h.stok <= 0 -> Text(strHabis, fontSize = 12.sp, color = Color(0xFF991B1B))
                            else -> Button(
                                onClick = {
                                    if (demo) return@Button
                                    scope.launch {
                                        val bearer = "Bearer $jwt"
                                        withContext(Dispatchers.IO) {
                                            runCatching { Klien.api.mintaHadiah(bearer, MintaHadiahReq(h.id)) }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                contentPadding = PaddingValues(12.dp, 4.dp),
                            ) { Text(strMinta, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }

        // Riwayat token
        if (ledger.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.riwayat_token)) }
            items(ledger) { l ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            (if (l.jumlah > 0) "+" else "") + l.jumlah,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (l.jumlah > 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.width(55.dp),
                        )
                        Text(l.tipe, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(4.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(l.catatan ?: "", fontSize = 12.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Pinjam waktu
        item {
            SectionLabel(stringResource(R.string.pinjam_waktu))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(String.format(strHutang, hutang), fontSize = 13.sp, color = Color(0xFF9A3412))
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (demo) return@Button
                            scope.launch {
                                val bearer = "Bearer $jwt"
                                val res = withContext(Dispatchers.IO) {
                                    runCatching { Klien.api.pinjamAjukan(bearer, PinjamReq(15)) }.getOrNull()
                                }
                                if (res?.isSuccessful == true) {
                                    val b = res.body()
                                    saldo += b?.menit ?: 0
                                    hutang += b?.menit ?: 0
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        enabled = hutang == 0,
                    ) { Text(String.format(stringResource(R.string.pinjam_btn), 15)) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569),
        modifier = Modifier.padding(top = 4.dp))
}
