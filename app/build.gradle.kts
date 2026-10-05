import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Satu sumber kebenaran: alamat.json (pola Z-Rooms-android).
fun bacaAlamat(kunci: String): String {
    val teks = file("../alamat.json").readText()
    val cocok = Regex("\"$kunci\"\\s*:\\s*\"?([^\",\\s}]+)\"?").find(teks)
    return cocok?.groupValues?.get(1)
        ?: error("alamat.json: kolom \"$kunci\" tidak ditemukan")
}

val sifatKeystore = Properties().apply {
    val f = file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

android {
    namespace = "com.zxparenting.agent"
    compileSdk = 35

    defaultConfig {
        applicationId = bacaAlamat("idPaket")
        minSdk = 24
        targetSdk = 35
        versionCode = bacaAlamat("versiKode").toInt()
        versionName = bacaAlamat("versiNama")
        buildConfigField("String", "API", "\"${bacaAlamat("api")}\"")
    }

    signingConfigs {
        if (sifatKeystore.isNotEmpty()) {
            create("rilis") {
                storeFile = file(sifatKeystore.getProperty("storeFile"))
                storePassword = sifatKeystore.getProperty("storePassword")
                keyAlias = sifatKeystore.getProperty("keyAlias")
                keyPassword = sifatKeystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (sifatKeystore.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("rilis")
            }
        }
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // ponytail: tanpa deps jaringan — HttpURLConnection stdlib cukup utk ping JSON.
    // Tambah Retrofit/OkHttp saat API agent makin rumit.
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
