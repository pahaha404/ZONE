package com.zone.android.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zone.android.core.model.StrictnessLevel

/**
 * Room entity for saved calibration baselines.
 */
@Entity(
    tableName = "calibration_profiles",
    foreignKeys = [
        ForeignKey(
            entity = VideoItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["video_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("video_id"), Index(value = ["video_id", "strictness_level"])],
)
data class CalibrationProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "video_id")
    val videoId: Long,
    @ColumnInfo(name = "strictness_level")
    val strictnessLevel: StrictnessLevel,
    @ColumnInfo(name = "baseline_quaternion_w")
    val baselineQuaternionW: Double,
    @ColumnInfo(name = "baseline_quaternion_x")
    val baselineQuaternionX: Double,
    @ColumnInfo(name = "baseline_quaternion_y")
    val baselineQuaternionY: Double,
    @ColumnInfo(name = "baseline_quaternion_z")
    val baselineQuaternionZ: Double,
    @ColumnInfo(name = "baseline_face_center_x")
    val baselineFaceCenterX: Float,
    @ColumnInfo(name = "baseline_face_center_y")
    val baselineFaceCenterY: Float,
    @ColumnInfo(name = "baseline_face_size_ratio")
    val baselineFaceSizeRatio: Float,
    @ColumnInfo(name = "baseline_yaw_degrees")
    val baselineYawDegrees: Float,
    @ColumnInfo(name = "baseline_pitch_degrees")
    val baselinePitchDegrees: Float,
    @ColumnInfo(name = "baseline_roll_degrees")
    val baselineRollDegrees: Float,
    @ColumnInfo(name = "captured_at_epoch_ms")
    val capturedAtEpochMs: Long,
)

