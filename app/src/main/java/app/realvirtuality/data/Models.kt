package app.realvirtuality.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SubscriptionTier(val value: String) {
    FREE("free"),
    AD_FREE("ad_free"),
    FULL("full");

    companion object {
        fun from(value: String) = entries.firstOrNull { it.value == value } ?: FREE
    }
}

@Serializable
data class User(
    val id: Int,
    val email: String,
    val username: String,
    val tier: String,
    @SerialName("created_at") val createdAt: String = ""
) {
    val subscriptionTier: SubscriptionTier get() = SubscriptionTier.from(tier)
}

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    val user: User
)

@Serializable
data class DownloadResponse(
    @SerialName("task_id") val taskId: String,
    val status: String,
    val message: String? = null
)

@Serializable
data class DownloadStatus(
    @SerialName("task_id") val taskId: String,
    val status: String,
    val progress: Double? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    val filename: String? = null,
    val error: String? = null
)

@Serializable
data class ErrorResponse(val detail: String = "Sunucu hatası")

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val price: String,
    val period: String,
    val features: List<String>,
    val tier: SubscriptionTier
)

sealed class ApiException : Exception() {
    object Unauthorized : ApiException()
    data class ServerError(val msg: String) : ApiException()
    data class NetworkError(val cause: Throwable) : ApiException()
}

// MARK: - Bulk Download

@Serializable
data class BulkItem(
    val id: String,
    val url: String,
    val title: String? = null,
    val thumbnail: String? = null
)

@Serializable
data class BulkDownloadListResponse(
    @SerialName("bulk_id") val bulkId: String,
    val items: List<BulkItem>,
    val total: Int
)

@Serializable
data class BulkStartResponse(
    @SerialName("task_ids") val taskIds: List<String>
)

// MARK: - Auto Download

@Serializable
data class AutoSubscription(
    val id: String,
    val url: String,
    val title: String? = null,
    val frequency: String,
    val active: Boolean,
    @SerialName("last_checked") val lastChecked: String? = null,
    @SerialName("download_count") val downloadCount: Int = 0
)

// MARK: - Clip / GIF

@Serializable
data class ClipRequest(
    val url: String,
    @SerialName("start_time") val startTime: Double,
    @SerialName("end_time") val endTime: Double,
    @SerialName("as_gif") val asGif: Boolean = false,
    val quality: String? = null
)

// MARK: - Subtitles

@Serializable
data class SubtitleTrack(
    val id: String,
    val language: String,
    @SerialName("language_name") val languageName: String,
    val format: String
)

// MARK: - Video Info / Key Moments

@Serializable
data class VideoChapter(
    val id: String,
    val title: String,
    @SerialName("start_time") val startTime: Double,
    @SerialName("end_time") val endTime: Double,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null
)

@Serializable
data class VideoInfo(
    val title: String,
    val duration: Double,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val platform: String? = null,
    val chapters: List<VideoChapter>,
    @SerialName("has_ai_chapters") val hasAiChapters: Boolean = false
)

// MARK: - Scheduled Download

@Serializable
data class ScheduledDownload(
    val id: String,
    val url: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    val quality: String,
    val status: String,
    val title: String? = null
)

// MARK: - Gallery

@Serializable
data class DownloadHistoryItem(
    val id: String,
    val url: String,
    val filename: String,
    @SerialName("download_url") val downloadUrl: String,
    val platform: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("completed_at") val completedAt: String,
    @SerialName("file_size") val fileSize: Long? = null
)
