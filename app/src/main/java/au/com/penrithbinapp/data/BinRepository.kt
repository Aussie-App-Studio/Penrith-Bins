package au.com.penrithbinapp.data

import android.content.Context
import au.com.penrithbinapp.model.CachedScheduleData
import au.com.penrithbinapp.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BinRepository private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _scheduleData = MutableStateFlow<CachedScheduleData?>(loadCachedSchedule())
    val scheduleData: StateFlow<CachedScheduleData?> = _scheduleData.asStateFlow()

    private val _userSettings = MutableStateFlow(loadUserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    fun loadCachedSchedule(): CachedScheduleData? {
        val raw = prefs.getString(KEY_SCHEDULE_DATA, null) ?: return null
        return runCatching { json.decodeFromString<CachedScheduleData>(raw) }.getOrNull()
    }

    fun saveCachedSchedule(data: CachedScheduleData) {
        val encoded = json.encodeToString(data)
        prefs.edit().putString(KEY_SCHEDULE_DATA, encoded).apply()
        _scheduleData.value = data
    }

    fun clearCachedSchedule() {
        prefs.edit().remove(KEY_SCHEDULE_DATA).apply()
        _scheduleData.value = null
    }

    fun loadUserSettings(): UserSettings {
        val raw = prefs.getString(KEY_USER_SETTINGS, null) ?: return UserSettings()
        return runCatching { json.decodeFromString<UserSettings>(raw) }.getOrDefault(UserSettings())
    }

    fun updateUserSettings(transform: (UserSettings) -> UserSettings): UserSettings {
        val updated = transform(_userSettings.value)
        val encoded = json.encodeToString(updated)
        prefs.edit().putString(KEY_USER_SETTINGS, encoded).apply()
        _userSettings.value = updated
        return updated
    }

    companion object {
        private const val PREFS_NAME = "penrith_bin_app_prefs"
        private const val KEY_SCHEDULE_DATA = "cached_schedule_data_v1"
        private const val KEY_USER_SETTINGS = "user_settings_v1"

        @Volatile
        private var instance: BinRepository? = null

        fun getInstance(context: Context): BinRepository {
            return instance ?: synchronized(this) {
                instance ?: BinRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
