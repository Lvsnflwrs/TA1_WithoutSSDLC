package com.example.mobilesurapp.UIApp.addFace

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilesurapp.domain.usecase.RegisterUserWithFaceUseCase
import com.example.mobilesurapp.face.FaceEmbedder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.mutableStateOf
import com.example.mobilesurapp.api.ApiResult
import com.example.mobilesurapp.model.User
import com.example.mobilesurapp.modelload.AddFaceDetector
import com.example.mobilesurapp.domain.utils.MediaPipeUtils.resizeBitmap
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.ExperimentalGetImage
import com.example.mobilesurapp.domain.utils.ImageCropper
import com.example.mobilesurapp.domain.utils.MediaPipeUtils.toBitmapWithoutConverter
import com.example.mobilesurapp.modelload.MediaPipeFaceDetector
import java.io.File
import java.io.FileOutputStream

@HiltViewModel
class AddFaceViewModel @Inject constructor(
    application: Application,
    private val registerUserWithFaceUseCase: RegisterUserWithFaceUseCase,
    private val faceEmbedder: FaceEmbedder,
    private val addFaceDetector: AddFaceDetector
) : AndroidViewModel(application) {

    val name = mutableStateOf("")
    val email = mutableStateOf("")
    val phone = mutableStateOf("")
    private var currentAdminId: String = ""

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_FRONT)
    val lensFacing: StateFlow<Int> = _lensFacing

    private val _isFaceDetected = MutableStateFlow(false)
    val isFaceDetected: StateFlow<Boolean> = _isFaceDetected

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _recordingProgress = MutableStateFlow(0f)
    val recordingProgress: StateFlow<Float> = _recordingProgress

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _liveDetectionResult = MutableStateFlow<FaceDetectorResult?>(null)
    val liveDetectionResult: StateFlow<FaceDetectorResult?> = _liveDetectionResult // Untuk FaceOverlay

    private val capturedFrames = mutableListOf<Bitmap>()
    private var recordingJob: Job? = null
    private var lastFrameCaptureTime: Long = 0L

    private val _imageDimensions = MutableStateFlow(Size(1, 1))
    val imageDimensions: StateFlow<Size> = _imageDimensions

    private val _countdown = MutableStateFlow(5)
    val countdown: StateFlow<Int> = _countdown

    private var isFrontCamera: Boolean = true

    private var liveFaceDetector: MediaPipeFaceDetector? = null

    private val _namaError = MutableStateFlow<String?>(null)
    val namaError: StateFlow<String?> = _namaError

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError

    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError

    companion object {
        private const val RECORD_DURATION_MILLIS = 5000L
        private const val FRAME_CAPTURE_INTERVAL_MILLIS = 500L
        private const val TARGET_FACE_SIZE = 160
        private const val FACE_EXPANSION_FACTOR = 0.9f
        private const val TAG = "AddFaceViewModel"
    }

    init {
        liveFaceDetector = MediaPipeFaceDetector(
            context = getApplication(),
            onResult = { result ->
                _isFaceDetected.value = result.detections().isNotEmpty()
                _liveDetectionResult.value = result
            },
            onError = { error ->
                Log.e(TAG, "Live Face Detector error: ${error.message}")
                _isFaceDetected.value = false
                _liveDetectionResult.value = null
            }
        )

        viewModelScope.launch {
            _lensFacing.collect { lens ->
                isFrontCamera = (lens == CameraSelector.LENS_FACING_FRONT)
            }
        }
    }
    private fun isNameValid(): Boolean {
        val nameRegex = "^[a-zA-Z\\s]{1,50}$".toRegex()
        return name.value.trim().matches(nameRegex)
    }

    private fun isEmailValid(): Boolean {
        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        val emailRegex = emailPattern.toRegex()
        return email.value.trim().matches(emailRegex) && email.value.length <= 100
    }

    private fun isPhoneNumValid(): Boolean {
        val phonePattern = "^(08|628)[0-9]{8,12}$"
        val phoneRegex = phonePattern.toRegex()
        return phone.value.trim().matches(phoneRegex)
    }

    fun startRecording(adminId: String, userName: String, userEmail: String, userPhone: String) {
        if (_recordingState.value != RecordingState.IDLE) {
            Log.w(TAG, "Already recording or processing. Ignoring startRecording call.")
            return
        }

        currentAdminId = adminId
        name.value = userName
        email.value = userEmail
        phone.value = userPhone

        var isValid = true
        if (!isNameValid()) {
            _namaError.value = "Nama tidak valid! Nama harus alphabet dan maskimal 50 huruf"
            isValid = false
        } else {
            _namaError.value = null
        }

        if (!isEmailValid()) {
            _emailError.value = "Format email tidak valid atau terlalu panjang."
            isValid = false
        } else {
            _emailError.value = null
        }

        if(!isPhoneNumValid()) {
            _phoneError.value = "Nomor telepon harus diawali 08/628 dan berisi 10-14 angka."
            isValid = false
        } else {
            _phoneError.value = null
        }

        if(!isValid){
            return
        }

        _recordingState.value = RecordingState.RECORDING
        _recordingProgress.value = 0f
        _countdown.value = (RECORD_DURATION_MILLIS / 1000).toInt()
        capturedFrames.clear()
        _message.value = "Merekam wajah, mohon tetap di tengah dan sedikit gerakkan kepala secara perlahan!"
        lastFrameCaptureTime = System.currentTimeMillis()

        recordingJob = viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < RECORD_DURATION_MILLIS) {
                _recordingProgress.value = ((System.currentTimeMillis() - startTime).toFloat() / RECORD_DURATION_MILLIS)
                delay(100)
            }
            _recordingState.value = RecordingState.PROCESSING
            _message.value = "Memproses frame dan membuat embedding..."
            processBufferedFrames()
        }

        viewModelScope.launch {
            val totalSeconds = (RECORD_DURATION_MILLIS / 1000).toInt()
            for (second in totalSeconds downTo 0) {
                _countdown.value = second
                delay(1000L)
            }
        }
    }

    fun updateImageDimensions(width: Int, height: Int) {
        if (width > 1 && height > 1 && _imageDimensions.value.width != width || _imageDimensions.value.height != height) {
            _imageDimensions.value = Size(width, height)
        }
    }

    @ExperimentalGetImage
    fun processFrame(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        try {
            if (imageProxy.image == null) {
                Log.w(TAG, "ImageProxy.image is null in processFrame. Skipping frame.")
                return
            }

            var bitmap = imageProxy.toBitmapWithoutConverter()

            if (bitmap == null || bitmap.isRecycled) {
                Log.e(TAG, "Failed to convert ImageProxy to Bitmap or bitmap is recycled.")
                return
            }

            var processedBitmap = bitmap
            if (rotationDegrees != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(
                    processedBitmap,
                    0,
                    0,
                    processedBitmap.width,
                    processedBitmap.height,
                    matrix,
                    true
                )
                processedBitmap.recycle()
                processedBitmap = rotatedBitmap
            }

            if (isFrontCamera) {
                val matrixFlip = Matrix()
                matrixFlip.postScale(-1f, 1f, processedBitmap.width / 2f, processedBitmap.height / 2f)
                val flippedBitmap = Bitmap.createBitmap(
                    processedBitmap,
                    0,
                    0,
                    processedBitmap.width,
                    processedBitmap.height,
                    matrixFlip,
                    true
                )
                processedBitmap.recycle()
                processedBitmap = flippedBitmap
            }

            liveFaceDetector?.detect(processedBitmap)

            if (_recordingState.value == RecordingState.RECORDING) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFrameCaptureTime >= FRAME_CAPTURE_INTERVAL_MILLIS) {
                    lastFrameCaptureTime = currentTime
                    if (processedBitmap != null && !processedBitmap.isRecycled) {
                        capturedFrames.add(processedBitmap)
                    } else {
                        Log.e(TAG, "Processed bitmap is null or recycled. Not adding to buffer.")
                    }
                } else {
                    processedBitmap.recycle()
                }
            } else {
                processedBitmap.recycle()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}", e)
        } finally {
            imageProxy.close()
        }
    }

    private suspend fun processBufferedFrames() {
        if (capturedFrames.isEmpty()) {
            _message.value = "Tidak ada frame yang diambil. Coba lagi."
            _recordingState.value = RecordingState.IDLE
            Log.w(TAG, "No frames captured for processing.")
            return
        }

        val allEmbeddings = mutableListOf<FloatArray>()
        var facesDetectedCount = 0
        var processedFramesForEmbeddingCount = 0

        withContext(Dispatchers.Default) {
            val totalFrames = capturedFrames.size
            val iterator = capturedFrames.iterator()
            var frameIndex = 0

            while (iterator.hasNext()) {
                val bitmap = iterator.next()
                frameIndex++

                _message.value = "Memproses frame..."
                try {
                    val debugFileName = "processed_frame_${frameIndex}_${System.currentTimeMillis()}.jpg"
                    val file = File(getApplication<Application>().getExternalFilesDir(null), debugFileName)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving debug bitmap BEFORE face detection: ${e.message}")
                }

                val detectionResult: FaceDetectorResult? = try {
                    addFaceDetector.detect(bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during face detection for frame ${frameIndex}: ${e.message}", e)
                    null
                }

                if (detectionResult == null || detectionResult.detections().isEmpty()) {
                    Log.w(TAG, "No detection result or no faces detected in frame ${frameIndex}.")
                    bitmap.recycle()
                    iterator.remove()
                    continue
                }

                detectionResult.detections().firstOrNull()?.let { detection ->
                    facesDetectedCount++
                    val originalFaceBox = detection.boundingBox()

                    val expandedFaceBox = ImageCropper.expandBoundingBox(
                        boundingBox = originalFaceBox,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                        expansionFactor = FACE_EXPANSION_FACTOR
                    )

                    var croppedFace: Bitmap? = null
                    try {
                        croppedFace = ImageCropper.cropBitmap(bitmap, expandedFaceBox)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cropping bitmap for frame ${frameIndex}: ${e.message}", e)
                        bitmap.recycle()
                        iterator.remove()
                        return@let
                    }

                    if (croppedFace == null || croppedFace.isRecycled) {
                        Log.e(TAG, "Cropped bitmap is null or recycled for frame ${frameIndex}.")
                        bitmap.recycle()
                        croppedFace?.recycle()
                        iterator.remove()
                        return@let
                    }

                    var resizedFace: Bitmap? = null
                    try {
                        resizedFace = croppedFace.resizeBitmap(TARGET_FACE_SIZE, TARGET_FACE_SIZE)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resizing bitmap for frame ${frameIndex}: ${e.message}", e)
                        croppedFace.recycle()
                        bitmap.recycle()
                        iterator.remove()
                        return@let
                    } finally {
                        croppedFace.recycle()
                    }

                    if (resizedFace == null || resizedFace.isRecycled) {
                        Log.e(TAG, "Resized bitmap is null or recycled for frame ${frameIndex}.")
                        resizedFace?.recycle()
                        bitmap.recycle()
                        iterator.remove()
                        return@let
                    }

                    val embedding = try {
                        faceEmbedder.getEmbeddings(resizedFace)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting embeddings for frame ${frameIndex}: ${e.message}", e)
                        null
                    } finally {
                        resizedFace.recycle()
                    }

                    if (embedding != null) {
                        allEmbeddings.add(embedding)
                    } else {
                        Log.w(TAG, "Embedding is null for frame ${frameIndex}.")
                    }
                    processedFramesForEmbeddingCount++
                    bitmap.recycle()
                    iterator.remove()
                } ?: run {
                    bitmap.recycle()
                    iterator.remove()
                }
            }
            capturedFrames.clear()
        }

        if (allEmbeddings.isNotEmpty()) {
            val averagedEmbedding = averageEmbeddings(allEmbeddings)
            val user = User(
                adminId = currentAdminId.toIntOrNull(),
                name = name.value,
                email = email.value,
                phone = phone.value,
                embeddings = averagedEmbedding,
                role = "admin"
            )
            when (val result = registerUserWithFaceUseCase(user.adminId, user.name, user.email, user.phone, user.embeddings, user.role)) {
                is ApiResult.Success -> {
                    if (result.data) {
                        _message.value = "Wajah berhasil disimpan!"
                        _recordingState.value = RecordingState.DONE
                        Log.i(TAG, "User registration successful.")
                    } else {
                        _message.value = "Gagal menyimpan data wajah di server."
                        _recordingState.value = RecordingState.IDLE
                        Log.e(TAG, "User registration failed on server side (returned false).")
                    }
                }
                is ApiResult.Error -> {
                    _message.value = "Error: ${result.message}"
                    Log.e(TAG, "Error during user registration API call: ${result.message}", result.exception)
                    _recordingState.value = RecordingState.IDLE
                }
                ApiResult.Loading -> { }
            }
            averagedEmbedding.fill(0.0f)
            user.embeddings.fill(0.0f)
            for (emb in allEmbeddings) {
                emb.fill(0.0f)
            }
            allEmbeddings.clear()
        } else {
            _message.value = "Tidak ada wajah yang terdeteksi di frame yang diambil. Terdeteksi ${facesDetectedCount} wajah. Coba lagi."
            _recordingState.value = RecordingState.IDLE
            Log.w(TAG, "No valid embeddings generated from captured frames.")
        }
    }

    private fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return floatArrayOf()
        val embeddingSize = embeddings[0].size
        val sumEmbedding = FloatArray(embeddingSize) { 0f }

        for (embedding in embeddings) {
            for (i in 0 until embeddingSize) {
                sumEmbedding[i] += embedding[i]
            }
        }

        return FloatArray(embeddingSize) { i -> sumEmbedding[i] / embeddings.size }
    }

    fun resetState() {
        recordingJob?.cancel()
        capturedFrames.forEach { it.recycle() }
        capturedFrames.clear()
        _recordingState.value = RecordingState.IDLE
        _recordingProgress.value = 0f
        _message.value = null
        currentAdminId = ""
        name.value = ""
        email.value = ""
        phone.value = ""
        _countdown.value = 5
        Log.d(TAG, "ViewModel state reset.")
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        capturedFrames.forEach { it.recycle() }
        capturedFrames.clear()
        addFaceDetector.close()
        liveFaceDetector?.close()
        faceEmbedder.close()
        Log.d(TAG, "ViewModel cleared. Resources closed.")
    }
}

enum class RecordingState {
    IDLE, RECORDING, PROCESSING, DONE
}