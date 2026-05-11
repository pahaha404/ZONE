package com.zone.android.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zone.android.core.model.SettingsRepository
import com.zone.android.core.model.StrictnessLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val SETTINGS_NAME = "zone_settings"

private val Context.zonePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_NAME,
)

/**
 * DataStore-backed implementation of [SettingsRepository].
 */
class ZoneSettingsRepository(
    context: Context,
) : SettingsRepository {
    private val dataStore = context.zonePreferencesDataStore

    override val strictnessLevel: Flow<StrictnessLevel> =
        dataStore.safeData.map { preferences ->
            preferences[Keys.StrictnessLevel]?.let(StrictnessLevel::valueOf)
                ?: StrictnessLevel.STANDARD
        }

    override val onboardingCompleted: Flow<Boolean> =
        dataStore.safeData.map { preferences -> preferences[Keys.OnboardingCompleted] ?: false }

    override val cameraPermissionAsked: Flow<Boolean> =
        dataStore.safeData.map { preferences -> preferences[Keys.CameraPermissionAsked] ?: false }

    override val lastVideoId: Flow<Long?> =
        dataStore.safeData.map { preferences -> preferences[Keys.LastVideoId] }

    override val sessionIntent: Flow<String> =
        dataStore.safeData.map { preferences -> preferences[Keys.SessionIntent] ?: "" }

    override suspend fun setStrictnessLevel(level: StrictnessLevel) {
        dataStore.edit { preferences ->
            preferences[Keys.StrictnessLevel] = level.name
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.OnboardingCompleted] = completed
        }
    }

    override suspend fun setCameraPermissionAsked(requested: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.CameraPermissionAsked] = requested
        }
    }

    override suspend fun setLastVideoId(videoId: Long?) {
        dataStore.edit { preferences ->
            if (videoId == null) {
                preferences.remove(Keys.LastVideoId)
            } else {
                preferences[Keys.LastVideoId] = videoId
            }
        }
    }

    override suspend fun setSessionIntent(intent: String) {
        dataStore.edit { preferences ->
            preferences[Keys.SessionIntent] = intent
        }
    }

    private val DataStore<Preferences>.safeData: Flow<Preferences>
        get() = data.catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    private object Keys {
        val StrictnessLevel = stringPreferencesKey("strictness_level")
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val CameraPermissionAsked = booleanPreferencesKey("camera_permission_asked")
        val LastVideoId = longPreferencesKey("last_video_id")
        val SessionIntent = stringPreferencesKey("session_intent")
    }
}
