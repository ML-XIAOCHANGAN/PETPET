package cn.petpet.app.data

import cn.petpet.app.model.CommunityComment
import cn.petpet.app.model.CommunityPost
import cn.petpet.app.model.DiaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceCodecsTest {
    @Test
    fun `日记往返编解码保留中文与换行`() {
        val entries = listOf(
            DiaryEntry(
                id = 10L,
                title = "今天的猫饭",
                content = "第一行\n第二行：\"很好吃\"",
                createdAt = "2026-08-09 22:30",
            ),
        )

        val decoded = PersistenceCodecs.decodeDiaryEntries(
            PersistenceCodecs.encodeDiaryEntries(entries),
        )

        assertFalse(decoded.isCorrupted)
        assertEquals(entries, decoded.value)
    }

    @Test
    fun `损坏的日记内容不会被伪装成正常空列表`() {
        val raw = "这不是有效的日记数据"

        val decoded = PersistenceCodecs.decodeDiaryEntries(raw)

        assertTrue(decoded.isCorrupted)
        assertEquals(raw, decoded.corruptedRaw)
        assertTrue(decoded.value.isEmpty())
    }

    @Test
    fun `社区动态往返编解码保留喜欢和评论状态`() {
        val posts = listOf(
            CommunityPost(
                id = 20L,
                author = "我和我的小猫",
                text = "今天很开心",
                time = "08-09 22:30",
                liked = true,
                likeCount = 3,
                comments = listOf(
                    CommunityComment(21L, "我", "喵～"),
                ),
            ),
        )

        val decoded = PersistenceCodecs.decodePosts(PersistenceCodecs.encodePosts(posts))

        assertFalse(decoded.isCorrupted)
        assertEquals(posts, decoded.value)
    }

    @Test
    fun `负数喜欢数量会被判定为损坏并保留原文`() {
        val raw = """[{"id":1,"author":"甲","text":"乙","time":"现在","likeCount":-1}]"""

        val decoded = PersistenceCodecs.decodePosts(raw)

        assertTrue(decoded.isCorrupted)
        assertEquals(raw, decoded.corruptedRaw)
        assertTrue(decoded.value.isEmpty())
    }
}
