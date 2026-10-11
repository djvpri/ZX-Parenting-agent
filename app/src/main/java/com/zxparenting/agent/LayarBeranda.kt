package com.zxparenting.agent

import com.zxparenting.agent.tema.*

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource

@Composable
fun LayarBeranda(jwt: String?, nama: String, umur: Int, onBukaKuis: (String) -> Unit = {}) {
    var items by remember { mutableStateOf<List<OfficialItem>>(emptyList()) }
    var tugas by remember { mutableStateOf<List<TugasAnak>>(emptyList()) }
    var challenge by remember { mutableStateOf<ChallengeRes?>(null) }
    var streak by remember { mutableStateOf<StreakRes?>(null) }
    var saldo by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val strFailLoad = stringResource(R.string.fail_load_content)
    LaunchedEffect(jwt) {
        if (jwt.isNullOrEmpty()) {
            items = DataDemo.officialItems
            tugas = DataDemo.tugas
            challenge = DataDemo.challenge
            streak = DataDemo.streak
            saldo = DataDemo.saldo
            loading = false
            return@LaunchedEffect
        }
        scope.launch {
            val bearer = "Bearer $jwt"
            val resFeed = withContext(Dispatchers.IO) {
                runCatching { Klien.api.feedOfficial(bearer) }.getOrNull()
            }
            val resTugas = withContext(Dispatchers.IO) {
                runCatching { Klien.api.tugasList(bearer) }.getOrNull()
            }
            val resSaldo = withContext(Dispatchers.IO) {
                runCatching { Klien.api.saldo(bearer) }.getOrNull()
            }
            val resChallenge = withContext(Dispatchers.IO) {
                runCatching { Klien.api.challenge(bearer) }.getOrNull()
            }
            val resStreak = withContext(Dispatchers.IO) {
                runCatching { Klien.api.streak(bearer) }.getOrNull()
            }
            loading = false
            if (resFeed?.isSuccessful == true) {
                items = resFeed.body()?.items ?: emptyList()
            } else {
                error = strFailLoad
            }
            if (resTugas?.isSuccessful == true) {
                tugas = resTugas.body() ?: emptyList()
            }
            if (resSaldo?.isSuccessful == true) {
                saldo = resSaldo.body()?.saldo ?: 0
            }
            if (resChallenge?.isSuccessful == true) {
                challenge = resChallenge.body()
            }
            if (resStreak?.isSuccessful == true) {
                streak = resStreak.body()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.home), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            BadgePill(stringResource(R.string.zx_official), Hijau.copy(alpha = .12f), Hijau)
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { GreetingCard(nama, umur, streak, saldo) }
            item { AccessibilityWarning() }

            if (challenge != null) {
                item {
                    SectionLabel(stringResource(R.string.section_challenge_today))
                    ChallengeCard(challenge!!, onBukaKuis)
                }
            }

            val quizItems = items.filter { it.tipe == "KUIS_HARIAN" || it.tipe == "TANTANGAN_MINGGUAN" }
            if (quizItems.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_quiz_today)) }
                items(quizItems) { item ->
                    KartuOfficial(item, onClick = { onBukaKuis(item.id) })
                }
            }

            val readItems = items.filter { it.tipe == "ARTIKEL" || it.tipe == "VIDEO_PENDEK" }
            if (readItems.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_read_watch)) }
                items(readItems) { item ->
                    KartuOfficial(item, onClick = {})
                }
            }

            if (tugas.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.section_task_parent)) }
                items(tugas) { t -> TugasCard(t) }
            }

            if (error.isNotEmpty()) {
                item { Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun GreetingCard(nama: String, umur: Int, streak: StreakRes?, saldo: Int) {
    val strHi = stringResource(R.string.greeting_hi)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Biru, Ungu))),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("$strHi $nama!", fontSize = 13.sp, color = Color.White.copy(alpha = .85f))
            Text("$nama, $umur", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GreetingStat(Icons.Default.LocalFireDepartment, "${streak?.current ?: 0}", stringResource(R.string.streak_hari))
                GreetingStat(Icons.Default.MonetizationOn, "$saldo", "token")
                GreetingStat(Icons.Default.EmojiEvents, stringResource(R.string.level_short, 4), "")
            }
        }
    }
}

@Composable
private fun GreetingStat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (label.isNotBlank()) {
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = .8f))
        }
    }
}

@Composable
private fun AccessibilityWarning() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Amber.copy(alpha = .1f))
            .border(1.dp, Amber.copy(alpha = .4f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.accessibility_off), fontSize = 12.sp, color = OranyeGelap, modifier = Modifier.weight(1f))
        TextButton(
            onClick = {
                val intent = Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Amber),
        ) {
            Text(stringResource(R.string.open), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MutedFg,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun ChallengeCard(c: ChallengeRes, onBukaKuis: (String) -> Unit) {
    Card(
        onClick = { if (!c.selesai && c.id != null) onBukaKuis(c.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(4.dp, Amber),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Amber.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Amber, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(c.judul ?: "", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Teks)
                    Text(c.topik ?: "", fontSize = 12.sp, color = MutedFg)
                }
                BadgePill(stringResource(R.string.challenge), Amber.copy(alpha = .12f), Amber)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(Muted),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (c.selesai) 1f else 0f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Amber),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.complete_one_quiz), fontSize = 12.sp, color = MutedFg)
                BadgePill("+${c.tokenReward}", Amber.copy(alpha = .15f), OranyeGelap)
            }
        }
    }
}

@Composable
private fun TugasCard(t: TugasAnak) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Biru.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = Biru, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.judul, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Teks)
                    t.deskripsi?.let { Text(it, fontSize = 12.sp, color = MutedFg) }
                }
                BadgePill(stringResource(R.string.from_parent), Biru.copy(alpha = .12f), Biru)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                t.deadline?.let { Text(it, fontSize = 12.sp, color = MutedFg) }
                BadgePill("+${t.reward}", Amber.copy(alpha = .15f), OranyeGelap)
            }
        }
    }
}

@Composable
private fun KartuOfficial(item: OfficialItem, onClick: () -> Unit = {}) {
    val meta = item.meta
    val tipeLabel = when (item.tipe) {
        "KUIS_HARIAN" -> stringResource(R.string.daily_quiz)
        "TANTANGAN_MINGGUAN" -> stringResource(R.string.challenge)
        "ARTIKEL" -> stringResource(R.string.article)
        "VIDEO_PENDEK" -> stringResource(R.string.video)
        else -> item.tipe
    }
    val warnaTipe = when (item.tipe) {
        "KUIS_HARIAN" -> Biru
        "TANTANGAN_MINGGUAN" -> Amber
        "ARTIKEL" -> Ungu
        else -> Hijau
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(warnaTipe.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = when (item.tipe) {
                        "KUIS_HARIAN" -> Icons.Default.Quiz
                        "TANTANGAN_MINGGUAN" -> Icons.Default.EmojiEvents
                        "ARTIKEL" -> Icons.Default.Article
                        else -> Icons.Default.PlayCircle
                    }
                    Icon(icon, contentDescription = null, tint = warnaTipe, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.judul, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Teks)
                    val subText = buildList {
                        if (item.tipe == "KUIS_HARIAN" || item.tipe == "TANTANGAN_MINGGUAN") {
                            if (meta != null) {
                                add(stringResource(R.string.questions_count, 5))
                                add(stringResource(R.string.level_n, meta.levelKesulitan ?: 1))
                            }
                        }
                        if (item.tipe == "ARTIKEL") add(stringResource(R.string.min_n, 3))
                        if (item.tipe == "VIDEO_PENDEK") add(stringResource(R.string.min_n, 2))
                        meta?.rentangUmur?.let { add("Umur $it") }
                    }.joinToString(" \u00B7 ")
                    if (subText.isNotBlank()) {
                        Text(subText, fontSize = 12.sp, color = MutedFg)
                    }
                }
                BadgePill(tipeLabel, warnaTipe.copy(alpha = .12f), warnaTipe)
            }
            if (!item.deskripsi.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(item.deskripsi, fontSize = 13.sp, color = MutedFg, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (meta != null) {
                    Text(stringResource(R.string.views_count, formatCount(meta.views)), fontSize = 12.sp, color = MutedFg)
                }
                BadgePill("+${meta?.tokenReward ?: 0}", Amber.copy(alpha = .15f), OranyeGelap)
            }
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1000 -> "${n / 1000}.${(n % 1000) / 100}k"
    else -> n.toString()
}

@Composable
fun BadgePill(teks: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(teks, fontSize = 11.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}
