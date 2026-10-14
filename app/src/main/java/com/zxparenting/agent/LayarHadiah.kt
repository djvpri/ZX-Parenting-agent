package com.zxparenting.agent

import com.zxparenting.agent.tema.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var kuotaMain by remember { mutableStateOf(0) }
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
    val strLocked = stringResource(R.string.locked)
    val strZX = stringResource(R.string.tokens_unit)
    val strCoins = stringResource(R.string.coins_unit)

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
            kuotaMain = 30
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
            val km = withContext(Dispatchers.IO) { runCatching { Klien.api.kuotaMain(bearer) }.getOrNull() }

            saldo = s?.body()?.saldo ?: 0
            streak = st?.body()
            challenge = ch?.body()
            katalog = ka?.body()?.katalog ?: emptyList()
            quests = qs?.body() ?: emptyList()
            ledger = le?.body()?.ledger ?: emptyList()
            hutang = pj?.body()?.hutang ?: 0
            kuotaMain = km?.body()?.menit ?: 0
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ===== SALDO HERO =====
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Biru, Ungu)))
                    .padding(24.dp, 20.dp),
            ) {
                Column {
                    Text(
                        stringResource(R.string.token_balance),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = .7f),
                    )
                    Text("$saldo", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    val todayGain = ledger.filter { it.jumlah > 0 }.sumOf { it.jumlah }
                    if (todayGain > 0) {
                        Text(stringResource(R.string.today_gain, todayGain), fontSize = 11.sp, color = Color.White.copy(alpha = .6f))
                    }
                }
                // Coin icon
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.White.copy(alpha = .15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // ===== STREAK BAR =====
        streak?.let { sk ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Kartu),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                                .background(Amber.copy(alpha = .12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${sk.current} $strStreak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedFg)
                            // Streak bar
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Biru.copy(alpha = .1f)),
                            ) {
                                val pct = if (sk.terbaik > 0) (sk.current.toFloat() / sk.terbaik).coerceAtMost(1f) else 0f
                                Box(
                                    Modifier
                                        .fillMaxWidth(pct)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Brush.horizontalGradient(listOf(Amber, Merah))),
                                )
                            }
                            Text("$strTerbaik: ${sk.terbaik}", fontSize = 10.sp, color = MutedFg.copy(alpha = .6f))
                        }
                    }
                }
            }
        }

        // ===== CHALLENGE BANNER =====
        challenge?.let { ch ->
            if (ch.id != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Kartu),
                        elevation = CardDefaults.cardElevation(2.dp),
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(Amber, Merah))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(ch.judul ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                ch.topik?.let { Text(it, fontSize = 11.sp, color = MutedFg) }
                            }
                            Text("+${ch.tokenReward}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber)
                        }
                    }
                }
            }
        }

        // ===== QUICK DOCK =====
        item {
            SectionLabel(stringResource(R.string.quick_actions))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Tukar dock
                DockBtn(
                    icon = Icons.Default.SwapHoriz,
                    label = strTukar,
                    bg = Hijau.copy(alpha = .1f),
                    tint = Hijau,
                    modifier = Modifier.weight(1f),
                ) {
                    // Scroll to tukar section — just trigger inline below
                }
                // Minta dock
                DockBtn(
                    icon = Icons.Default.CardGiftcard,
                    label = strMinta,
                    bg = Pink.copy(alpha = .1f),
                    tint = Pink,
                    modifier = Modifier.weight(1f),
                ) {
                    // Scroll to katalog
                }
                // Pinjam dock
                DockBtn(
                    icon = Icons.Default.Schedule,
                    label = stringResource(R.string.pinjam_waktu),
                    bg = Amber.copy(alpha = .1f),
                    tint = Amber,
                    modifier = Modifier.weight(1f),
                ) {
                    // Scroll to pinjam
                }
            }
        }

        // ===== TUKAR TOKEN (compact) =====
        item {
            SectionLabel(stringResource(R.string.tukar_token))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Hijau.copy(alpha = .08f)),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(stringResource(R.string.tukar_token), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Hijau)
                            Text(menitOptions.joinToString(" · ") { "$it" } + " $strMenit", fontSize = 10.sp, color = MutedFg)
                        }
                        Button(
                            onClick = {
                                if (demo) {
                                    tukarMsg = "Demo: $tukarMenit $strMenit"
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
                                        kuotaMain += body.menit
                                        tukarMsg = "+${body.menit} $strMenit"
                                    } else {
                                        tukarMsg = "Saldo tidak cukup"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Hijau),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(strTukar, fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Quick select chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        menitOptions.forEach { m ->
                            FilterChip(
                                selected = tukarMenit == m,
                                onClick = { tukarMenit = m },
                                label = { Text("$m", fontSize = 11.sp) },
                            )
                        }
                    }
                    if (tukarMsg.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(tukarMsg, fontSize = 11.sp, color = Hijau)
                    }
                }
            }
        }

        // ===== KUOTA WAKTU MAIN =====
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Teal.copy(alpha = .08f)),
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(Teal, Biru))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.kuota_main), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Teal)
                        if (kuotaMain > 0) {
                            Text(stringResource(R.string.kuota_main_sub, kuotaMain), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MutedFg)
                        } else {
                            Text(stringResource(R.string.kuota_empty), fontSize = 11.sp, color = MutedFg.copy(alpha = .6f))
                        }
                    }
                }
            }
        }

        // ===== QUEST KELUARGA =====
        if (quests.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.quest_family)) }
            items(quests) { q ->
                val selesai = q.anggota?.any { it.selesai } == true
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Kartu),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            if (selesai) Icons.Default.CheckCircle else Icons.Default.Pending,
                            contentDescription = null,
                            tint = if (selesai) Hijau else Teal,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(q.judul, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("+${q.tokenReward} $strZX", fontSize = 11.sp, color = MutedFg)
                        }
                        if (selesai) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Hijau, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // ===== KATALOG GRID =====
        if (katalog.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.katalog_hadiah)) }
            // Grid: 2 columns, manual rows
            val rows = katalog.chunked(2)
            items(rows) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { h ->
                        KatalogGridCard(
                            hadiah = h,
                            strMinta = strMinta,
                            strSudahMinta = strSudahMinta,
                            strHabis = strHabis,
                            strLocked = strLocked,
                            strCoins = strCoins,
                            modifier = Modifier.weight(1f),
                            onRequest = {
                                if (demo) return@KatalogGridCard
                                scope.launch {
                                    val bearer = "Bearer $jwt"
                                    withContext(Dispatchers.IO) {
                                        runCatching { Klien.api.mintaHadiah(bearer, MintaHadiahReq(h.id)) }
                                    }
                                }
                            },
                        )
                    }
                    // Fill empty slot if odd
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // ===== RIWAYAT TIMELINE =====
        if (ledger.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.riwayat_token)) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Kartu),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    ledger.take(8).forEachIndexed { i, l ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Dot
                            Box(
                                Modifier.size(8.dp).clip(RoundedCornerShape(50.dp))
                                    .background(if (l.jumlah > 0) Hijau else Merah),
                            )
                            Text(
                                (if (l.jumlah > 0) "+" else "") + l.jumlah,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (l.jumlah > 0) Hijau else Merah,
                                modifier = Modifier.width(45.dp),
                            )
                            Text(
                                l.catatan ?: "",
                                fontSize = 11.sp,
                                color = MutedFg,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                l.tipe,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedFg,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Biru.copy(alpha = .08f))
                                    .padding(4.dp, 2.dp),
                            )
                        }
                        if (i < minOf(ledger.size, 8) - 1) {
                            HorizontalDivider(color = Biru.copy(alpha = .06f), thickness = 1.dp)
                        }
                    }
                }
                }
            }
        }

        // ===== PINJAM WAKTU (compact) =====
        item {
            SectionLabel(stringResource(R.string.pinjam_waktu))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OranyeBg),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(String.format(strHutang, hutang), fontSize = 12.sp, color = OranyeGelap)
                    }
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
                        colors = ButtonDefaults.buttonColors(containerColor = Oranye),
                        enabled = hutang == 0,
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(String.format(stringResource(R.string.borrow_n), 15), fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun DockBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bg: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
private fun KatalogGridCard(
    hadiah: HadiahItem,
    strMinta: String,
    strSudahMinta: String,
    strHabis: String,
    strLocked: String,
    strCoins: String,
    modifier: Modifier = Modifier,
    onRequest: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Kartu),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Pink.copy(alpha = .1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Pink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                hadiah.judul,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text("${hadiah.hargaCoin} $strCoins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Pink)
            Spacer(Modifier.height(8.dp))
            when {
                hadiah.sudahMinta -> {
                    Text(strSudahMinta, fontSize = 11.sp, color = MutedFg)
                }
                hadiah.stok <= 0 -> {
                    Text(strHabis, fontSize = 11.sp, color = Merah)
                }
                hadiah.hargaCoin > 0 && hadiah.stok > 0 -> {
                    Button(
                        onClick = onRequest,
                        colors = ButtonDefaults.buttonColors(containerColor = Pink),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(12.dp, 6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(strMinta, fontSize = 11.sp) }
                }
                else -> {
                    Text(strLocked, fontSize = 11.sp, color = MutedFg)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MutedFg,
        modifier = Modifier.padding(top = 4.dp))
}
