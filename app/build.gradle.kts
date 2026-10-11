import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
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
    compileSdk = 36

    defaultConfig {
        applicationId = bacaAlamat("idPaket")
        minSdk = 24
        targetSdk = 36
        versionCode = bacaAlamat("versiKode").toInt()
        versionName = bacaAlamat("versiNama")
        buildConfigField("String", "API", "\"${bacaAlamat("api")}\"")
        buildConfigField("String", "BERANDA", "\"${bacaAlamat("beranda")}\"")
        buildConfigField("String", "REPO_API",
            "\"https://api.github.com/repos/${bacaAlamat("repoRilis")}/releases/latest\"")
        buildConfigField("String", "VERSI_NAMA", "\"${bacaAlamat("versiNama")}\"")
        buildConfigField("int", "VERSI_KODE", bacaAlamat("versiKode"))
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

    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.android.material:material:1.12.0")
}
