package com.example.model

enum class UploadStatus {
  QUEUED,
  UPLOADING,
  UPLOADED,
  FAILED
}

data class PhotoItem(
  val id: String,
  val fileName: String,
  val originalSize: Long,
  val compressedSize: Long,
  val width: Int,
  val height: Int,
  val base64: String,
  val cachedFilePath: String,
  val status: UploadStatus = UploadStatus.QUEUED,
  val statusMessage: String? = null,
  val driveFileId: String? = null,
  val driveFileUrl: String? = null,
  val createdAt: Long = System.currentTimeMillis()
) {
  val compressionRatioPercent: Int
    get() {
      if (originalSize <= 0) return 0
      val saved = originalSize - compressedSize
      return if (saved > 0) ((saved.toDouble() / originalSize.toDouble()) * 100).toInt() else 0
    }
}

data class QueueSummary(
  val total: Int = 0,
  val queued: Int = 0,
  val uploading: Int = 0,
  val uploaded: Int = 0,
  val failed: Int = 0
) {
  val isComplete: Boolean
    get() = total > 0 && uploaded == total

  val progressPercent: Float
    get() = if (total > 0) uploaded.toFloat() / total.toFloat() else 0f
}
