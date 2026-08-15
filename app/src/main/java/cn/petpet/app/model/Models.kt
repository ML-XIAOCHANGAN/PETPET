package cn.petpet.app.model

data class PetProfile(
    val name: String,
    val affection: Int,
    val adoptedAt: String,
    val breedId: String = "black_cat",
)

data class DailyProgress(
    val chatCount: Int = 0,
    val chatRewarded: Boolean = false,
    val interactionCount: Int = 0,
)

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val time: String,
)

data class CommunityComment(
    val id: Long,
    val author: String,
    val text: String,
)

data class CommunityPost(
    val id: Long,
    val author: String,
    val text: String,
    val time: String,
    val liked: Boolean = false,
    val likeCount: Int = 0,
    val comments: List<CommunityComment> = emptyList(),
    val collected: Boolean = false,
    val disliked: Boolean = false,
)

enum class HomeSection(val title: String, val symbol: String) {
    MY_PET("My Pet", "猫"),
    GROWTH("成长", "记"),
    COMMUNITY("社区", "友"),
}

data class DiaryEntry(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: String,
)

data class PetPetUiState(
    val pet: PetProfile? = null,
    val dailyProgress: DailyProgress = DailyProgress(),
    val messages: List<ChatMessage> = emptyList(),
    val posts: List<CommunityPost> = emptyList(),
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val darkTheme: Boolean = false,
    val greeting: String = "今天也一起慢慢生活吧。",
    val section: HomeSection = HomeSection.MY_PET,
    val notice: String? = null,
    val petTyping: Boolean = false,
    val communityOnline: Boolean = false,
    /** 对话弹窗：(消息id, 文本)，UI 显示 5 秒后自动清除。 */
    val chatBubble: Pair<Long, String>? = null,
    /** 领养流程：无宠物时显示抽卡+介绍+命名。 */
    val showAdoption: Boolean = false,
    val candidateBreed: PetBreed? = null,
    /** 对话模型配置（设置页可改）。 */
    val chatProvider: ChatProvider = ChatProvider.ZHIPU,
    val chatApiKey: String = "",
)
