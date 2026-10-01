package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GeminiPhotoService
import com.example.data.PhotoAiAnalysis
import com.example.data.PhotoEntity
import com.example.data.GeneratedCaption
import com.example.data.SamplePhotoItem
import com.example.util.ColorGradingSettings
import com.example.util.CropRatio
import com.example.util.ExportAspectRatio
import com.example.util.FilterPreset
import com.example.util.ImageProcessingUtils
import com.example.util.PhotoAdjustments
import com.example.util.SocialPlatform
import com.example.util.SocialShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    CAMERA,
    EDITOR,
    SHARE,
    GALLERY
}

enum class EditorTab(val title: String) {
    AUTO_AI("Auto AI"),
    COLOR_GRADE("Warna"),
    CROP_ROTATE("Potong"),
    MAGIC_ERASER("Magic Eraser"),
    PRO_ADJUST("Atur Pro"),
    AI_INSPECTOR("Analisis")
}

data class UiState(
    val currentScreen: AppScreen = AppScreen.CAMERA,
    val activeEditorTab: EditorTab = EditorTab.AUTO_AI,
    val originalBitmap: Bitmap? = null,
    val baseBitmap: Bitmap? = null,           // After eraser strokes, before color grading
    val displayedBitmap: Bitmap? = null,      // Full render with preset & adjustments
    val photoTitle: String = "Foto AI",
    val selectedPreset: FilterPreset = FilterPreset.ORIGINAL,
    val adjustments: PhotoAdjustments = PhotoAdjustments(),
    val selectedCropRatio: CropRatio = CropRatio.FREE,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val isComparingOriginal: Boolean = false,
    val splitSliderPosition: Float = 0.5f,
    val isSplitSliderActive: Boolean = false,
    val objectsRemovedCount: Int = 0,
    val brushRadius: Float = 40f,
    val isAiProcessing: Boolean = false,
    val aiProcessingMessage: String = "",
    val aiAnalysis: PhotoAiAnalysis? = null,
    val aiCaptions: List<GeneratedCaption> = emptyList(),
    val selectedCaptionText: String = "",
    val shareAspectRatio: ExportAspectRatio = ExportAspectRatio.ORIGINAL,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val toastMessage: String? = null
)

class CameraAiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val photoDao = db.photoDao()

    val savedPhotos: StateFlow<List<PhotoEntity>> = photoDao.getAllPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<Bitmap>()
    private val redoStack = mutableListOf<Bitmap>()

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setEditorTab(tab: EditorTab) {
        _uiState.update { it.copy(activeEditorTab = tab) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun setSplitSliderActive(active: Boolean) {
        _uiState.update { it.copy(isSplitSliderActive = active) }
    }

    fun updateSplitSliderPosition(pos: Float) {
        _uiState.update { it.copy(splitSliderPosition = pos.coerceIn(0.05f, 0.95f)) }
    }

    fun toggleCompare(showOriginal: Boolean) {
        _uiState.update { it.copy(isComparingOriginal = showOriginal) }
    }

    fun setBrushRadius(radius: Float) {
        _uiState.update { it.copy(brushRadius = radius) }
    }

    fun loadSample(context: Context, sample: SamplePhotoItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiProcessingMessage = "Memuat ${sample.title}..."
                )
            }
            val bitmap = ImageProcessingUtils.decodeBitmapFromResource(context, sample.resId)
            withContext(Dispatchers.Main) {
                initNewPhoto(bitmap, sample.title)
            }
        }
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiProcessingMessage = "Mengimpor foto & mendeteksi resolusi..."
                )
            }
            val bitmap = ImageProcessingUtils.decodeBitmapFromUri(context, uri)
            withContext(Dispatchers.Main) {
                if (bitmap != null) {
                    initNewPhoto(bitmap, "Foto Kamera")
                } else {
                    _uiState.update {
                        it.copy(
                            isAiProcessing = false,
                            toastMessage = "Gagal memuat gambar dari galeri."
                        )
                    }
                }
            }
        }
    }

    fun loadSavedPhoto(context: Context, entity: PhotoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiProcessingMessage = "Membuka karya tersimpan..."
                )
            }
            val uri = Uri.parse(entity.filePath)
            val bitmap = ImageProcessingUtils.decodeBitmapFromUri(context, uri)
            withContext(Dispatchers.Main) {
                if (bitmap != null) {
                    initNewPhoto(bitmap, entity.title)
                } else {
                    _uiState.update {
                        it.copy(
                            isAiProcessing = false,
                            toastMessage = "Berkas foto tidak ditemukan."
                        )
                    }
                }
            }
        }
    }

    private fun initNewPhoto(bitmap: Bitmap, title: String) {
        undoStack.clear()
        redoStack.clear()
        val copyOrig = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val copyBase = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        _uiState.update {
            it.copy(
                currentScreen = AppScreen.EDITOR,
                activeEditorTab = EditorTab.AUTO_AI,
                originalBitmap = copyOrig,
                baseBitmap = copyBase,
                displayedBitmap = copyBase,
                photoTitle = title,
                selectedPreset = FilterPreset.ORIGINAL,
                adjustments = PhotoAdjustments(),
                isComparingOriginal = false,
                isSplitSliderActive = false,
                objectsRemovedCount = 0,
                isAiProcessing = false,
                canUndo = false,
                canRedo = false,
                aiAnalysis = null,
                aiCaptions = emptyList(),
                selectedCaptionText = SocialShareManager.DEFAULT_CAPTIONS.first()
            )
        }
        // Auto trigger AI analysis in background
        triggerAiAnalysis()
    }

    fun applyPreset(preset: FilterPreset) {
        val base = _uiState.value.baseBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val updated = ImageProcessingUtils.applyFilterAndAdjustments(
                base,
                preset,
                _uiState.value.adjustments
            )
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        selectedPreset = preset,
                        displayedBitmap = updated
                    )
                }
            }
        }
    }

    fun applyAutoAiEnhance() {
        val base = _uiState.value.baseBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiProcessingMessage = "Auto AI Menganalisis pencahayaan & warna..."
                )
            }
            // Auto AI applies preset AUTO_AI + smart optimal adjustments
            val smartAdjust = PhotoAdjustments(
                brightness = 5f,
                contrast = 12f,
                saturation = 15f,
                warmth = 4f,
                sharpness = 20f,
                vignette = 10f
            )
            val enhanced = ImageProcessingUtils.applyFilterAndAdjustments(
                base,
                FilterPreset.AUTO_AI,
                smartAdjust
            )
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        selectedPreset = FilterPreset.AUTO_AI,
                        adjustments = smartAdjust,
                        displayedBitmap = enhanced,
                        toastMessage = "✨ Kualitas foto disempurnakan otomatis oleh AI!"
                    )
                }
            }
        }
    }

    fun updateAdjustments(newAdjustments: PhotoAdjustments) {
        val base = _uiState.value.baseBitmap ?: return
        _uiState.update { it.copy(adjustments = newAdjustments) }
        viewModelScope.launch(Dispatchers.Default) {
            val updated = ImageProcessingUtils.applyFilterAndAdjustments(
                base,
                _uiState.value.selectedPreset,
                newAdjustments
            )
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(displayedBitmap = updated) }
            }
        }
    }

    fun resetAdjustments() {
        updateAdjustments(PhotoAdjustments())
    }

    fun updateColorGrading(colorGrading: ColorGradingSettings) {
        val currentAdjustments = _uiState.value.adjustments
        updateAdjustments(currentAdjustments.copy(colorGrading = colorGrading))
    }

    fun resetColorGrading() {
        val currentAdjustments = _uiState.value.adjustments
        updateAdjustments(currentAdjustments.copy(colorGrading = ColorGradingSettings()))
    }

    fun selectCropRatio(ratio: CropRatio) {
        val base = _uiState.value.baseBitmap ?: return
        val w = base.width.toFloat()
        val h = base.height.toFloat()

        if (ratio == CropRatio.FREE || ratio.ratioX == 0f || ratio.ratioY == 0f) {
            _uiState.update {
                it.copy(
                    selectedCropRatio = ratio,
                    cropLeft = 0f,
                    cropTop = 0f,
                    cropRight = 1f,
                    cropBottom = 1f
                )
            }
            return
        }

        val targetAspect = ratio.ratioX / ratio.ratioY
        val currentAspect = w / h

        var newLeft = 0f
        var newTop = 0f
        var newRight = 1f
        var newBottom = 1f

        if (targetAspect > currentAspect) {
            val fractionHeight = currentAspect / targetAspect
            val diff = ((1f - fractionHeight) / 2f).coerceIn(0f, 0.45f)
            newTop = diff
            newBottom = 1f - diff
        } else {
            val fractionWidth = targetAspect / currentAspect
            val diff = ((1f - fractionWidth) / 2f).coerceIn(0f, 0.45f)
            newLeft = diff
            newRight = 1f - diff
        }

        _uiState.update {
            it.copy(
                selectedCropRatio = ratio,
                cropLeft = newLeft.coerceIn(0f, 0.45f),
                cropTop = newTop.coerceIn(0f, 0.45f),
                cropRight = newRight.coerceIn(0.55f, 1f),
                cropBottom = newBottom.coerceIn(0.55f, 1f)
            )
        }
    }

    fun updateCropBounds(left: Float, top: Float, right: Float, bottom: Float) {
        _uiState.update {
            it.copy(
                cropLeft = left.coerceIn(0f, right - 0.1f),
                cropTop = top.coerceIn(0f, bottom - 0.1f),
                cropRight = right.coerceIn(left + 0.1f, 1f),
                cropBottom = bottom.coerceIn(top + 0.1f, 1f)
            )
        }
    }

    fun applyCrop() {
        val base = _uiState.value.baseBitmap ?: return
        val orig = _uiState.value.originalBitmap ?: return
        val left = _uiState.value.cropLeft
        val top = _uiState.value.cropTop
        val right = _uiState.value.cropRight
        val bottom = _uiState.value.cropBottom

        if (left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f) {
            _uiState.update { it.copy(toastMessage = "Ukuran potongan sama dengan foto asli.") }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(isAiProcessing = true, aiProcessingMessage = "Memotong foto...")
            }
            undoStack.add(base.copy(Bitmap.Config.ARGB_8888, true))
            redoStack.clear()

            val croppedBase = ImageProcessingUtils.cropBitmap(base, left, top, right, bottom)
            val croppedOrig = ImageProcessingUtils.cropBitmap(orig, left, top, right, bottom)

            val updatedDisplay = ImageProcessingUtils.applyFilterAndAdjustments(
                croppedBase,
                _uiState.value.selectedPreset,
                _uiState.value.adjustments
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        baseBitmap = croppedBase,
                        originalBitmap = croppedOrig,
                        displayedBitmap = updatedDisplay,
                        cropLeft = 0f,
                        cropTop = 0f,
                        cropRight = 1f,
                        cropBottom = 1f,
                        canUndo = true,
                        canRedo = false,
                        toastMessage = "✂️ Foto berhasil dipotong!"
                    )
                }
            }
        }
    }

    fun rotatePhoto(degrees: Float = 90f) {
        val base = _uiState.value.baseBitmap ?: return
        val orig = _uiState.value.originalBitmap ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(isAiProcessing = true, aiProcessingMessage = "Memutar foto...")
            }
            undoStack.add(base.copy(Bitmap.Config.ARGB_8888, true))
            redoStack.clear()

            val rotatedBase = ImageProcessingUtils.rotateBitmap(base, degrees)
            val rotatedOrig = ImageProcessingUtils.rotateBitmap(orig, degrees)

            val updatedDisplay = ImageProcessingUtils.applyFilterAndAdjustments(
                rotatedBase,
                _uiState.value.selectedPreset,
                _uiState.value.adjustments
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        baseBitmap = rotatedBase,
                        originalBitmap = rotatedOrig,
                        displayedBitmap = updatedDisplay,
                        canUndo = true,
                        canRedo = false,
                        toastMessage = "🔄 Foto diputar $degrees°"
                    )
                }
            }
        }
    }

    fun flipPhoto(horizontal: Boolean, vertical: Boolean) {
        val base = _uiState.value.baseBitmap ?: return
        val orig = _uiState.value.originalBitmap ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(isAiProcessing = true, aiProcessingMessage = "Membalik foto...")
            }
            undoStack.add(base.copy(Bitmap.Config.ARGB_8888, true))
            redoStack.clear()

            val flippedBase = ImageProcessingUtils.flipBitmap(base, horizontal, vertical)
            val flippedOrig = ImageProcessingUtils.flipBitmap(orig, horizontal, vertical)

            val updatedDisplay = ImageProcessingUtils.applyFilterAndAdjustments(
                flippedBase,
                _uiState.value.selectedPreset,
                _uiState.value.adjustments
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        baseBitmap = flippedBase,
                        originalBitmap = flippedOrig,
                        displayedBitmap = updatedDisplay,
                        canUndo = true,
                        canRedo = false,
                        toastMessage = "🪞 Foto dibalik"
                    )
                }
            }
        }
    }

    /**
     * One-Click Magic Eraser:
     * Automatically scans the frame, locates unwanted distractions or photobombers,
     * and inpaint-erases them with 1 single click!
     */
    fun oneClickAutoErase() {
        val base = _uiState.value.baseBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiProcessingMessage = "Magic Eraser mendeteksi & menghapus objek pengganggu..."
                )
            }
            // Push to undo stack
            undoStack.add(base.copy(Bitmap.Config.ARGB_8888, true))
            redoStack.clear()

            val (erasedBase, count) = ImageProcessingUtils.oneClickAutoRemoveDistractions(base)
            val updatedDisplay = ImageProcessingUtils.applyFilterAndAdjustments(
                erasedBase,
                _uiState.value.selectedPreset,
                _uiState.value.adjustments
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        baseBitmap = erasedBase,
                        displayedBitmap = updatedDisplay,
                        objectsRemovedCount = it.objectsRemovedCount + count,
                        canUndo = true,
                        canRedo = false,
                        toastMessage = "✨ Berhasil menghapus $count objek pengganggu dalam sekali klik!"
                    )
                }
            }
        }
    }

    /**
     * Interactive Brush / Point Magic Eraser:
     * Erases whatever specific object the user taps or brushes on the canvas!
     */
    fun eraseAtPoint(xRatio: Float, yRatio: Float) {
        val base = _uiState.value.baseBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiProcessingMessage = "Menghapus objek terpilih..."
                )
            }
            undoStack.add(base.copy(Bitmap.Config.ARGB_8888, true))
            redoStack.clear()

            val targetX = xRatio * base.width
            val targetY = yRatio * base.height
            // Scale brush radius relative to image dimension
            val radiusPx = (base.width.toFloat() / 360f) * _uiState.value.brushRadius

            val erasedBase = ImageProcessingUtils.eraseAtPoint(
                base,
                PointF(targetX, targetY),
                radiusPx
            )

            val updatedDisplay = ImageProcessingUtils.applyFilterAndAdjustments(
                erasedBase,
                _uiState.value.selectedPreset,
                _uiState.value.adjustments
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        baseBitmap = erasedBase,
                        displayedBitmap = updatedDisplay,
                        objectsRemovedCount = it.objectsRemovedCount + 1,
                        canUndo = true,
                        canRedo = false,
                        toastMessage = "✨ Objek berhasil dihapus & di-blend secara mulus!"
                    )
                }
            }
        }
    }

    fun eraseWithCustomMask(maskBitmap: Bitmap) {
        val base = _uiState.value.baseBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiProcessingMessage = "Magic Inpainting menghapus goresan objek..."
                )
            }
            undoStack.add(base.copy(Bitmap.Config.ARGB_8888, true))
            redoStack.clear()

            val scaledMask = if (maskBitmap.width != base.width || maskBitmap.height != base.height) {
                Bitmap.createScaledBitmap(maskBitmap, base.width, base.height, true)
            } else {
                maskBitmap
            }

            val erasedBase = ImageProcessingUtils.inpaintErase(base, scaledMask)
            val updatedDisplay = ImageProcessingUtils.applyFilterAndAdjustments(
                erasedBase,
                _uiState.value.selectedPreset,
                _uiState.value.adjustments
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        baseBitmap = erasedBase,
                        displayedBitmap = updatedDisplay,
                        objectsRemovedCount = it.objectsRemovedCount + 1,
                        canUndo = true,
                        canRedo = false,
                        toastMessage = "✨ Area objek berhasil dihapus rapi!"
                    )
                }
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val currentBase = _uiState.value.baseBitmap ?: return
        redoStack.add(currentBase)
        val previousBase = undoStack.removeAt(undoStack.size - 1)

        viewModelScope.launch(Dispatchers.Default) {
            val updated = ImageProcessingUtils.applyFilterAndAdjustments(
                previousBase,
                _uiState.value.selectedPreset,
                _uiState.value.adjustments
            )
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        baseBitmap = previousBase,
                        displayedBitmap = updated,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = true,
                        toastMessage = "Tindakan diurungkan"
                    )
                }
            }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val currentBase = _uiState.value.baseBitmap ?: return
        undoStack.add(currentBase)
        val nextBase = redoStack.removeAt(redoStack.size - 1)

        viewModelScope.launch(Dispatchers.Default) {
            val updated = ImageProcessingUtils.applyFilterAndAdjustments(
                nextBase,
                _uiState.value.selectedPreset,
                _uiState.value.adjustments
            )
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        baseBitmap = nextBase,
                        displayedBitmap = updated,
                        canUndo = true,
                        canRedo = redoStack.isNotEmpty(),
                        toastMessage = "Tindakan diulang"
                    )
                }
            }
        }
    }

    fun triggerAiAnalysis() {
        val bitmap = _uiState.value.displayedBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = GeminiPhotoService.analyzePhotoAesthetics(bitmap)
            val captionsResult = GeminiPhotoService.generateSocialCaptions(bitmap)
            withContext(Dispatchers.Main) {
                result.onSuccess { analysis ->
                    _uiState.update { it.copy(aiAnalysis = analysis) }
                }
                captionsResult.onSuccess { captions ->
                    _uiState.update {
                        it.copy(
                            aiCaptions = captions,
                            selectedCaptionText = captions.firstOrNull()?.caption ?: it.selectedCaptionText
                        )
                    }
                }
            }
        }
    }

    fun selectCaption(caption: String) {
        _uiState.update { it.copy(selectedCaptionText = caption) }
    }

    fun setShareAspectRatio(ratio: ExportAspectRatio) {
        _uiState.update { it.copy(shareAspectRatio = ratio) }
    }

    fun savePhotoToGallery(context: Context, onSaved: (Long) -> Unit = {}) {
        val bitmap = _uiState.value.displayedBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isAiProcessing = true,
                    aiProcessingMessage = "Menyimpan foto kualitas tinggi..."
                )
            }
            val filePath = ImageProcessingUtils.saveBitmapToInternalStorage(context, bitmap)
            val origPath = _uiState.value.originalBitmap?.let {
                ImageProcessingUtils.saveBitmapToInternalStorage(context, it, "orig")
            }

            val entity = PhotoEntity(
                filePath = filePath,
                originalFilePath = origPath,
                title = _uiState.value.photoTitle,
                presetApplied = _uiState.value.selectedPreset.displayName,
                objectRemovedCount = _uiState.value.objectsRemovedCount,
                aiCaption = _uiState.value.selectedCaptionText
            )
            val id = photoDao.insertPhoto(entity)

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isAiProcessing = false,
                        toastMessage = "📸 Foto berhasil disimpan ke Galeri AuraLens!"
                    )
                }
                onSaved(id)
            }
        }
    }

    fun deletePhoto(entity: PhotoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            photoDao.deletePhoto(entity)
        }
    }

    fun shareCurrentPhoto(context: Context, platform: SocialPlatform) {
        val bitmap = _uiState.value.displayedBitmap ?: return
        SocialShareManager.shareImage(
            context = context,
            bitmap = bitmap,
            caption = _uiState.value.selectedCaptionText,
            platform = platform,
            aspectRatio = _uiState.value.shareAspectRatio
        )
    }
}
