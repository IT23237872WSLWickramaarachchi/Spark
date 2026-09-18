package com.example.spark.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.example.spark.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object ProfileImageHelper {

    /**
     * Safely copies an image from an external content URI into the app's internal private storage.
     * This guarantees that the profile picture persists across reboots and permission revocations.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri, userId: Long): Uri? {
        return try {
            val profilesDir = File(context.filesDir, "profiles").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(profilesDir, "user_${userId}_avatar.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Safely loads an image URI into an ImageView with downsampling and graceful fallback.
     * Guaranteed never to crash if the URI is inaccessible, missing, or corrupted.
     */
    fun loadProfileImage(
        imageView: ImageView,
        uriString: String?,
        fallbackResId: Int = R.drawable.sample_avatar
    ) {
        if (uriString.isNullOrEmpty()) {
            imageView.setImageResource(fallbackResId)
            return
        }

        try {
            val uri = Uri.parse(uriString)
            val context = imageView.context

            // Obtain image dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                imageView.setImageResource(fallbackResId)
                return
            }

            // Downsample to target size (~160x160 px for avatar)
            val targetSize = 160
            val sampleSize = max(1, max(options.outWidth / targetSize, options.outHeight / targetSize))

            val decodeOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(fallbackResId)
            }
        } catch (t: Throwable) {
            // Fails gracefully without crash on any SecurityException / FileNotFoundException / OOM
            imageView.setImageResource(fallbackResId)
        }
    }
}
