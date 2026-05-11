package com.zone.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zone.android.core.database.dao.CalibrationProfileDao
import com.zone.android.core.database.dao.SessionDao
import com.zone.android.core.database.dao.SessionEventDao
import com.zone.android.core.database.dao.VideoItemDao
import com.zone.android.core.database.entity.CalibrationProfileEntity
import com.zone.android.core.database.entity.SessionEntity
import com.zone.android.core.database.entity.SessionEventEntity
import com.zone.android.core.database.entity.VideoItemEntity

/**
 * Root Room database for ZONE persistence.
 */
@Database(
    entities = [
        VideoItemEntity::class,
        SessionEntity::class,
        SessionEventEntity::class,
        CalibrationProfileEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class ZoneDatabase : RoomDatabase() {
    abstract fun videoItemDao(): VideoItemDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionEventDao(): SessionEventDao
    abstract fun calibrationProfileDao(): CalibrationProfileDao
}

