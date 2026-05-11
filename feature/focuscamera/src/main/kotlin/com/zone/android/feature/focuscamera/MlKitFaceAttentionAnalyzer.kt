package com.zone.android.feature.focuscamera

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.zone.android.core.model.AttentionSignal
import com.zone.android.core.model.AttentionTrackingStatus
import com.zone.android.core.model.FaceAttentionAnalyzer
import com.zone.android.core.model.SessionRuntimeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX + ML Kit implementation of [FaceAttentionAnalyzer].
 */
class MlKitFaceAttentionAnalyzer(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val runtimeConfig: SessionRuntimeConfig,
) : FaceAttentionAnalyzer {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setMinFaceSize(0.12f)
            .enableTracking()
            .build(),
    )
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val _signals = MutableStateFlow(
        AttentionSignalRules.evaluate(
            timestampMillis = SystemClock.elapsedRealtime(),
            metrics = null,
            runtimeConfig = runtimeConfig,
            trackingStatus = AttentionTrackingStatus.IDLE,
        ),
    )

    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var started = false

    override fun signals(): Flow<AttentionSignal> = _signals.asStateFlow()

    override fun start() {
        if (started) {
            return
        }
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            emitFailure(AttentionTrackingStatus.NO_FRONT_CAMERA)
            return
        }
        started = true
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    cameraProviderFuture.get()
                }.onSuccess { provider ->
                    cameraProvider = provider
                    bindAnalysis()
                }.onFailure {
                    emitFailure(AttentionTrackingStatus.CAMERA_BIND_FAILED)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun stop() {
        if (!started) {
            return
        }
        started = false
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        _signals.value = AttentionSignalRules.evaluate(
            timestampMillis = SystemClock.elapsedRealtime(),
            metrics = null,
            runtimeConfig = runtimeConfig,
            trackingStatus = AttentionTrackingStatus.IDLE,
        )
    }

    private fun bindAnalysis() {
        val provider = cameraProvider ?: return
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysisUseCase ->
                analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }

        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                imageAnalysis,
            )
        }.onSuccess {
            analysis = imageAnalysis
            _signals.value = AttentionSignalRules.evaluate(
                timestampMillis = SystemClock.elapsedRealtime(),
                metrics = null,
                runtimeConfig = runtimeConfig,
                trackingStatus = AttentionTrackingStatus.RUNNING,
            )
        }.onFailure {
            emitFailure(AttentionTrackingStatus.CAMERA_BIND_FAILED)
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val timestampMillis = SystemClock.elapsedRealtime()
                _signals.value = buildSignal(
                    faces = faces,
                    imageWidth = imageProxy.width,
                    imageHeight = imageProxy.height,
                    timestampMillis = timestampMillis,
                )
            }
            .addOnFailureListener {
                emitFailure(AttentionTrackingStatus.DETECTOR_FAILED)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun buildSignal(
        faces: List<Face>,
        imageWidth: Int,
        imageHeight: Int,
        timestampMillis: Long,
    ) = AttentionSignalRules.evaluate(
        timestampMillis = timestampMillis,
        metrics = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }?.toMetrics(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        ),
        runtimeConfig = runtimeConfig,
        trackingStatus = AttentionTrackingStatus.RUNNING,
    )

    private fun Face.toMetrics(
        imageWidth: Int,
        imageHeight: Int,
    ): FaceFrameMetrics {
        val box = boundingBox
        val sizeRatio = (box.width().toFloat() * box.height().toFloat()) /
            (imageWidth.toFloat() * imageHeight.toFloat())
        return FaceFrameMetrics(
            centerX = box.centerX() / imageWidth.toFloat(),
            centerY = box.centerY() / imageHeight.toFloat(),
            faceSizeRatio = sizeRatio,
            yawDegrees = headEulerAngleY,
            pitchDegrees = headEulerAngleX,
            rollDegrees = headEulerAngleZ,
        )
    }

    private fun emitFailure(status: AttentionTrackingStatus) {
        started = false
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        _signals.value = AttentionSignalRules.evaluate(
            timestampMillis = SystemClock.elapsedRealtime(),
            metrics = null,
            runtimeConfig = runtimeConfig,
            trackingStatus = status,
        )
    }
}
