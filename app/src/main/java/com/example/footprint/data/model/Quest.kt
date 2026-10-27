data class Quest(
    val questId: Long,
    val title: String,
    val description: String?,
    val regionName: String,
    val targetLatitude: Double,
    val targetLongitude: Double,
    val targetRadius: Int,
    val badgeImageUrl: String?,
    val createdAt: String
)