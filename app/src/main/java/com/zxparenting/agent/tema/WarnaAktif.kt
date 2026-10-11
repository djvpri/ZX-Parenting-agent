package com.zxparenting.agent.tema

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Singleton palet aktif. Swap runtime: WarnaAktif.ganti(palet).
// Compose auto-recompose semua pemanggilan val Bg/Biru/dst.
object WarnaAktif {
    var palet by mutableStateOf(Palet.DEFAULT)
        private set

    fun ganti(p: Palet) { palet = p }

    fun gantiByKey(ctx: Context, key: String) {
        palet = TemaRepo.load(ctx, key)
        ctx.getSharedPreferences("zx_tema", Context.MODE_PRIVATE)
            .edit().putString("key", key).apply()
    }

    fun inisialisasi(ctx: Context) {
        val key = ctx.getSharedPreferences("zx_tema", Context.MODE_PRIVATE)
            .getString("key", TemaRepo.DEFAULT_KEY) ?: TemaRepo.DEFAULT_KEY
        palet = TemaRepo.load(ctx, key)
    }

    fun keyAktif(ctx: Context): String =
        ctx.getSharedPreferences("zx_tema", Context.MODE_PRIVATE)
            .getString("key", TemaRepo.DEFAULT_KEY) ?: TemaRepo.DEFAULT_KEY
}

// Computed property — baca dari palet aktif. Nol perubahan di call site.
val Bg get() = WarnaAktif.palet.bg
val Kartu get() = WarnaAktif.palet.kartu
val Teks get() = WarnaAktif.palet.teks
val Muted get() = WarnaAktif.palet.muted
val MutedFg get() = WarnaAktif.palet.teksMuted
val Border get() = WarnaAktif.palet.border
val Biru get() = WarnaAktif.palet.biru
val BiruGelap get() = WarnaAktif.palet.biruGelap
val Ungu get() = WarnaAktif.palet.ungu
val Amber get() = WarnaAktif.palet.amber
val Hijau get() = WarnaAktif.palet.hijau
val Merah get() = WarnaAktif.palet.merah
val Pink get() = WarnaAktif.palet.pink
val Teal get() = WarnaAktif.palet.teal
val Oranye get() = WarnaAktif.palet.oranye
val OranyeBg get() = WarnaAktif.palet.oranyeBg
val OranyeGelap get() = WarnaAktif.palet.oranyeGelap

private val SkemaTerang = lightColorScheme(
    primary = Biru,
    secondary = Amber,
    tertiary = Ungu,
    background = Bg,
    surface = Kartu,
    onPrimary = Color.White,
    onSecondary = Color(0xFF92400E),
    onBackground = Teks,
    onSurface = Teks,
    error = Merah,
)

private val SkemaGelap = darkColorScheme(
    primary = Biru,
    secondary = Amber,
    tertiary = Ungu,
    background = Bg,
    surface = Kartu,
    onPrimary = Color.White,
    onSecondary = Color(0xFF78350F),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    error = Merah,
)

val Tipografi = Typography(
    titleLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
    bodySmall = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = MutedFg),
    labelSmall = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
)

val RadiusBesar = 20.dp
val RadiusKartu = 16.dp
val RadiusKecil = 12.dp

@Composable
fun TemaZX(content: @Composable () -> Unit) {
    val p = WarnaAktif.palet
    val gelap = hitungLuminansi(p.bg) < 0.5f
    val skema = if (gelap) SkemaGelap else SkemaTerang
    MaterialTheme(
        colorScheme = skema,
        typography = Tipografi,
        shapes = Shapes(
            small = RoundedCornerShape(RadiusKecil),
            medium = RoundedCornerShape(RadiusKartu),
            large = RoundedCornerShape(RadiusBesar),
        ),
        content = content,
    )
}

// ponytail: luminansi sederhana. Upgrade: copyFrom() Color.luminance() saat tersedia.
private fun hitungLuminansi(c: Color): Float {
    val r = c.red
    val g = c.green
    val b = c.blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
