package cn.petpet.app.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.petpet.app.model.DiaryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.format.DateTimeFormatter

private val Context.petPetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "petpet_preferences",
)

/**
 * 同时返回可展示的日记和读取警告，避免用正常空列表掩盖存储故障。
 */
data class DiaryEntriesState(
    val entries: List<DiaryEntry>,
    val storageWarning: String? = null,
)

private data class SafePreferences(
    val preferences: Preferences,
    val storageWarning: String? = null,
)

/**
 * 负责保存跨启动仍需保留的轻量设置和日记。
 * 日记目前使用 JSON 存入 DataStore，后续数据量扩大时可以平滑迁移到 Room。
 */
class PetPetPreferencesRepository(
    context: Context,
    private val runtimeValues: RuntimeValues = ApplicationRuntimeValues.instance,
) {
    private val dataStore = context.petPetDataStore
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    val darkThemeFlow: Flow<Boolean> = safePreferencesFlow()
        .map { state -> state.preferences[KEY_DARK_THEME] ?: false }

    val diaryEntriesStateFlow: Flow<DiaryEntriesState> = safePreferencesFlow()
        .map { state ->
            val decoded = PersistenceCodecs.decodeDiaryEntries(
                state.preferences[KEY_DIARY_ENTRIES],
            )
            DiaryEntriesState(
                entries = decoded.value,
                storageWarning = state.storageWarning ?: if (decoded.isCorrupted) {
                    "日记数据读取失败；原始内容仍保留，并会在下次保存时备份"
                } else {
                    null
                },
            )
        }

    val diaryEntriesFlow: Flow<List<DiaryEntry>> = diaryEntriesStateFlow
        .map { it.entries }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = enabled
        }
    }

    suspend fun addDiaryEntry(title: String, content: String): Boolean {
        var backedUpCorruptedRaw = false
        dataStore.edit { preferences ->
            val decoded = PersistenceCodecs.decodeDiaryEntries(preferences[KEY_DIARY_ENTRIES])
            decoded.corruptedRaw?.let { raw ->
                preferences[KEY_DIARY_ENTRIES_CORRUPTED_BACKUP] = raw
                backedUpCorruptedRaw = true
            }
            val newEntry = DiaryEntry(
                id = runtimeValues.nextId(),
                title = title.trim(),
                content = content.trim(),
                createdAt = runtimeValues.now().format(dateTimeFormatter),
            )
            preferences[KEY_DIARY_ENTRIES] = PersistenceCodecs.encodeDiaryEntries(
                listOf(newEntry) + decoded.value,
            )
        }
        return backedUpCorruptedRaw
    }

    private fun safePreferencesFlow(): Flow<SafePreferences> {
        return dataStore.data
            .map { SafePreferences(it) }
            .catch { exception ->
                if (exception is IOException || exception is CorruptionException) {
                    emit(
                        SafePreferences(
                            preferences = emptyPreferences(),
                            storageWarning = "设置与日记文件读取失败，原始文件未被覆盖",
                        ),
                    )
                } else {
                    throw exception
                }
            }
    }

    private companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_DIARY_ENTRIES = stringPreferencesKey("diary_entries")
        val KEY_DIARY_ENTRIES_CORRUPTED_BACKUP =
            stringPreferencesKey("diary_entries_corrupted_backup")
    }
}
