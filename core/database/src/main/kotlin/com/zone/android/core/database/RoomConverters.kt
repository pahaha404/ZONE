package com.zone.android.core.database

import androidx.room.TypeConverter
import com.zone.android.core.model.FocusViolation
import com.zone.android.core.model.SessionEventType
import com.zone.android.core.model.StrictnessLevel

/**
 * Type converters for enum persistence.
 */
class RoomConverters {
    @TypeConverter
    fun strictnessLevelToString(value: StrictnessLevel): String = value.name

    @TypeConverter
    fun stringToStrictnessLevel(value: String): StrictnessLevel = StrictnessLevel.valueOf(value)

    @TypeConverter
    fun sessionEventTypeToString(value: SessionEventType): String = value.name

    @TypeConverter
    fun stringToSessionEventType(value: String): SessionEventType = SessionEventType.valueOf(value)

    @TypeConverter
    fun focusViolationToString(value: FocusViolation): String = value.name

    @TypeConverter
    fun stringToFocusViolation(value: String): FocusViolation = FocusViolation.valueOf(value)
}

