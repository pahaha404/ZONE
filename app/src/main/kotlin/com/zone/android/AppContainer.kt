package com.zone.android

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.room.Room
import com.zone.android.core.database.ZoneDatabase
import com.zone.android.core.database.repository.SessionRepositoryImpl
import com.zone.android.core.database.repository.VideoRepositoryImpl
import com.zone.android.core.datastore.ZoneSettingsRepository
import com.zone.android.core.model.FaceAttentionAnalyzer
import com.zone.android.core.model.SensorFocusEngine
import com.zone.android.core.model.SessionRuntimeConfig
import com.zone.android.core.model.SessionRepository
import com.zone.android.core.model.SettingsRepository
import com.zone.android.core.model.StrictnessLevel
import com.zone.android.core.model.VideoItem
import com.zone.android.core.model.VideoRepository
import com.zone.android.feature.focuscamera.MlKitFaceAttentionAnalyzer
import com.zone.android.feature.player.ExoPlayerCoordinator
import com.zone.android.feature.sensors.AndroidSensorFocusEngine
import com.zone.android.feature.session.DefaultSessionController

/**
 * Manual dependency container for the ZONE app.
 */
class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val database: ZoneDatabase = Room.databaseBuilder(
        appContext,
        ZoneDatabase::class.java,
        "zone.db",
    ).fallbackToDestructiveMigration().build()

    val videoRepository: VideoRepository = VideoRepositoryImpl(
        context = appContext,
        videoItemDao = database.videoItemDao(),
    )

    val sessionRepository: SessionRepository = SessionRepositoryImpl(
        sessionDao = database.sessionDao(),
        sessionEventDao = database.sessionEventDao(),
        calibrationProfileDao = database.calibrationProfileDao(),
        videoItemDao = database.videoItemDao(),
    )

    val settingsRepository: SettingsRepository = ZoneSettingsRepository(appContext)

    fun createSensorFocusEngine(runtimeConfig: SessionRuntimeConfig): SensorFocusEngine =
        AndroidSensorFocusEngine(
            context = appContext,
            runtimeConfig = runtimeConfig,
        )

    fun createFaceAttentionAnalyzer(
        lifecycleOwner: LifecycleOwner,
        runtimeConfig: SessionRuntimeConfig,
    ): FaceAttentionAnalyzer =
        MlKitFaceAttentionAnalyzer(
            context = appContext,
            lifecycleOwner = lifecycleOwner,
            runtimeConfig = runtimeConfig,
        )

    fun createPlayerCoordinator(): ExoPlayerCoordinator = ExoPlayerCoordinator(appContext)

    fun createSessionController(
        videoItem: VideoItem,
        strictnessLevel: StrictnessLevel,
        runtimeConfig: SessionRuntimeConfig,
        sessionIntent: String,
        sensorFocusEngine: SensorFocusEngine,
        faceAttentionAnalyzer: FaceAttentionAnalyzer,
        playerCoordinator: ExoPlayerCoordinator,
    ): DefaultSessionController = DefaultSessionController(
        videoItem = videoItem,
        strictnessLevel = strictnessLevel,
        runtimeConfig = runtimeConfig,
        sessionIntent = sessionIntent,
        sessionRepository = sessionRepository,
        sensorFocusEngine = sensorFocusEngine,
        faceAttentionAnalyzer = faceAttentionAnalyzer,
        playerCoordinator = playerCoordinator,
    )
}
