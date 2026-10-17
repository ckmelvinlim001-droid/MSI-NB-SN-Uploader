package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.example.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ImageCompressor {

  private const val MAX_WIDTH = 1600
  private const val JPEG_QUALITY = 80 // Equivalent to canvas 0.8 quality

  suspend fun processImageUri(
    context: Context,
    uri: Uri,
    batchPrefix: String,
    sequenceIndex: Int
  ): PhotoItem? = withContext(Dispatchers.IO) {
    try {
      // 1. Get original size
      val originalSize = getUriFileSize(context, uri)

      // 2. Read EXIF orientation
      val orientation = getExifOrientation(context, uri)

      // 3. Decode bounds first to calculate safe inSampleSize
      val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
      }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }

      val origW = boundsOptions.outWidth
      val origH = boundsOptions.outHeight
      if (origW <= 0 || origH <= 0) return@withContext null

      // Calculate sample size if very large to prevent OOM
      var sampleSize = 1
      while ((origW / sampleSize) > (MAX_WIDTH * 2) || (origH / sampleSize) > (MAX_WIDTH * 2)) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

      val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      } ?: return@withContext null

      // 4. Apply EXIF orientation
      val orientedBitmap = applyOrientation(decodedBitmap, orientation)

      // 5. Scale to max width 1600px maintaining aspect ratio
      val finalBitmap = scaleToMaxWidth(orientedBitmap, MAX_WIDTH)

      // 6. Compress to JPEG 80% (0.8 quality)
      val outputStream = ByteArrayOutputStream()
      finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
      val compressedBytes = outputStream.toByteArray()

      // 7. Base64 encode without data URL prefix (stripping any headers)
      val base64String = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

      // 8. Generate safe, standard filename
      val fileName = generateFileName(batchPrefix, sequenceIndex)

      // 9. Cache locally for fast UI thumbnail rendering and memory optimization
      val cacheDir = File(context.cacheDir, "compressed_photos").apply { mkdirs() }
      val cachedFile = File(cacheDir, "${UUID.randomUUID()}.jpg")
      FileOutputStream(cachedFile).use { fos ->
        fos.write(compressedBytes)
      }

      val item = PhotoItem(
        id = UUID.randomUUID().toString(),
        fileName = fileName,
        originalSize = if (originalSize > 0) originalSize else compressedBytes.size.toLong(),
        compressedSize = compressedBytes.size.toLong(),
        width = finalBitmap.width,
        height = finalBitmap.height,
        base64 = base64String,
        cachedFilePath = cachedFile.absolutePath
      )

      if (orientedBitmap != decodedBitmap) {
        decodedBitmap.recycle()
      }
      if (finalBitmap != orientedBitmap) {
        orientedBitmap.recycle()
      }
      finalBitmap.recycle()

      item
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  suspend fun processFile(
    context: Context,
    file: File,
    batchPrefix: String,
    sequenceIndex: Int
  ): PhotoItem? = withContext(Dispatchers.IO) {
    if (!file.exists()) return@withContext null
    processImageUri(context, Uri.fromFile(file), batchPrefix, sequenceIndex)
  }

  private fun scaleToMaxWidth(bitmap: Bitmap, maxWidth: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    if (width <= maxWidth) {
      return bitmap
    }

    val targetWidth = maxWidth
    val targetHeight = ((height.toDouble() / width.toDouble()) * targetWidth).toInt().coerceAtLeast(1)

    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
  }

  private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      else -> return bitmap
    }
    return try {
      Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: OutOfMemoryError) {
      bitmap
    }
  }

  private fun getExifOrientation(context: Context, uri: Uri): Int {
    return try {
      if (uri.scheme == "file") {
        val exifInterface = ExifInterface(uri.path ?: return ExifInterface.ORIENTATION_NORMAL)
        return exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val exifInterface = ExifInterface(stream)
        exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
      ExifInterface.ORIENTATION_NORMAL
    }
  }

  private fun getUriFileSize(context: Context, uri: Uri): Long {
    return try {
      if (uri.scheme == "file") {
        return File(uri.path ?: return 0L).length()
      }
      val cursor = context.contentResolver.query(uri, null, null, null, null)
      cursor?.use {
        if (it.moveToFirst()) {
          val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
          if (sizeIndex != -1) {
            return it.getLong(sizeIndex)
          }
        }
      }
      0L
    } catch (e: Exception) {
      0L
    }
  }

  fun generateFileName(batchPrefix: String, index: Int): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val cleanPrefix = batchPrefix.trim()
      .replace(Regex("[^a-zA-Z0-9_-]"), "_")
      .take(30)
    val indexFormatted = String.format(Locale.US, "%03d", index)

    return if (cleanPrefix.isNotBlank()) {
      "${cleanPrefix}_${timeStamp}_${indexFormatted}.jpg"
    } else {
      "SN_${timeStamp}_${indexFormatted}.jpg"
    }
  }
}
