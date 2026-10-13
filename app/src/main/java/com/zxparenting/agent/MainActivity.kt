package com.zxparenting.agent

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.zxparenting.agent.tema.WarnaAktif
import com.zxparenting.agent.tema.TemaZX

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        // Tangkap crash tak terduga → kirim ke backend sebelum app mati.
        val ctx = applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            LaporError.kirim(ctx, "MainActivity", e.javaClass.simpleName + ": " + e.message)
            prev?.uncaughtException(t, e)
        }
        enableEdgeToEdge()

        // Inisialisasi context global utk Simpanan
        Ctx.app = applicationContext
        val ctx = applicationContext

        // Inisialisasi bahasa tersimpan
        ManajerBahasa.inisialisasi(ctx)

        // Inisialisasi tema tersimpan
        WarnaAktif.inisialisasi(ctx)

        // Fase 4: jadwalkan pengingat harian (challenge 09:00 + streak 20:00)
        val jwt = Simpanan.jwt(ctx)
        val demo = Simpanan.isDemo(ctx)
        if (jwt != null || demo) Pengingat.jadwalkan(ctx)

        setContent {
            TemaZX {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { },
                ) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        val jwt = Simpanan.jwt(ctx)
                        val demo = Simpanan.isDemo(ctx)
                        if (jwt == null && !demo) {
                            Pengingat.batalkan(ctx)
                            LayarMasuk(onMasuk = { recreate() }, onDemo = {
                                Simpanan.setDemo(ctx)
                                Pengingat.jadwalkan(ctx)
                                recreate()
                            })
                        } else {
                            LayarUtama(jwt = jwt, onKeluar = {
                                Simpanan.hapusAnak(ctx)
                                Pengingat.batalkan(ctx)
                                recreate()
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayarUtama(jwt: String?, onKeluar: () -> Unit) {
    val strAnak = stringResource(R.string.nav_child)
    var tab by remember { mutableIntStateOf(0) }
    var kuisId by remember { mutableStateOf<String?>(null) }
    var tugasKuisId by remember { mutableStateOf<String?>(null) }
    var topikPrefill by remember { mutableStateOf("") }
    val nama = remember { Simpanan.namaAnak(Ctx.get()) ?: strAnak }
    val username = remember { Simpanan.usernameAnak(Ctx.get()) ?: "" }
    val umur = remember { Simpanan.umurAnak(Ctx.get()) }

    // Full-screen kuis detail ketika kuisId dipilih
    if (kuisId != null) {
        LayarKuisDetail(jwt = jwt ?: "", kuisId = kuisId!!) { kuisId = null }
        return
    }

    // Full-screen kuis session ketika tugasKuisId dipilih
    if (tugasKuisId != null) {
        LayarKuisSession(jwt = jwt ?: "", tugasId = tugasKuisId!!) { tugasKuisId = null }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavItem.entries.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (tab) {
                0 -> LayarBeranda(jwt, nama, umur, username = username, onBukaKuis = { kuisId = it }, onKlikTugas = { tugasKuisId = it.id }, onKlikChallenge = { topikPrefill = it; tab = 1 })
                1 -> LayarKuisAI(jwt = jwt ?: "", onKembali = { tab = 0 }, topikAwal = topikPrefill)
                2 -> LayarTugas(jwt, onKlikTugas = { t -> tugasKuisId = t.id })
                3 -> LayarHadiah(jwt = jwt)
                4 -> LayarSaya(nama, umur, onKeluar)
            }
        }
    }
}

enum class NavItem(@androidx.annotation.StringRes val labelRes: Int, val icon: ImageVector) {
    Beranda(R.string.nav_home, Icons.Default.Home),
    Kuis(R.string.quiz_ai, Icons.Default.AutoAwesome),
    Tugas(R.string.task_title, Icons.Default.Task),
    Hadiah(R.string.nav_hadiah, Icons.Default.CardGiftcard),
    Saya(R.string.nav_profile, Icons.Default.Person),
}
