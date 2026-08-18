package cn.petpet.app.data

import android.content.Context
import android.util.Log
import cn.petpet.app.domain.AffectionRules
import cn.petpet.app.model.CommunityComment
import cn.petpet.app.model.CommunityPost
import cn.petpet.app.model.DailyProgress
import cn.petpet.app.model.PetProfile
import cn.petpet.app.model.Temperament
import java.time.format.DateTimeFormatter

data class LocalSnapshot(
    val pet: PetProfile?,
    val dailyProgress: DailyProgress,
    val posts: List<CommunityPost>,
    val storageWarning: String? = null,
)

private data class PostsReadResult(
    val posts: List<CommunityPost>,
    val storageWarning: String? = null,
)

class PetPetRepository(
    context: Context,
    private val runtimeValues: RuntimeValues = ApplicationRuntimeValues.instance,
) {
    private val preferences = context.getSharedPreferences("petpet_local_data", Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    init {
        resetDailyProgressIfNeeded()
        seedCommunityIfNeeded()
    }

    fun snapshot(): LocalSnapshot {
        return snapshot(storageWarning = null)
    }

    private fun snapshot(storageWarning: String?): LocalSnapshot {
        resetDailyProgressIfNeeded()
        val postsResult = readPosts()
        val petName = preferences.getString(KEY_PET_NAME, null)
        val pet = petName?.let {
            PetProfile(
                name = it,
                affection = preferences.getInt(KEY_AFFECTION, 0).coerceAtLeast(0),
                adoptedAt = preferences.getString(KEY_ADOPTED_AT, "") ?: "",
                breedId = preferences.getString(KEY_PET_BREED, "black_cat") ?: "black_cat",
                temperament = Temperament.byName(preferences.getString(KEY_PET_TEMPERAMENT, null)),
            )
        }
        return LocalSnapshot(
            pet = pet,
            dailyProgress = DailyProgress(
                chatCount = preferences.getInt(KEY_CHAT_COUNT, 0)
                    .coerceIn(0, AffectionRules.CHAT_TARGET),
                chatRewarded = preferences.getBoolean(KEY_CHAT_REWARDED, false),
                interactionCount = preferences.getInt(KEY_INTERACTION_COUNT, 0)
                    .coerceIn(0, AffectionRules.INTERACTION_DAILY_LIMIT),
            ),
            posts = postsResult.posts,
            storageWarning = storageWarning ?: postsResult.storageWarning,
        )
    }

    fun adoptPet(name: String, breedId: String = "black_cat", temperament: Temperament = Temperament.random()): LocalSnapshot {
        if (!preferences.getString(KEY_PET_NAME, null).isNullOrBlank()) {
            return snapshot()
        }
        preferences.edit()
            .putString(KEY_PET_NAME, name.trim())
            .putString(KEY_PET_BREED, breedId)
            .putString(KEY_PET_TEMPERAMENT, temperament.name)
            .putInt(KEY_AFFECTION, 0)
            .putString(KEY_ADOPTED_AT, runtimeValues.today().format(dateFormatter))
            .apply()
        return snapshot()
    }

    fun interact(): Pair<LocalSnapshot, Int> {
        resetDailyProgressIfNeeded()
        val currentCount = preferences.getInt(KEY_INTERACTION_COUNT, 0)
        val result = AffectionRules.afterInteraction(currentCount)
        val affection = preferences.getInt(KEY_AFFECTION, 0) + result.gained
        preferences.edit()
            .putInt(KEY_INTERACTION_COUNT, result.nextCount)
            .putInt(KEY_AFFECTION, affection)
            .apply()
        return snapshot() to result.gained
    }

    fun recordValidChat(): Pair<LocalSnapshot, Int> {
        resetDailyProgressIfNeeded()
        val result = AffectionRules.afterValidChat(
            validCount = preferences.getInt(KEY_CHAT_COUNT, 0),
            alreadyRewarded = preferences.getBoolean(KEY_CHAT_REWARDED, false),
        )
        val affection = preferences.getInt(KEY_AFFECTION, 0) + result.gained
        preferences.edit()
            .putInt(KEY_CHAT_COUNT, result.nextCount)
            .putBoolean(KEY_CHAT_REWARDED, result.rewarded)
            .putInt(KEY_AFFECTION, affection)
            .apply()
        return snapshot() to result.gained
    }

    fun createPost(text: String, images: List<String> = emptyList(), author: String = "我和我的小猫"): LocalSnapshot {
        val readResult = readPosts()
        val posts = readResult.posts.toMutableList()
        posts.add(
            index = 0,
            element = CommunityPost(
                id = runtimeValues.nextId(),
                author = author,
                text = text.trim(),
                time = runtimeValues.now().format(dateTimeFormatter),
                images = images,
            ),
        )
        writePosts(posts)
        return snapshot(readResult.storageWarning)
    }

    fun toggleLike(postId: Long): LocalSnapshot {
        val readResult = readPosts()
        val posts = readResult.posts.map { post ->
            if (post.id != postId) {
                post
            } else {
                post.copy(
                    liked = !post.liked,
                    likeCount = (post.likeCount + if (post.liked) -1 else 1).coerceAtLeast(0),
                )
            }
        }
        writePosts(posts)
        return snapshot(readResult.storageWarning)
    }

    /** 收藏 / 取消收藏（三点菜单，本地持久化）。 */
    fun toggleCollect(postId: Long): LocalSnapshot {
        val readResult = readPosts()
        val posts = readResult.posts.map { post ->
            if (post.id != postId) post else post.copy(collected = !post.collected)
        }
        writePosts(posts)
        return snapshot(readResult.storageWarning)
    }

    /** 不喜欢 / 取消不喜欢（三点菜单，本地持久化）。 */
    fun toggleDislike(postId: Long): LocalSnapshot {
        val readResult = readPosts()
        val posts = readResult.posts.map { post ->
            if (post.id != postId) post else post.copy(disliked = !post.disliked)
        }
        writePosts(posts)
        return snapshot(readResult.storageWarning)
    }

    /** 删除帖子进垃圾桶：正文与图片不再展示，仅保留头像/名字/标题（本地持久化）。 */
    fun deletePost(postId: Long): LocalSnapshot {
        val readResult = readPosts()
        val posts = readResult.posts.map { post ->
            if (post.id != postId) post else post.copy(inTrash = true)
        }
        writePosts(posts)
        return snapshot(readResult.storageWarning)
    }

    fun addComment(postId: Long, text: String): LocalSnapshot {
        val readResult = readPosts()
        val posts = readResult.posts.map { post ->
            if (post.id != postId) {
                post
            } else {
                post.copy(
                    comments = post.comments + CommunityComment(
                        id = runtimeValues.nextId(),
                        author = "我",
                        text = text.trim(),
                    ),
                )
            }
        }
        writePosts(posts)
        return snapshot(readResult.storageWarning)
    }

    /** 合并在线拉取的动态进本地存储（按 id 去重，插到最前）。 */
    fun mergeRemotePosts(remotePosts: List<CommunityPost>): LocalSnapshot {
        val readResult = readPosts()
        val existingIds = readResult.posts.map { it.id }.toSet()
        val fresh = remotePosts.filter { it.id !in existingIds }
        if (fresh.isEmpty()) return snapshot(readResult.storageWarning)
        val merged = (fresh + readResult.posts)
        writePosts(merged)
        return snapshot(readResult.storageWarning)
    }

    private fun resetDailyProgressIfNeeded() {
        val today = runtimeValues.today().format(dateFormatter)
        if (preferences.getString(KEY_PROGRESS_DATE, null) == today) return
        preferences.edit()
            .putString(KEY_PROGRESS_DATE, today)
            .putInt(KEY_CHAT_COUNT, 0)
            .putBoolean(KEY_CHAT_REWARDED, false)
            .putInt(KEY_INTERACTION_COUNT, 0)
            .apply()
    }

    private fun seedCommunityIfNeeded() {
        if (preferences.contains(KEY_POSTS)) return
        val initialPosts = listOf(
            CommunityPost(
                id = 2,
                author = "布丁的主人",
                text = "今天和布丁聊了好久。原来每天停下来陪陪它，也是在照顾自己的心情。",
                time = "今天 09:30",
                likeCount = 12,
                comments = listOf(CommunityComment(21, "一颗鱼丸", "好温柔的陪伴。")),
            ),
            CommunityPost(
                id = 1,
                author = "小满",
                text = "刚刚领养了我的第一只小猫，给它取名叫云朵，欢迎来认识它！",
                time = "昨天 21:18",
                likeCount = 8,
            ),
        )
        writePosts(initialPosts)
    }

    private fun readPosts(): PostsReadResult {
        val raw = preferences.getString(KEY_POSTS, "[]") ?: "[]"
        val decoded = PersistenceCodecs.decodePosts(raw)
        if (!decoded.isCorrupted) return PostsReadResult(decoded.value)

        val backupSucceeded = preferences.edit()
            .putString(KEY_POSTS_CORRUPTED_BACKUP, decoded.corruptedRaw)
            .commit()
        Log.e(TAG, "社区本地数据无法解析，损坏内容备份结果：$backupSucceeded")
        val warning = if (backupSucceeded) {
            "社区本地数据读取失败，原始内容已备份"
        } else {
            "社区本地数据读取失败，且原始内容备份失败"
        }
        return PostsReadResult(emptyList(), warning)
    }

    private fun writePosts(posts: List<CommunityPost>) {
        preferences.edit()
            .putString(KEY_POSTS, PersistenceCodecs.encodePosts(posts))
            .apply()
    }

    private companion object {
        const val TAG = "PetPetRepository"
        const val KEY_PET_NAME = "pet_name"
        const val KEY_PET_BREED = "pet_breed"
        const val KEY_PET_TEMPERAMENT = "pet_temperament"
        const val KEY_AFFECTION = "affection"
        const val KEY_ADOPTED_AT = "adopted_at"
        const val KEY_PROGRESS_DATE = "progress_date"
        const val KEY_CHAT_COUNT = "chat_count"
        const val KEY_CHAT_REWARDED = "chat_rewarded"
        const val KEY_INTERACTION_COUNT = "interaction_count"
        const val KEY_POSTS = "community_posts"
        const val KEY_POSTS_CORRUPTED_BACKUP = "community_posts_corrupted_backup"
    }
}
