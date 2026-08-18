package cn.petpet.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.petpet.app.data.ApplicationRuntimeValues
import cn.petpet.app.data.ChatGatewayClient
import cn.petpet.app.data.LocalSnapshot
import cn.petpet.app.data.PetPetPreferencesRepository
import cn.petpet.app.data.PetPetRepository
import cn.petpet.app.model.ChatMessage
import cn.petpet.app.model.ChatProvider
import cn.petpet.app.model.CommunityPost
import cn.petpet.app.model.HomeSection
import cn.petpet.app.model.PetBreeds
import cn.petpet.app.model.PetPetUiState
import cn.petpet.app.model.PetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class PetPetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PetPetRepository(application)
    private val preferencesRepository = PetPetPreferencesRepository(application)
    private val gatewayClient = ChatGatewayClient()
    private val runtimeValues = ApplicationRuntimeValues.instance
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private var lastDiaryStorageWarning: String? = null

    /** 是否狗狗（按品种判断，无宠物默认猫）。 */
    private fun isDog(): Boolean =
        PetBreeds.byId(repository.snapshot().pet?.breedId ?: "black_cat")?.type == PetType.DOG

    /** 品种称呼：小狗 / 小猫。 */
    private fun petKindText(): String = if (isDog()) "小狗" else "小猫"

    /** 品种叫声：汪 / 喵。 */
    private fun petSound(): String = if (isDog()) "汪" else "喵"

    private val startupGreeting = listOf(
        "你回来啦，我刚刚还在想你。",
        "今天也一起慢慢生活吧。",
        "见到你真好，要不要摸摸我？",
        "外面的世界很大，我会在这里等你。",
        "先深呼吸一下，再开始今天吧。",
        "${if (PetBreeds.byId(repository.snapshot().pet?.breedId ?: "black_cat")?.type == PetType.DOG) "汪" else "喵"}～今天有什么想和我说的吗？",
    ).random()
    private val initialSnapshot = repository.snapshot()

    private val _state = MutableStateFlow(
        initialSnapshot.toUiState().copy(
            greeting = startupGreeting,
            // 没有宠物时进入领养流程：随机抽一只候选
            showAdoption = initialSnapshot.pet == null,
            candidateBreed = if (initialSnapshot.pet == null) PetBreeds.random() else null,
        ),
    )
    val state: StateFlow<PetPetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.darkThemeFlow.collect { darkTheme ->
                _state.update { it.copy(darkTheme = darkTheme) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.diaryEntriesStateFlow.collect { diaryState ->
                val newWarning = diaryState.storageWarning
                    ?.takeUnless { it == lastDiaryStorageWarning }
                lastDiaryStorageWarning = diaryState.storageWarning
                _state.update {
                    it.copy(
                        diaryEntries = diaryState.entries,
                        notice = newWarning ?: it.notice,
                    )
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.chatProviderFlow.collect { providerId ->
                _state.update { it.copy(chatProvider = ChatProvider.byId(providerId)) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.chatApiKeyFlow.collect { apiKey ->
                _state.update { it.copy(chatApiKey = apiKey) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.petSoundEnabledFlow.collect { enabled ->
                _state.update { it.copy(petSoundEnabled = enabled) }
            }
        }
    }

    /** 保存对话模型配置（Provider + API Key），设置页调用。 */
    fun setChatConfig(provider: ChatProvider, apiKey: String) {
        _state.update {
            it.copy(
                chatProvider = provider,
                chatApiKey = apiKey.trim(),
            )
        }
        viewModelScope.launch {
            preferencesRepository.setChatConfig(provider.id, apiKey)
        }
        showNotice("已切换对话模型：${provider.displayName}")
    }

    /** 领养流程：重新随机抽取一只宠物（用户不满意可换）。 */
    fun rollCandidatePet() {
        _state.update { it.copy(candidateBreed = PetBreeds.random()) }
    }

    /** 领养流程：确认领养当前候选宠物并命名。 */
    fun adoptPet(name: String) {
        val normalized = name.trim()
        if (normalized.length !in 1..12) {
            showNotice("宠物名字需要是 1 至 12 个字符")
            return
        }
        val breed = _state.value.candidateBreed ?: PetBreeds.random()
        val snapshot = repository.adoptPet(normalized, breed.id)
        applySnapshot(snapshot)
        _state.update {
            it.copy(
                showAdoption = false,
                candidateBreed = null,
            )
        }
        showNotice(snapshot.storageWarning ?: "领养成功，${normalized}以后就是你的唯一伙伴啦")
    }

    fun selectSection(section: HomeSection) {
        _state.update { it.copy(section = section) }
    }

    fun setDarkTheme(enabled: Boolean) {
        val previous = _state.value.darkTheme
        _state.update { it.copy(darkTheme = enabled) }
        viewModelScope.launch {
            try {
                preferencesRepository.setDarkTheme(enabled)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update {
                    if (it.darkTheme == enabled) it.copy(darkTheme = previous) else it
                }
                showNotice("主题设置保存失败，请稍后重试")
            }
        }
    }

    /** 宠物点击音效开关（默认关闭）。 */
    fun setPetSoundEnabled(enabled: Boolean) {
        _state.update { it.copy(petSoundEnabled = enabled) }
        viewModelScope.launch {
            try {
                preferencesRepository.setPetSoundEnabled(enabled)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _state.update { it.copy(petSoundEnabled = !enabled) }
                showNotice("音效设置保存失败，请稍后重试")
            }
        }
    }

    fun addDiaryEntry(title: String, content: String, images: List<String> = emptyList()) {
        val normalizedTitle = title.trim()
        val normalizedContent = content.trim()
        if (normalizedTitle.length !in 1..30) {
            showNotice("日记标题需要是 1 至 30 个字符")
            return
        }
        if (normalizedContent.length !in 1..1000) {
            showNotice("日记正文需要是 1 至 1000 个字符")
            return
        }
        viewModelScope.launch {
            try {
                val backedUpCorruptedRaw = preferencesRepository.addDiaryEntry(
                    normalizedTitle,
                    normalizedContent,
                    images,
                )
                showNotice(
                    if (backedUpCorruptedRaw) {
                        "${petKindText()}日记已保存，损坏的原始内容也已备份"
                    } else {
                        "${petKindText()}日记保存成功"
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                showNotice("${petKindText()}日记保存失败，原有内容未被覆盖")
            }
        }
    }

    fun deleteDiaryEntry(entryId: Long) {
        viewModelScope.launch {
            preferencesRepository.deleteDiaryEntry(entryId)
            showNotice("日记已删除")
        }
    }

    fun interact() {
        val (snapshot, gained) = repository.interact()
        applySnapshot(snapshot)
        val petName = snapshot.pet?.name ?: petKindText()
        showNotice(
            snapshot.storageWarning ?: if (gained > 0) {
                "摸摸成功，好感度 +$gained"
            } else {
                "$petName 很开心，今天的互动奖励已经领满啦"
            },
        )
    }

    fun sendMessage(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        if (normalized.length > 200) {
            showNotice("单条消息最多 200 个字符")
            return
        }
        if (_state.value.petTyping) {
            showNotice("${petKindText()}正在想怎么回你，稍等一下${petSound()}")
            return
        }

        val now = runtimeValues.now().format(timeFormatter)
        val userMessage = ChatMessage(runtimeValues.nextId(), true, normalized, now)
        // 先放一条"思考中"占位，网关返回后替换
        val thinkingId = runtimeValues.nextId()
        val thinkingMessage = ChatMessage(thinkingId, false, "…", now)
        val (snapshot, gained) = repository.recordValidChat()
        _state.update { current ->
            current.copy(
                pet = snapshot.pet,
                dailyProgress = snapshot.dailyProgress,
                posts = snapshot.posts,
                messages = current.messages + userMessage + thinkingMessage,
                petTyping = true,
                notice = snapshot.storageWarning ?: if (gained > 0) {
                    "完成今天的陪伴，好感度 +$gained"
                } else {
                    current.notice
                },
            )
        }

        viewModelScope.launch {
            val history = _state.value.messages
                .filter { it.id != thinkingId && it.id != userMessage.id }
                .takeLast(6)
                .map { (if (it.fromUser) "user" else "pet") to it.text }
            // 设置了 API Key → 直连模型（手机无需网关）；否则走网关（无 Key 时回退 .env 免费 Key）
            val chatProvider = _state.value.chatProvider
            val chatApiKey = _state.value.chatApiKey
            val response = if (chatApiKey.isNotBlank()) {
                gatewayClient.directChat(
                    provider = chatProvider,
                    apiKey = chatApiKey,
                    message = normalized,
                    history = history,
                    petName = _state.value.pet?.name ?: petKindText(),
                    isDog = isDog(),
                )
            } else {
                gatewayClient.sendMessage(
                    userId = "local-" + (repository.snapshot().pet?.name ?: "anonymous"),
                    message = normalized,
                    history = history,
                    provider = chatProvider.id,
                    apiKey = "",
                    petName = _state.value.pet?.name ?: petKindText(),
                    isDog = isDog(),
                )
            }
            // 网关可用且未降级 → 用模型回复；否则回退本地规则
            val replyText = if (response != null && !response.degraded && response.reply.isNotBlank()) {
                response.reply
            } else {
                // T-013：网络层失败（response == null）时明确提示离线陪伴状态；
                // 安全/限流等 degraded 响应本身有内容，不额外打扰
                if (response == null) {
                    showNotice("网络不可用，${petKindText()}改用离线方式陪你")
                }
                replyFor(normalized)
            }
            val replyMessage = ChatMessage(runtimeValues.nextId(), false, replyText, now)
            _state.update { current ->
                current.copy(
                    messages = current.messages.map { if (it.id == thinkingId) replyMessage else it },
                    petTyping = false,
                    // 按回复内容分析情绪，宠物显示对应表情（短暂展示后由协程清除）
                    petExpression = analyzeReplyEmotion(replyText),
                    // 触发对话弹窗（5 秒后由 UI 自动隐藏）
                    chatBubble = replyMessage.id to replyText,
                )
            }
            // 情绪表情展示 2.8 秒后恢复主头像
            delay(2800)
            _state.update { current ->
                if (current.petExpression != null) current.copy(petExpression = null) else current
            }
        }
    }

    /**
     * 根据模型回复内容分析宠物情绪表情。
     * 规则简单可扩展：命中关键词返回对应情绪，否则返回 null（主头像）。
     */
    private fun analyzeReplyEmotion(reply: String): String? {
        val text = reply.trim()
        return when {
            // 开心/积极
            text.contains("开心") || text.contains("高兴") || text.contains("哈哈") ||
                text.contains("太好了") || text.contains("真棒") || text.contains("喜欢") ||
                text.contains("加油") || text.contains("真好") -> "happy"
            // 惊喜
            text.contains("哇") || text.contains("真的吗") || text.contains("居然") ||
                text.contains("没想到") -> "surprised"
            // 难过/安慰
            text.contains("难过") || text.contains("伤心") || text.contains("别担心") ||
                text.contains("抱抱") || text.contains("没事的") || text.contains("心疼") -> "sad"
            // 默认：不额外切换（保持主头像）
            else -> null
        }
    }

    fun createPost(text: String, images: List<String> = emptyList()) {
        val normalized = text.trim()
        if (normalized.length !in 1..500) {
            showNotice("动态内容需要是 1 至 500 个字符")
            return
        }
        val petName = repository.snapshot().pet?.name
        val snapshot = repository.createPost(
            normalized,
            images,
            author = petName?.let { "我和我的${petKindText()}($it)" } ?: "我和我的${petKindText()}",
        )
        applySnapshot(snapshot)
        showNotice(snapshot.storageWarning ?: "动态发布成功（离线已保存）")
        // 后台同步到线上（失败不影响本地）
        viewModelScope.launch {
            val ok = gatewayClient.publishCommunityPost(
                authorId = "local-" + (repository.snapshot().pet?.name ?: "anonymous"),
                author = repository.snapshot().pet?.name?.let { "我和我的${petKindText()}($it)" } ?: "我和我的${petKindText()}",
                text = normalized,
            )
            if (ok) {
                _state.update { it.copy(communityOnline = true) }
            }
        }
    }

    /**
     * 进入社区时尝试拉取在线动态并合并到本地展示。
     * 无网络/网关不可用时静默保持本地数据（离线模式）。
     */
    fun refreshCommunityFromNetwork() {
        viewModelScope.launch {
            val remote = gatewayClient.fetchCommunityPosts()
            if (remote == null) {
                _state.update { it.copy(communityOnline = false) }
                return@launch
            }
            _state.update { it.copy(communityOnline = true) }
            // 在线动态合并进本地（按 id 去重，在线优先展示在前）
            val localPosts = repository.snapshot().posts
            val remotePosts = remote.mapNotNull { rp ->
                val local = localPosts.firstOrNull { it.id.toString() == rp.id }
                if (local != null) null else comParse(rp)
            }
            if (remotePosts.isNotEmpty()) {
                repository.mergeRemotePosts(remotePosts)
                applySnapshot(repository.snapshot())
            }
        }
    }

    private fun comParse(rp: ChatGatewayClient.RemotePost): CommunityPost {
        return CommunityPost(
            id = rp.id.hashCode().toLong() and 0x7fffffffL,
            author = rp.author,
            text = rp.text,
            time = rp.time.take(16).replace("T", " "),
            liked = false,
            likeCount = rp.likeCount,
            comments = emptyList(),
        )
    }

    fun toggleLike(postId: Long) {
        applySnapshot(repository.toggleLike(postId))
    }

    fun addComment(postId: Long, text: String) {
        val normalized = text.trim()
        if (normalized.length !in 1..120) {
            showNotice("评论需要是 1 至 120 个字符")
            return
        }
        val snapshot = repository.addComment(postId, normalized)
        applySnapshot(snapshot)
        showNotice(snapshot.storageWarning ?: "评论成功")
    }

    fun reportPost() {
        showNotice("已收到举报，演示版会将记录交给审核队列")
    }

    /** 收藏 / 取消收藏（帖子三点菜单）。 */
    fun toggleCollect(postId: Long) {
        applySnapshot(repository.toggleCollect(postId))
    }

    /** 不喜欢 / 取消不喜欢（帖子三点菜单）。 */
    fun toggleDislike(postId: Long) {
        applySnapshot(repository.toggleDislike(postId))
    }

    /** 删除帖子进垃圾桶（正文/图片隐藏，仅保留头像/名字/标题）。 */
    fun deletePost(postId: Long) {
        applySnapshot(repository.deletePost(postId))
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    private fun applySnapshot(snapshot: LocalSnapshot) {
        _state.update {
            it.copy(
                pet = snapshot.pet,
                dailyProgress = snapshot.dailyProgress,
                posts = snapshot.posts,
                notice = snapshot.storageWarning ?: it.notice,
            )
        }
    }

    private fun showNotice(message: String) {
        _state.update { it.copy(notice = message) }
    }

    private fun replyFor(message: String): String {
        val sound = petSound()
        val kind = petKindText()
        return when {
            listOf("难过", "伤心", "不好", "累").any(message::contains) ->
                "$sound……我会安静地陪着你。今天已经很努力了，可以慢一点。"
            listOf("开心", "高兴", "喜欢").any(message::contains) ->
                "听起来是很棒的一天！我也跟着开心起来啦，$sound～"
            listOf("早安", "早上好").any(message::contains) ->
                "早安！今天也请带着一点点好心情出发吧。"
            listOf("晚安", "睡觉").any(message::contains) ->
                "晚安，我会把尾巴圈起来，守着你的好梦。"
            message.endsWith("吗") || message.contains("？") || message.contains("?") ->
                "让我用${kind}的方式想一想……我觉得你的感受最重要，$sound。"
            else -> "我听见啦！再和我多说一点吧，$sound～"
        }
    }

    private fun LocalSnapshot.toUiState(): PetPetUiState {
        return PetPetUiState(
            pet = pet,
            dailyProgress = dailyProgress,
            posts = posts,
            messages = listOf(
                ChatMessage(
                    id = 0,
                    fromUser = false,
                    text = "你好呀，我会一直在这里听你说话。今天过得怎么样？",
                    time = runtimeValues.now().format(timeFormatter),
                ),
            ),
            notice = storageWarning,
        )
    }
}
