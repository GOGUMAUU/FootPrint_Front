data class Guestbook(
    val guestbookId: Long,
    val userId: Long,
    val content: String?,
    val latitude: Double,
    val longitude: Double,
    val visibility: String?,
    val locationName: String?,
    val createdAt: String,
    val updatedAt: String?
)