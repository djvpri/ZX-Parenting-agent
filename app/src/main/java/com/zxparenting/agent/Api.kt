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
)

// --- Retrofit interface ---

interface ZxApi {
    @POST("auth/native-anak")
    suspend fun loginAnak(@Body req: LoginAnakReq): Response<LoginAnakRes>

    @GET("anak/official")
    suspend fun feedOfficial(@Header("Authorization") bearer: String): Response<OfficialFeed>

    @GET("anak/tugas")
    suspend fun tugasList(@Header("Authorization") bearer: String): Response<List<TugasAnak>>
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
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
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
