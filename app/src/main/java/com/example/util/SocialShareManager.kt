package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max
import kotlin.math.min

enum class SocialPlatform(
    val title: String,
    val packageName: String?,
    val description: String
) {
    SYSTEM_SHARE("Semua Aplikasi", null, "Bagikan ke aplikasi apa pun"),
    INSTAGRAM_FEED("Instagram Feed", "com.instagram.android", "Format Feed 1:1 atau 4:5"),
    INSTAGRAM_STORIES("Instagram Stories", "com.instagram.android", "Format Story 9:16 vertikal"),
    WHATSAPP("WhatsApp", "com.whatsapp", "Kirim foto HD langsung ke chat/status"),
    TIKTOK("TikTok", "com.zhiliaoapp.musically", "Bagikan ke TikTok Photo Mode"),
    TWITTER_X("X (Twitter)", "com.twitter.android", "Posting tweet foto berkualitas tinggi")
}

enum class ExportAspectRatio(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    ORIGINAL("Asli", 0f, 0f),
    SQUARE_1_1("1:1 (Insta)", 1f, 1f),
    PORTRAIT_4_5("4:5 (Portrait)", 4f, 5f),
    STORY_9_16("9:16 (Story)", 9f, 16f)
}

object SocialShareManager {

    val DEFAULT_CAPTIONS = listOf(
        "✨ Diedit sempurna dengan AuraLens AI Camera! Detail tajam, warna sinematik, dan objek terhapus bersih dalam sekali klik. 📸💫 #AuraLensAI #MobilePhotography #AIEnhance #CreativeVisuals",
        "Golden vibes & cinematic tones. Perfected in seconds with @AuraLensAI 🌟 #PhotographyOfTheDay #VibeCheck #ProCamera #Visuals",
        "No distractions, just pure moments. Magic Eraser by AuraLens AI ✨👀 #BeforeAndAfter #MagicEraser #MinimalPhotography #TravelVibes",
        "Mastering mobile photography with next-gen AI processing. 🚀📷 #ShotOnMobile #ColorGrading #PhotoArt"
    )

    fun copyCaptionToClipboard(context: Context, caption: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AuraLens Caption", caption)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Caption & hashtag berhasil disalin!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Resizes and frames a bitmap to match standard social media aspect ratios (with clean matte canvas)
     */
    fun formatBitmapForSocial(bitmap: Bitmap, ratio: ExportAspectRatio): Bitmap {
        if (ratio == ExportAspectRatio.ORIGINAL) return bitmap

        val targetRatio = ratio.ratioWidth / ratio.ratioHeight
        val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        var canvasWidth: Int
        var canvasHeight: Int

        if (targetRatio > currentRatio) {
            // Need wider canvas
            canvasHeight = bitmap.height
            canvasWidth = (bitmap.height * targetRatio).toInt()
        } else {
            // Need taller canvas
            canvasWidth = bitmap.width
            canvasHeight = (bitmap.width / targetRatio).toInt()
        }

        val formatted = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(formatted)
        canvas.drawColor(Color.BLACK)

        val left = (canvasWidth - bitmap.width) / 2f
        val top = (canvasHeight - bitmap.height) / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, left, top, paint)

        return formatted
    }

    fun shareImage(
        context: Context,
        bitmap: Bitmap,
        caption: String,
        platform: SocialPlatform,
        aspectRatio: ExportAspectRatio = ExportAspectRatio.ORIGINAL
    ) {
        try {
            val formattedBitmap = formatBitmapForSocial(bitmap, aspectRatio)
            val file = ImageProcessingUtils.saveBitmapToSharedCache(context, formattedBitmap)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (platform.packageName != null) {
                // Check if specific app is installed
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(platform.packageName)
                if (launchIntent != null) {
                    shareIntent.setPackage(platform.packageName)
                    context.startActivity(shareIntent)
                    return
                } else {
                    Toast.makeText(
                        context,
                        "${platform.title} belum terpasang, membuka pemilih aplikasi...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan karya dengan AuraLens AI")
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membagikan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
