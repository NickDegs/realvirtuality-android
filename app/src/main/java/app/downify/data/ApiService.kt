package app.downify.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ApiService(private val tokenStorage: TokenStorage) {

    private val baseUrl = "https://realvirtuality.app/downify-api"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private suspend fun request(
        path: String,
        method: String = "GET",
        body: String? = null
    ): String = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl$path")
        val conn = url.openConnection() as HttpsURLConnection
        try {
            conn.requestMethod = method
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            tokenStorage.loadToken()?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000

            if (body != null) {
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(body) }
            }

            val code = conn.responseCode
            val responseText = runCatching {
                if (code < 400) conn.inputStream.bufferedReader().readText()
                else conn.errorStream?.bufferedReader()?.readText() ?: ""
            }.getOrDefault("")

            when (code) {
                401 -> throw ApiException.Unauthorized
                in 400..599 -> {
                    val detail = runCatching { json.decodeFromString<ErrorResponse>(responseText).detail }
                        .getOrDefault("Sunucu hatası ($code)")
                    throw ApiException.ServerError(detail)
                }
            }
            responseText
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException.NetworkError(e)
        } finally {
            conn.disconnect()
        }
    }

    // MARK: Auth

    suspend fun login(email: String, password: String): AuthResponse {
        val body = """{"email":"${email.sanitize()}","password":"${password.sanitize()}"}"""
        return json.decodeFromString(request("/auth/login", "POST", body))
    }

    suspend fun register(email: String, username: String, password: String): AuthResponse {
        val body = """{"email":"${email.sanitize()}","username":"${username.sanitize()}","password":"${password.sanitize()}"}"""
        return json.decodeFromString(request("/auth/register", "POST", body))
    }

    suspend fun guestLogin(): AuthResponse =
        json.decodeFromString(request("/auth/guest", "POST", "{}"))

    suspend fun getMe(): User = json.decodeFromString(request("/auth/me"))

    suspend fun deleteAccount() {
        request("/auth/account", "DELETE")
    }

    /** Verifies a Google Play purchase server-side and syncs the user's tier. */
    suspend fun recordPlayPurchase(productId: String, purchaseToken: String): User {
        val body = """{"product_id":"${productId.sanitize()}","purchase_token":"${purchaseToken.sanitize()}"}"""
        return json.decodeFromString(request("/subscription/google-play", "POST", body))
    }

    // MARK: Download

    suspend fun startDownload(
        url: String,
        quality: String = "best",
        audioOnly: Boolean = false,
        noWatermark: Boolean = true
    ): DownloadResponse {
        val body = """{"url":"${url.sanitize()}","quality":"$quality","audio_only":$audioOnly,"no_watermark":$noWatermark}"""
        return json.decodeFromString(request("/download/start", "POST", body))
    }

    suspend fun getDownloadStatus(taskId: String): DownloadStatus =
        json.decodeFromString(request("/download/status/$taskId"))

    // MARK: Subscription

    suspend fun getCheckoutUrl(plan: String): String {
        val body = """{"plan":"$plan"}"""
        val raw = request("/subscription/checkout", "POST", body)
        val obj = json.decodeFromString<kotlinx.serialization.json.JsonObject>(raw)
        return obj["checkout_url"]?.toString()?.trim('"') ?: throw ApiException.ServerError("URL alınamadı")
    }

    // MARK: Bulk Download

    suspend fun fetchBulkItems(url: String, limit: Int = 50): BulkDownloadListResponse {
        val body = """{"url":"${url.sanitize()}","limit":$limit}"""
        return json.decodeFromString(request("/download/bulk/list", "POST", body))
    }

    suspend fun startBulkDownload(bulkId: String, itemIds: List<String>): BulkStartResponse {
        val ids = itemIds.joinToString(",") { "\"$it\"" }
        val body = """{"bulk_id":"$bulkId","item_ids":[$ids]}"""
        return json.decodeFromString(request("/download/bulk/start", "POST", body))
    }

    // MARK: Auto Download

    suspend fun getAutoSubscriptions(): List<AutoSubscription> =
        json.decodeFromString(request("/download/auto/list"))

    suspend fun addAutoSubscription(url: String, frequency: String): AutoSubscription {
        val body = """{"url":"${url.sanitize()}","frequency":"$frequency"}"""
        return json.decodeFromString(request("/download/auto/subscribe", "POST", body))
    }

    suspend fun deleteAutoSubscription(id: String) {
        request("/download/auto/$id", "DELETE")
    }

    // MARK: Gallery / History

    suspend fun getDownloadHistory(page: Int = 1): List<DownloadHistoryItem> =
        json.decodeFromString(request("/download/history?page=$page"))

    // MARK: Clip / GIF

    suspend fun startClip(
        url: String,
        startTime: Double,
        endTime: Double,
        asGif: Boolean = false,
        quality: String? = null
    ): DownloadResponse {
        val q = if (quality != null) ""","quality":"$quality"""" else ""
        val body = """{"url":"${url.sanitize()}","start_time":$startTime,"end_time":$endTime,"as_gif":$asGif$q}"""
        return json.decodeFromString(request("/download/clip", "POST", body))
    }

    // MARK: Subtitles

    suspend fun getSubtitleTracks(url: String): List<SubtitleTrack> {
        val body = """{"url":"${url.sanitize()}"}"""
        return json.decodeFromString(request("/download/subtitles/tracks", "POST", body))
    }

    suspend fun startSubtitleDownload(url: String, language: String, embed: Boolean): DownloadResponse {
        val body = """{"url":"${url.sanitize()}","language":"$language","embed":$embed}"""
        return json.decodeFromString(request("/download/subtitles/start", "POST", body))
    }

    // MARK: Key Moments

    suspend fun getVideoInfo(url: String): VideoInfo {
        val body = """{"url":"${url.sanitize()}"}"""
        return json.decodeFromString(request("/download/info", "POST", body))
    }

    suspend fun startChapterDownload(url: String, chapterIds: List<String>): BulkStartResponse {
        val ids = chapterIds.joinToString(",") { "\"$it\"" }
        val body = """{"url":"${url.sanitize()}","chapter_ids":[$ids]}"""
        return json.decodeFromString(request("/download/chapters", "POST", body))
    }

    // MARK: Scheduled

    suspend fun getScheduledDownloads(): List<ScheduledDownload> =
        json.decodeFromString(request("/download/scheduled/list"))

    suspend fun scheduleDownload(url: String, scheduledAt: String, quality: String): ScheduledDownload {
        val body = """{"url":"${url.sanitize()}","scheduled_at":"$scheduledAt","quality":"$quality"}"""
        return json.decodeFromString(request("/download/scheduled/add", "POST", body))
    }

    suspend fun deleteScheduledDownload(id: String) {
        request("/download/scheduled/$id", "DELETE")
    }

    private fun String.sanitize() = replace("\\", "\\\\").replace("\"", "\\\"")
}
