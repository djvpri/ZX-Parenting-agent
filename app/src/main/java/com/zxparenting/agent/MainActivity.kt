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

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        enableEdgeToEdge()

        // Inisialisasi context global utk Simpanan
        Ctx.app = applicationContext
        val ctx = applicationContext

        // Inisialisasi bahasa tersimpan
        ManajerBahasa.inisialisasi(ctx)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { },
                ) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        val jwt = Simpanan.jwt(ctx)
                        if (jwt == null) {
                            LayarMasuk(onMasuk = { recreate() })
                        } else {
                            LayarUtama(jwt = jwt, onKeluar = {
                                Simpanan.hapusAnak(ctx)
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
private fun LayarUtama(jwt: String, onKeluar: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var kuisId by remember { mutableStateOf<String?>(null) }
    var tugasKuisId by remember { mutableStateOf<String?>(null) }
    val nama = remember { Simpanan.namaAnak(Ctx.get()) ?: stringResource(R.string.nav_child) }
    val umur = remember { Simpanan.umurAnak(Ctx.get()) }

    // Full-screen kuis detail ketika kuisId dipilih
    if (kuisId != null) {
        LayarKuisDetail(jwt = jwt, kuisId = kuisId!!) { kuisId = null }
        return
    }

    // Full-screen kuis session ketika tugasKuisId dipilih
    if (tugasKuisId != null) {
        LayarKuisSession(jwt = jwt, tugasId = tugasKuisId!!) { tugasKuisId = null }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavItem.entries.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (tab) {
                0 -> LayarBeranda(jwt, onBukaKuis = { kuisId = it })
                1 -> LayarKuisAI(jwt = jwt, onKembali = { tab = 0 })
                2 -> LayarTugas(jwt, onKlikTugas = { t -> tugasKuisId = t.id })
                3 -> LayarSaya(nama, umur, onKeluar)
            }
        }
    }
}

enum class NavItem(val label: String, val icon: ImageVector) {
    Beranda(stringResource(R.string.nav_home), Icons.Default.Home),
    Kuis(stringResource(R.string.quiz_ai), Icons.Default.AutoAwesome),
    Tugas(stringResource(R.string.task_title), Icons.Default.Task),
    Saya(stringResource(R.string.nav_profile), Icons.Default.Person),
}
