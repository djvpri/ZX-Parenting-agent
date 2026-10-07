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

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        enableEdgeToEdge()

        // Inisialisasi context global utk Simpanan
        Ctx.app = applicationContext
        val ctx = applicationContext

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
    val nama = remember { Simpanan.namaAnak(Ctx.get()) ?: "Anak" }
    val umur = remember { Simpanan.umurAnak(Ctx.get()) }

    // Full-screen kuis detail ketika kuisId dipilih
    if (kuisId != null) {
        LayarKuisDetail(jwt = jwt, kuisId = kuisId!!) { kuisId = null }
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
                2 -> LayarTugas(jwt)
                3 -> LayarSaya(nama, umur, onKeluar)
            }
        }
    }
}

enum class NavItem(val label: String, val icon: ImageVector) {
    Beranda("Beranda", Icons.Default.Home),
    Kuis("Kuis AI", Icons.Default.AutoAwesome),
    Tugas("Tugas", Icons.Default.Task),
    Saya("Saya", Icons.Default.Person),
}
