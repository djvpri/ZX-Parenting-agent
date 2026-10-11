package com.zxparenting.agent

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// --- DTO ---

data class LoginAnakReq(
    val username: String,
    val pin: String,
)

data class AnakInfo(
    val id: String,
    val nama: String,
    val umur: Int,
    val ortuId: String,
)

data class LoginAnakRes(
    val token: String,
    val anak: AnakInfo?,
)

data class OfficialItem(
    val id: String,
    val tipe: String,
    val judul: String,
    val deskripsi: String?,
    @SerializedName("konten") val konten: List<Map<String, Any?>>?,
    @SerializedName("officialMeta") val meta: OfficialMeta?,
)

data class OfficialMeta(
    val kategori: String?,
    @SerializedName("rentangUmur") val rentangUmur: String?,
    @SerializedName("levelKesulitan") val levelKesulitan: Int?,
    @SerializedName("tokenReward") val tokenReward: Int,
    val premium: Boolean,
    val views: Int,
    @SerializedName("aiGenerated") val aiGenerated: Boolean,
)

data class OfficialFeed(
    val items: List<OfficialItem>,
    val total: Int,
)

data class TugasAnak(
    val id: String,
    val judul: String,
    val deskripsi: String?,
    val reward: Int,
    val status: String,
    val deadline: String?,
    val type: String? = null, // manual | ai_quiz | ai_scheduled
)

// --- Kuis ---

data class Soal(
    val q: String,
    val options: List<String>,
)

data class OfficialDetail(
    val id: String,
    val tipe: String,
    val judul: String,
    val deskripsi: String?,
    val konten: List<Soal>?,
    val kategori: String?,
    @SerializedName("tokenReward") val tokenReward: Int,
    val premium: Boolean,
    val selesai: Boolean,
)

data class CompleteReq(
    val jawaban: List<Int>,
)

data class CompleteRes(
    val ok: Boolean,
    val skor: Int?,
    @SerializedName("totalSoal") val totalSoal: Int?,
    @SerializedName("tokenDidapat") val tokenDidapat: Int,
)

// Kuis AI (Gemini on-demand)

data class KuisAIReq(
    val topik: String,
    @SerializedName("levelKesulitan") val levelKesulitan: Int,
    @SerializedName("jumlahSoal") val jumlahSoal: Int,
)

data class KuisAIRes(
    @SerializedName("attemptId") val attemptId: String,
    val soal: List<Soal>,
)

data class SubmitKuisReq(
    val jawaban: List<Int>,
)

data class HasilSoal(
    val q: String,
    @SerializedName("jawabAnak") val jawabAnak: Int,
    @SerializedName("jawabBenar") val jawabBenar: Int,
    val benar: Boolean,
    val explain: String,
)

data class SubmitKuisRes(
    val skor: Int,
    @SerializedName("totalSoal") val totalSoal: Int,
    @SerializedName("tokenDidapat") val tokenDidapat: Int,
    @SerializedName("challengeSelesai") val challengeSelesai: Boolean,
    val hasil: List<HasilSoal>,
)

// ============ KUIS SESSION (lock per-soal, shuffle, review, retry) ============

data class KuisSessionStartReq(
    @SerializedName("tugasId") val tugasId: String,
)

data class SoalSession(
    @SerializedName("posisi") val posisi: Int,
    val question: String,
    val options: List<String>,
)

data class KuisSessionStartRes(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("totalSoal") val totalSoal: Int,
    val soal: List<SoalSession>,
)

data class JawabanLocked(
    @SerializedName("posisi") val posisi: Int,
    @SerializedName("opsiDipilih") val opsiDipilih: Int,
)

data class KuisSessionSubmitReq(
    @SerializedName("sessionId") val sessionId: String,
    val jawaban: List<JawabanLocked>,
)

data class KuisSessionSubmitRes(
    val skor: Int,
    @SerializedName("benarCount") val benarCount: Int,
    @SerializedName("totalSoal") val totalSoal: Int,
    val token: Int,
)

data class ReviewSoal(
    @SerializedName("posisi") val posisi: Int,
    val question: String,
    val options: List<String>,
    @SerializedName("jawabanAnak") val jawabanAnak: Int?,
    @SerializedName("jawabanBenar") val jawabanBenar: Int,
    val benar: Boolean,
    val explanation: String,
)

data class KuisSessionReviewRes(
    @SerializedName("sessionId") val sessionId: String,
    val skor: Int?,
    @SerializedName("benarCount") val benarCount: Int?,
    @SerializedName("totalSoal") val totalSoal: Int?,
    @SerializedName("durasiDetik") val durasiDetik: Int?,
    val review: List<ReviewSoal>,
)

// --- Hadiah tab: saldo, tukar token, katalog, quest, challenge, streak, riwayat, pinjam ---

data class SaldoRes(val saldo: Int)

data class TukarTokenReq(val menit: Int)
data class TukarTokenRes(val ok: Boolean, val menit: Int, val saldo: Int)

data class HadiahItem(
    val id: String,
    val judul: String,
    val deskripsi: String?,
    @SerializedName("hargaCoin") val hargaCoin: Int,
    val stok: Int,
    @SerializedName("sudahMinta") val sudahMinta: Boolean,
)
data class KatalogRes(val katalog: List<HadiahItem>)
data class MintaHadiahReq(@SerializedName("hadiahId") val hadiahId: String)

data class QuestAnak(
    val id: String,
    val judul: String,
    val deskripsi: String?,
    @SerializedName("tokenReward") val tokenReward: Int,
    val deadline: String?,
    val anggota: List<QuestAnggota>?,
)
data class QuestAnggota(val anakId: String, val selesai: Boolean)

data class ChallengeRes(
    val id: String?,
    val judul: String?,
    val topik: String?,
    @SerializedName("tokenReward") val tokenReward: Int,
    val selesai: Boolean,
)

data class StreakRes(
    val current: Int,
    val terbaik: Int,
    val terakhir: String?,
)

data class LedgerItem(
    val id: String,
    val tipe: String,
    val jumlah: Int,
    val catatan: String?,
    val refId: String?,
    val createdAt: String,
)
data class RiwayatRes(val ledger: List<LedgerItem>)

data class PinjamRes(
    val hutang: Int,
    val riwayat: List<LedgerItem>?,
)
data class PinjamReq(val menit: Int)
data class PinjamPostRes(val ok: Boolean, val menit: Int, val id: String)

// --- Error reporting (fire-and-forget) ---

data class ErrorLogReq(
    val screen: String,
    val message: String,
    val extra: Map<String, Any?>? = null,
)

// --- Retrofit interface ---

interface ZxApi {
    @POST("auth/native-anak")
    suspend fun loginAnak(@Body req: LoginAnakReq): Response<LoginAnakRes>

    @GET("anak/official")
    suspend fun feedOfficial(@Header("Authorization") bearer: String): Response<OfficialFeed>

    @GET("anak/official/{id}")
    suspend fun officialDetail(@Header("Authorization") bearer: String, @retrofit2.http.Path("id") id: String): Response<OfficialDetail>

    @POST("anak/official/{id}/complete")
    suspend fun officialComplete(@Header("Authorization") bearer: String, @retrofit2.http.Path("id") id: String, @Body req: CompleteReq): Response<CompleteRes>

    @GET("anak/tugas")
    suspend fun tugasList(@Header("Authorization") bearer: String): Response<List<TugasAnak>>

    @POST("anak/kuis/start")
    suspend fun kuisStart(@Header("Authorization") bearer: String, @Body req: KuisAIReq): Response<KuisAIRes>

    @POST("anak/kuis/{id}/submit")
    suspend fun kuisSubmit(@Header("Authorization") bearer: String, @retrofit2.http.Path("id") id: String, @Body req: SubmitKuisReq): Response<SubmitKuisRes>

    // ============ KUIS SESSION (lock per-soal, shuffle, review) ============

    @POST("kuis-session")
    suspend fun kuisSessionStart(
        @Header("Authorization") bearer: String,
        @Body req: KuisSessionStartReq,
    ): Response<KuisSessionStartRes>

    @POST("kuis-session/submit")
    suspend fun kuisSessionSubmit(
        @Header("Authorization") bearer: String,
        @Body req: KuisSessionSubmitReq,
    ): Response<KuisSessionSubmitRes>

    @GET("kuis-session/review")
    suspend fun kuisSessionReview(
        @Header("Authorization") bearer: String,
        @Query("sessionId") sessionId: String,
    ): Response<KuisSessionReviewRes>

    // --- Hadiah tab ---

    @GET("anak/saldo")
    suspend fun saldo(@Header("Authorization") bearer: String): Response<SaldoRes>

    @POST("anak/sesi-main")
    suspend fun tukarToken(@Header("Authorization") bearer: String, @Body req: TukarTokenReq): Response<TukarTokenRes>

    @GET("anak/hadiah")
    suspend fun katalogHadiah(@Header("Authorization") bearer: String): Response<KatalogRes>

    @POST("anak/hadiah")
    suspend fun mintaHadiah(@Header("Authorization") bearer: String, @Body req: MintaHadiahReq): Response<Map<String, Any>>

    @GET("quests-saya")
    suspend fun questsSaya(@Header("Authorization") bearer: String): Response<List<QuestAnak>>

    @GET("anak/challenge")
    suspend fun challenge(@Header("Authorization") bearer: String): Response<ChallengeRes>

    @GET("anak/streak")
    suspend fun streak(@Header("Authorization") bearer: String): Response<StreakRes>

    @GET("anak/riwayat-token")
    suspend fun riwayatToken(@Header("Authorization") bearer: String): Response<RiwayatRes>

    @GET("anak/pinjam")
    suspend fun pinjamStatus(@Header("Authorization") bearer: String): Response<PinjamRes>

    @POST("anak/pinjam")
    suspend fun pinjamAjukan(@Header("Authorization") bearer: String, @Body req: PinjamReq): Response<PinjamPostRes>

    @POST("log-error")
    suspend fun logError(@Header("Authorization") bearer: String, @Body req: ErrorLogReq): Response<Map<String, Any>>
}

// --- Singleton client ---

object Klien {
    private fun baseUrl(): String {
        // BuildConfig.API = ".../api/agent" → potong ke ".../api/"
        val api = com.zxparenting.agent.BuildConfig.API
        return if (api.endsWith("/agent")) api.removeSuffix("/agent") + "/" else api
    }

    private val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    private val okhttp = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val api: ZxApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ZxApi::class.java)
    }
}
