package com.example.spark.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import com.example.spark.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
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

            openInputStreamSafe(context, sourceUri)?.use { inputStream ->
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
     * Opens an InputStream for the given URI, handling both content:// and file:// schemes.
     * For file:// URIs, uses FileInputStream directly instead of contentResolver which can fail.
     */
    private fun openInputStreamSafe(context: Context, uri: Uri): InputStream? {
        return when (uri.scheme) {
            "file" -> {
                val path = uri.path ?: return null
                val file = File(path)
                if (file.exists() && file.canRead()) FileInputStream(file) else null
            }
            else -> {
                // content:// or other schemes
                context.contentResolver.openInputStream(uri)
            }
        }
    }

    /**
     * Safely loads an image URI into an ImageView with downsampling and graceful fallback.
     * Guaranteed never to crash if the URI is inaccessible, missing, or corrupted.
     * Handles both file:// and content:// URIs transparently.
     */
    fun loadProfileImage(
        imageView: ImageView,
        uriString: String?,
        fallbackResId: Int = R.drawable.ic_profile_placeholder
    ) {
        if (uriString.isNullOrEmpty()) {
            imageView.setImageResource(fallbackResId)
            return
        }

        try {
            val uri = Uri.parse(uriString)
            val context = imageView.context

            // For file:// URIs, also try loading directly from path if the file exists
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        loadFromFile(imageView, file, fallbackResId)
                        return
                    }
                }
                // File doesn't exist, fall through to fallback
                imageView.setImageResource(fallbackResId)
                return
            }

            // For content:// URIs, use contentResolver
            loadFromContentUri(imageView, uri, context, fallbackResId)
        } catch (t: Throwable) {
            // Fails gracefully without crash on any SecurityException / FileNotFoundException / OOM
            imageView.setImageResource(fallbackResId)
        }
    }

    private fun loadFromFile(imageView: ImageView, file: File, fallbackResId: Int) {
        try {
            // Obtain image dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            FileInputStream(file).use { stream ->
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

            val bitmap = FileInputStream(file).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(fallbackResId)
            }
        } catch (t: Throwable) {
            imageView.setImageResource(fallbackResId)
        }
    }

    private fun loadFromContentUri(imageView: ImageView, uri: Uri, context: Context, fallbackResId: Int) {
        try {
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
            imageView.setImageResource(fallbackResId)
        }
    }
}
