data class User(
    val userId: Long,
    val email: String,
    val nickname: String,
    val profileImageUrl: String?,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val guestbookCount: Int = 0,
    val createdAt: String
)