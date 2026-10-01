package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.CameraAiViewModel
import com.example.ui.EditorTab
import com.example.ui.UiState
import com.example.util.ColorGradingSettings
import com.example.util.CropRatio
import com.example.util.FilterPreset
import com.example.util.SocialShareManager
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RadiantPurple
import com.example.ui.theme.SunsetCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EditorScreen(
    viewModel: CameraAiViewModel,
    state: UiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Mandatory BackHandler for navigation
    BackHandler {
        viewModel.navigateTo(AppScreen.CAMERA)
    }

    var lastTapPoint by remember { mutableStateOf<PointF?>(null) }
    var displayedImageBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar: Back, Undo, Redo, Split Compare, Save, Share
            EditorTopBar(
                title = state.photoTitle,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                isSplitActive = state.isSplitSliderActive,
                onBack = { viewModel.navigateTo(AppScreen.CAMERA) },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleSplit = { viewModel.setSplitSliderActive(!state.isSplitSliderActive) },
                onSave = { viewModel.savePhotoToGallery(context) },
                onShare = { viewModel.navigateTo(AppScreen.SHARE) }
            )

            // Main Photo Canvas with Split Slider & Interactive Magic Eraser
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF06080D))
                    .border(BorderStroke(1.dp, Color(0xFF1E2536)), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                val currentBitmap = state.displayedBitmap
                val originalBitmap = state.originalBitmap

                if (currentBitmap != null && originalBitmap != null) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(state.activeEditorTab) {
                                if (state.activeEditorTab == EditorTab.MAGIC_ERASER && !state.isSplitSliderActive) {
                                    detectTapGestures { tapOffset ->
                                        if (displayedImageBounds.width > 0 && displayedImageBounds.height > 0) {
                                            val relX = (tapOffset.x - displayedImageBounds.left) / displayedImageBounds.width
                                            val relY = (tapOffset.y - displayedImageBounds.top) / displayedImageBounds.height
                                            if (relX in 0f..1f && relY in 0f..1f) {
                                                lastTapPoint = PointF(tapOffset.x, tapOffset.y)
                                                viewModel.eraseAtPoint(relX, relY)
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = constraints.maxWidth.toFloat()
                        val canvasHeight = constraints.maxHeight.toFloat()

                        // Calculate aspect fit bounds
                        val imgRatio = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
                        val canvasRatio = canvasWidth / canvasHeight

                        val renderWidth: Float
                        val renderHeight: Float
                        val renderLeft: Float
                        val renderTop: Float

                        if (canvasRatio > imgRatio) {
                            renderHeight = canvasHeight
                            renderWidth = canvasHeight * imgRatio
                            renderLeft = (canvasWidth - renderWidth) / 2f
                            renderTop = 0f
                        } else {
                            renderWidth = canvasWidth
                            renderHeight = canvasWidth / imgRatio
                            renderLeft = 0f
                            renderTop = (canvasHeight - renderHeight) / 2f
                        }

                        displayedImageBounds = Rect(
                            renderLeft,
                            renderTop,
                            renderLeft + renderWidth,
                            renderTop + renderHeight
                        )

                        if (state.isSplitSliderActive) {
                            // Split Slider Before / After side by side
                            val splitX = canvasWidth * state.splitSliderPosition

                            Box(modifier = Modifier.fillMaxSize()) {
                                // Original Image (Left side)
                                Image(
                                    bitmap = originalBitmap.asImageBitmap(),
                                    contentDescription = "Original Photo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawWithContent {
                                            clipRect(left = 0f, top = 0f, right = splitX, bottom = size.height) {
                                                this@drawWithContent.drawContent()
                                            }
                                        }
                                )

                                // Edited Image (Right side)
                                Image(
                                    bitmap = currentBitmap.asImageBitmap(),
                                    contentDescription = "Edited Photo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .drawWithContent {
                                            clipRect(left = splitX, top = 0f, right = size.width, bottom = size.height) {
                                                this@drawWithContent.drawContent()
                                            }
                                        }
                                )

                                // Draggable Divider Line & Knob
                                Box(
                                    modifier = Modifier
                                        .offset(x = (splitX - 1.5.dp.value).dp)
                                        .width(3.dp)
                                        .fillMaxHeight()
                                        .background(NeonCyan)
                                )

                                // Center Drag Handle
                                Box(
                                    modifier = Modifier
                                        .offset(x = (splitX - 20.dp.value).dp)
                                        .align(Alignment.CenterStart)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                        .border(BorderStroke(2.dp, Color.Black), CircleShape)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val newPos = (splitX + dragAmount.x) / canvasWidth
                                                viewModel.updateSplitSliderPosition(newPos)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Compare,
                                        contentDescription = "Drag Divider",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Badges
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.75f)
                                ) {
                                    Text(
                                        text = "ASLI",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.75f)
                                ) {
                                    Text(
                                        text = "AI PRO",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            // Standard Single View with Eraser feedback
                            val bitmapToShow = if (state.isComparingOriginal) originalBitmap else currentBitmap
                            Image(
                                bitmap = bitmapToShow.asImageBitmap(),
                                contentDescription = "Canvas Foto",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Magic Eraser Target Ring animation when tapped
                            if (state.activeEditorTab == EditorTab.MAGIC_ERASER && lastTapPoint != null) {
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = (lastTapPoint!!.x - state.brushRadius / 2f).dp,
                                            y = (lastTapPoint!!.y - state.brushRadius / 2f).dp
                                        )
                                        .size(state.brushRadius.dp)
                                        .border(BorderStroke(2.dp, NeonCyan), CircleShape)
                                        .background(Color(0x3300E5FF), CircleShape)
                                )
                            }

                            // Interactive Crop Frame Overlay
                            if (state.activeEditorTab == EditorTab.CROP_ROTATE && !state.isSplitSliderActive && displayedImageBounds.width > 0) {
                                val density = LocalDensity.current
                                with(density) {
                                    val cLeft = displayedImageBounds.left + displayedImageBounds.width * state.cropLeft
                                    val cTop = displayedImageBounds.top + displayedImageBounds.height * state.cropTop
                                    val cWidth = displayedImageBounds.width * (state.cropRight - state.cropLeft)
                                    val cHeight = displayedImageBounds.height * (state.cropBottom - state.cropTop)

                                    Box(
                                        modifier = Modifier
                                            .offset(x = cLeft.toDp(), y = cTop.toDp())
                                            .size(cWidth.toDp(), cHeight.toDp())
                                            .border(BorderStroke(2.dp, NeonCyan), RoundedCornerShape(2.dp))
                                            .drawBehind {
                                                val gridColor = Color(0x6600E5FF)
                                                drawLine(gridColor, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), strokeWidth = 1.dp.toPx())
                                                drawLine(gridColor, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), strokeWidth = 1.dp.toPx())
                                                drawLine(gridColor, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), strokeWidth = 1.dp.toPx())
                                                drawLine(gridColor, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), strokeWidth = 1.dp.toPx())
                                            }
                                    ) {
                                        Box(modifier = Modifier.align(Alignment.TopStart).size(12.dp).background(NeonCyan))
                                        Box(modifier = Modifier.align(Alignment.TopEnd).size(12.dp).background(NeonCyan))
                                        Box(modifier = Modifier.align(Alignment.BottomStart).size(12.dp).background(NeonCyan))
                                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).background(NeonCyan))
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Processing Overlay with Progress Indicator
                if (state.isAiProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkCardSurface,
                            border = BorderStroke(1.5.dp, NeonCyan),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = NeonCyan,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = state.aiProcessingMessage.ifEmpty { "Memproses dengan AI..." },
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Object removed counter badge (if > 0)
                if (state.objectsRemovedCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, EmeraldGreen)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${state.objectsRemovedCount} Objek Terhapus",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Bottom Tool Navigation Tabs
            EditorTabBar(
                activeTab = state.activeEditorTab,
                onTabSelected = { viewModel.setEditorTab(it) }
            )

            // Bottom Controls Area corresponding to Active Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (state.activeEditorTab) {
                    EditorTab.AUTO_AI -> {
                        AutoAiTabContent(
                            selectedPreset = state.selectedPreset,
                            onAutoAiClick = { viewModel.applyAutoAiEnhance() },
                            onPresetSelected = { viewModel.applyPreset(it) }
                        )
                    }
                    EditorTab.COLOR_GRADE -> {
                        ColorGradingTabContent(
                            colorGrading = state.adjustments.colorGrading,
                            onColorGradingChange = { viewModel.updateColorGrading(it) },
                            onReset = { viewModel.resetColorGrading() }
                        )
                    }
                    EditorTab.CROP_ROTATE -> {
                        CropRotateTabContent(
                            selectedRatio = state.selectedCropRatio,
                            onRatioSelected = { viewModel.selectCropRatio(it) },
                            onApplyCrop = { viewModel.applyCrop() },
                            onRotate = { viewModel.rotatePhoto(90f) },
                            onFlipHorizontal = { viewModel.flipPhoto(horizontal = true, vertical = false) },
                            onFlipVertical = { viewModel.flipPhoto(horizontal = false, vertical = true) },
                            onResetCrop = { viewModel.selectCropRatio(CropRatio.FREE) }
                        )
                    }
                    EditorTab.MAGIC_ERASER -> {
                        MagicEraserTabContent(
                            brushRadius = state.brushRadius,
                            onBrushRadiusChange = { viewModel.setBrushRadius(it) },
                            onOneClickAutoErase = { viewModel.oneClickAutoErase() }
                        )
                    }
                    EditorTab.PRO_ADJUST -> {
                        ProAdjustTabContent(
                            adjustments = state.adjustments,
                            onAdjustmentsChange = { viewModel.updateAdjustments(it) },
                            onReset = { viewModel.resetAdjustments() }
                        )
                    }
                    EditorTab.AI_INSPECTOR -> {
                        AiInspectorTabContent(
                            analysis = state.aiAnalysis,
                            captions = state.aiCaptions,
                            selectedCaption = state.selectedCaptionText,
                            onSelectCaption = { viewModel.selectCaption(it) },
                            onRefreshAnalysis = { viewModel.triggerAiAnalysis() },
                            onCopyCaption = {
                                SocialShareManager.copyCaptionToClipboard(context, state.selectedCaptionText)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorTopBar(
    title: String,
    canUndo: Boolean,
    canRedo: Boolean,
    isSplitActive: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSplit: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("editor_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Kamera",
                    tint = TextPrimary
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(110.dp)
                )
                Text(
                    text = "AuraLens Studio",
                    fontSize = 10.sp,
                    color = NeonCyan
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Urungkan",
                    tint = if (canUndo) TextPrimary else TextMuted
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Ulangi",
                    tint = if (canRedo) TextPrimary else TextMuted
                )
            }

            IconButton(
                onClick = onToggleSplit,
                modifier = Modifier.testTag("split_slider_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = "Bandingkan Slider Asli vs Edit",
                    tint = if (isSplitActive) NeonCyan else TextSecondary
                )
            }

            IconButton(
                onClick = onSave,
                modifier = Modifier.testTag("save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Simpan ke Galeri",
                    tint = AmberGlow
                )
            }

            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("open_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Bagikan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EditorTabBar(
    activeTab: EditorTab,
    onTabSelected: (EditorTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = activeTab.ordinal,
        containerColor = DarkSurfaceVariant,
        contentColor = NeonCyan,
        edgePadding = 8.dp,
        indicator = { tabPositions ->
            if (activeTab.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                    color = NeonCyan,
                    height = 3.dp
                )
            }
        }
    ) {
        EditorTab.values().forEach { tab ->
            val isSelected = activeTab == tab
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonCyan else TextSecondary
                    )
                },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            EditorTab.AUTO_AI -> Icons.Default.AutoAwesome
                            EditorTab.COLOR_GRADE -> Icons.Default.Palette
                            EditorTab.CROP_ROTATE -> Icons.Default.Crop
                            EditorTab.MAGIC_ERASER -> Icons.Default.CleanHands
                            EditorTab.PRO_ADJUST -> Icons.Default.Tune
                            EditorTab.AI_INSPECTOR -> Icons.Default.Insights
                        },
                        contentDescription = tab.title,
                        tint = if (isSelected) NeonCyan else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
fun AutoAiTabContent(
    selectedPreset: FilterPreset,
    onAutoAiClick: () -> Unit,
    onPresetSelected: (FilterPreset) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Prominent 1-Click Auto AI Enhance Button
        Button(
            onClick = onAutoAiClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("auto_ai_enhance_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF5252))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✨ 1-Klik Auto AI Enhance (Tone & Clarity Pro)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Preset Chips
        Column {
            Text(
                text = "Preset Gaya Sinematik & Studio:",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(FilterPreset.values()) { preset ->
                    val isSelected = selectedPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPresetSelected(preset) },
                        label = {
                            Text(
                                text = preset.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkCardSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) NeonCyan else Color(0xFF263044)
                        ),
                        modifier = Modifier.testTag("preset_${preset.name}")
                    )
                }
            }
        }
    }
}

@Composable
fun MagicEraserTabContent(
    brushRadius: Float,
    onBrushRadiusChange: (Float) -> Unit,
    onOneClickAutoErase: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // One-Click Magic Distraction Removal button
        Button(
            onClick = onOneClickAutoErase,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("one_click_erase_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = SunsetCoral
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "⚡ Sekali Klik Hapus Objek Pengganggu",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Manual Brush Size Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Brush,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ukuran Kuas: ${brushRadius.toInt()}px",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.width(115.dp)
            )
            Slider(
                value = brushRadius,
                onValueChange = onBrushRadiusChange,
                valueRange = 15f..80f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0xFF263044)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "💡 Tips: Ketuk atau usap objek apapun pada foto untuk menghapusnya secara mulus menggunakan AI inpainting.",
            fontSize = 10.sp,
            color = TextMuted,
            lineHeight = 13.sp
        )
    }
}

@Composable
fun ProAdjustTabContent(
    adjustments: com.example.util.PhotoAdjustments,
    onAdjustmentsChange: (com.example.util.PhotoAdjustments) -> Unit,
    onReset: () -> Unit
) {
    var selectedAdjustType by remember { mutableStateOf("brightness") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Adjust Category selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                Pair("brightness", "Kecerahan"),
                Pair("contrast", "Kontras"),
                Pair("saturation", "Saturasi"),
                Pair("warmth", "Kehangatan"),
                Pair("vignette", "Vignette")
            )
            items(categories) { (key, label) ->
                val isSelected = selectedAdjustType == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedAdjustType = key },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkCardSurface,
                        labelColor = TextSecondary,
                        selectedContainerColor = RadiantPurple.copy(alpha = 0.25f),
                        selectedLabelColor = NeonCyan
                    )
                )
            }
        }

        // Active slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentValue = when (selectedAdjustType) {
                "brightness" -> adjustments.brightness
                "contrast" -> adjustments.contrast
                "saturation" -> adjustments.saturation
                "warmth" -> adjustments.warmth
                "vignette" -> adjustments.vignette
                else -> 0f
            }

            Text(
                text = "${currentValue.toInt()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                modifier = Modifier.width(42.dp)
            )

            Slider(
                value = currentValue,
                onValueChange = { newVal ->
                    when (selectedAdjustType) {
                        "brightness" -> onAdjustmentsChange(adjustments.copy(brightness = newVal))
                        "contrast" -> onAdjustmentsChange(adjustments.copy(contrast = newVal))
                        "saturation" -> onAdjustmentsChange(adjustments.copy(saturation = newVal))
                        "warmth" -> onAdjustmentsChange(adjustments.copy(warmth = newVal))
                        "vignette" -> onAdjustmentsChange(adjustments.copy(vignette = newVal))
                    }
                },
                valueRange = if (selectedAdjustType == "vignette") 0f..100f else -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0xFF263044)
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onReset) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Nilai",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = "Kustomisasi kurva warna dan eksposur untuk hasil fotografi studio profesional.",
            fontSize = 10.sp,
            color = TextMuted
        )
    }
}

@Composable
fun AiInspectorTabContent(
    analysis: com.example.data.PhotoAiAnalysis?,
    captions: List<com.example.data.GeneratedCaption>,
    selectedCaption: String,
    onSelectCaption: (String) -> Unit,
    onRefreshAnalysis: () -> Unit,
    onCopyCaption: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (analysis != null) {
            // Quality score row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(EmeraldGreen.copy(alpha = 0.2f), CircleShape)
                            .border(BorderStroke(1.dp, EmeraldGreen), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${analysis.overallScore}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Skor Kualitas AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Cahaya: ${analysis.lightingScore}% • Komposisi: ${analysis.compositionScore}%",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onRefreshAnalysis,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analisis Ulang", fontSize = 10.sp, color = NeonCyan)
                }
            }

            Text(
                text = analysis.summaryReview,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 14.sp
            )
        }

        // Social Captions Carousel
        if (captions.isNotEmpty()) {
            Text(
                text = "Pilihan Kepsen & Hashtag Medsos Siap Pakai:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(captions) { item ->
                    val isSelected = selectedCaption == item.caption
                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .clickable { onSelectCaption(item.caption) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkCardSurface
                        ),
                        border = BorderStroke(1.dp, if (isSelected) NeonCyan else Color(0xFF263044))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = item.style,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.caption,
                                fontSize = 10.sp,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorGradingTabContent(
    colorGrading: ColorGradingSettings,
    onColorGradingChange: (ColorGradingSettings) -> Unit,
    onReset: () -> Unit
) {
    var selectedChannel by remember { mutableStateOf("temp_tint") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Mode Selector: Suhu & Tint, RGB Channel, Vibrance & Sepia, Preset Cepat
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                Pair("temp_tint", "Suhu & Tint"),
                Pair("rgb", "Saluran RGB"),
                Pair("vibrance_sepia", "Vibrance & Sepia"),
                Pair("quick_tones", "Preset Warna")
            )
            items(categories) { (key, label) ->
                val isSelected = selectedChannel == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedChannel = key },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkCardSurface,
                        labelColor = TextSecondary,
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan
                    ),
                    modifier = Modifier.testTag("color_channel_$key")
                )
            }
        }

        when (selectedChannel) {
            "temp_tint" -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Temperature slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Suhu (Dingin/Hangat): ${colorGrading.temperature.toInt()}",
                            fontSize = 11.sp,
                            color = if (colorGrading.temperature > 0) AmberGlow else NeonCyan,
                            modifier = Modifier.width(170.dp)
                        )
                        Slider(
                            value = colorGrading.temperature,
                            onValueChange = { onColorGradingChange(colorGrading.copy(temperature = it)) },
                            valueRange = -100f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = if (colorGrading.temperature > 0) AmberGlow else NeonCyan,
                                activeTrackColor = if (colorGrading.temperature > 0) AmberGlow else NeonCyan
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Tint slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tint (Hijau/Magenta): ${colorGrading.tint.toInt()}",
                            fontSize = 11.sp,
                            color = if (colorGrading.tint > 0) RadiantPurple else EmeraldGreen,
                            modifier = Modifier.width(170.dp)
                        )
                        Slider(
                            value = colorGrading.tint,
                            onValueChange = { onColorGradingChange(colorGrading.copy(tint = it)) },
                            valueRange = -100f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = if (colorGrading.tint > 0) RadiantPurple else EmeraldGreen,
                                activeTrackColor = if (colorGrading.tint > 0) RadiantPurple else EmeraldGreen
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            "rgb" -> {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Red Channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Merah: ${colorGrading.redBalance.toInt()}", fontSize = 10.sp, color = SunsetCoral, modifier = Modifier.width(90.dp))
                        Slider(
                            value = colorGrading.redBalance,
                            onValueChange = { onColorGradingChange(colorGrading.copy(redBalance = it)) },
                            valueRange = -100f..100f,
                            colors = SliderDefaults.colors(thumbColor = SunsetCoral, activeTrackColor = SunsetCoral),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Green Channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hijau: ${colorGrading.greenBalance.toInt()}", fontSize = 10.sp, color = EmeraldGreen, modifier = Modifier.width(90.dp))
                        Slider(
                            value = colorGrading.greenBalance,
                            onValueChange = { onColorGradingChange(colorGrading.copy(greenBalance = it)) },
                            valueRange = -100f..100f,
                            colors = SliderDefaults.colors(thumbColor = EmeraldGreen, activeTrackColor = EmeraldGreen),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Blue Channel
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Biru: ${colorGrading.blueBalance.toInt()}", fontSize = 10.sp, color = NeonCyan, modifier = Modifier.width(90.dp))
                        Slider(
                            value = colorGrading.blueBalance,
                            onValueChange = { onColorGradingChange(colorGrading.copy(blueBalance = it)) },
                            valueRange = -100f..100f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            "vibrance_sepia" -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrance Warna: ${colorGrading.vibrance.toInt()}", fontSize = 11.sp, color = NeonCyan, modifier = Modifier.width(160.dp))
                        Slider(
                            value = colorGrading.vibrance,
                            onValueChange = { onColorGradingChange(colorGrading.copy(vibrance = it)) },
                            valueRange = -100f..100f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sepia Vintage: ${colorGrading.sepia.toInt()}%", fontSize = 11.sp, color = AmberGlow, modifier = Modifier.width(160.dp))
                        Slider(
                            value = colorGrading.sepia,
                            onValueChange = { onColorGradingChange(colorGrading.copy(sepia = it)) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = AmberGlow, activeTrackColor = AmberGlow),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            "quick_tones" -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickTones = listOf(
                        Triple("Warm Sunset", ColorGradingSettings(temperature = 30f, redBalance = 20f, vibrance = 15f), AmberGlow),
                        Triple("Cool Nordic", ColorGradingSettings(temperature = -30f, blueBalance = 25f, vibrance = 10f), NeonCyan),
                        Triple("Teal & Orange", ColorGradingSettings(temperature = 20f, blueBalance = 25f, redBalance = 20f, tint = -15f), SunsetCoral),
                        Triple("Emerald Lush", ColorGradingSettings(greenBalance = 35f, tint = -25f, vibrance = 20f), EmeraldGreen),
                        Triple("Pastel Dream", ColorGradingSettings(vibrance = -15f, tint = 20f, temperature = 10f), RadiantPurple),
                        Triple("Vintage Sepia", ColorGradingSettings(sepia = 65f, temperature = 25f), Color(0xFFD7CCC8))
                    )
                    items(quickTones) { (name, setting, color) ->
                        Button(
                            onClick = { onColorGradingChange(setting) },
                            colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f), contentColor = color),
                            border = BorderStroke(1.dp, color),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("tone_${name.lowercase().replace(' ', '_')}")
                        ) {
                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tuning warna presisi untuk estetika fotografi sinematik.",
                fontSize = 10.sp,
                color = TextMuted
            )
            IconButton(onClick = onReset, modifier = Modifier.size(28.dp).testTag("reset_color_button")) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Warna",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CropRotateTabContent(
    selectedRatio: CropRatio,
    onRatioSelected: (CropRatio) -> Unit,
    onApplyCrop: () -> Unit,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onResetCrop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Ratio Chips
        Column {
            Text(
                text = "Pilih Rasio Aspek Potongan:",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CropRatio.values()) { ratio ->
                    val isSelected = selectedRatio == ratio
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRatioSelected(ratio) },
                        label = {
                            Text(
                                text = ratio.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkCardSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) NeonCyan else Color(0xFF263044)
                        ),
                        modifier = Modifier.testTag("crop_ratio_${ratio.name.lowercase()}")
                    )
                }
            }
        }

        // Action Buttons Row: Terapkan Potong, Putar 90°, Cermin Horizontal, Balik Vertikal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onApplyCrop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(42.dp)
                    .testTag("apply_crop_button"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Crop,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Terapkan Potong",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onRotate,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF323D52)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("rotate_button"),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AmberGlow
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Putar 90°", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onFlipHorizontal,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF323D52)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier
                    .weight(0.9f)
                    .height(42.dp)
                    .testTag("flip_h_button"),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flip,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = RadiantPurple
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cermin", fontSize = 11.sp)
            }
        }

        Text(
            text = "💡 Sesuaikan rasio crop lalu tekan 'Terapkan Potong' untuk memotong foto.",
            fontSize = 10.sp,
            color = TextMuted
        )
    }
}
