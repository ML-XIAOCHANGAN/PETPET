package cn.petpet.app.data

import cn.petpet.app.model.CommunityComment
import cn.petpet.app.model.CommunityPost
import cn.petpet.app.model.DiaryEntry
import org.json.JSONArray
import org.json.JSONObject

internal data class DecodedStorageValue<T>(
    val value: T,
    val corruptedRaw: String? = null,
) {
    val isCorrupted: Boolean
        get() = corruptedRaw != null
}

/**
 * JSON 的编解码集中在此处，损坏时返回原始内容，交由仓库先备份再继续写入。
 */
internal object PersistenceCodecs {
    fun decodeDiaryEntries(raw: String?): DecodedStorageValue<List<DiaryEntry>> {
        if (raw.isNullOrBlank()) return DecodedStorageValue(emptyList())
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    add(
                        DiaryEntry(
                            id = item.getLong("id"),
                            title = item.getString("title"),
                            content = item.getString("content"),
                            createdAt = item.getString("createdAt"),
                            images = decodeStringList(item, "images"),
                        ),
                    )
                }
            }
        }.fold(
            onSuccess = { DecodedStorageValue(it) },
            onFailure = { DecodedStorageValue(emptyList(), corruptedRaw = raw) },
        )
    }

    fun encodeDiaryEntries(entries: List<DiaryEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("title", entry.title)
                    .put("content", entry.content)
                    .put("createdAt", entry.createdAt)
                    .put("images", JSONArray(entry.images)),
            )
        }
        return array.toString()
    }

    fun decodePosts(raw: String?): DecodedStorageValue<List<CommunityPost>> {
        if (raw.isNullOrBlank()) return DecodedStorageValue(emptyList())
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    val commentsArray = if (!item.has("comments") || item.isNull("comments")) {
                        JSONArray()
                    } else {
                        item.getJSONArray("comments")
                    }
                    val comments = buildList {
                        repeat(commentsArray.length()) { commentIndex ->
                            val comment = commentsArray.getJSONObject(commentIndex)
                            add(
                                CommunityComment(
                                    id = comment.getLong("id"),
                                    author = comment.getString("author"),
                                    text = comment.getString("text"),
                                ),
                            )
                        }
                    }
                    val likeCount = if (item.has("likeCount")) item.getInt("likeCount") else 0
                    require(likeCount >= 0) { "喜欢数量不能为负数" }
                    add(
                        CommunityPost(
                            id = item.getLong("id"),
                            author = item.getString("author"),
                            text = item.getString("text"),
                            time = item.getString("time"),
                            liked = if (item.has("liked")) item.getBoolean("liked") else false,
                            likeCount = likeCount,
                            comments = comments,
                            collected = if (item.has("collected")) item.getBoolean("collected") else false,
                            disliked = if (item.has("disliked")) item.getBoolean("disliked") else false,
                            images = decodeStringList(item, "images"),
                            inTrash = if (item.has("inTrash")) item.getBoolean("inTrash") else false,
                        ),
                    )
                }
            }
        }.fold(
            onSuccess = { DecodedStorageValue(it) },
            onFailure = { DecodedStorageValue(emptyList(), corruptedRaw = raw) },
        )
    }

    fun encodePosts(posts: List<CommunityPost>): String {
        val array = JSONArray()
        posts.forEach { post ->
            val comments = JSONArray()
            post.comments.forEach { comment ->
                comments.put(
                    JSONObject()
                        .put("id", comment.id)
                        .put("author", comment.author)
                        .put("text", comment.text),
                )
            }
            array.put(
                JSONObject()
                    .put("id", post.id)
                    .put("author", post.author)
                    .put("text", post.text)
                    .put("time", post.time)
                    .put("liked", post.liked)
                    .put("likeCount", post.likeCount)
                    .put("comments", comments)
                    .put("collected", post.collected)
                    .put("disliked", post.disliked)
                    .put("images", JSONArray(post.images))
                    .put("inTrash", post.inTrash),
            )
        }
        return array.toString()
    }

    /** 容错读取字符串数组字段（缺失/非数组返回空列表）。 */
    private fun decodeStringList(item: JSONObject, key: String): List<String> {
        val arr = item.optJSONArray(key) ?: return emptyList()
        return buildList {
            repeat(arr.length()) { i ->
                arr.optString(i, "").takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }
}
