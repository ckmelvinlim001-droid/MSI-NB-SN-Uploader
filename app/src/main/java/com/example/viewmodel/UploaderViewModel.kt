package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PhotoItem
import com.example.model.QueueSummary
import com.example.model.UploadStatus
import com.example.network.DriveUploader
import com.example.network.UploadResult
import com.example.util.ImageCompressor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class UploaderViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs = application.getSharedPreferences("sn_uploader_prefs", Context.MODE_PRIVATE)

  private val _batchPrefix = MutableStateFlow(prefs.getString("batch_prefix", "") ?: "")
  val batchPrefix: StateFlow<String> = _batchPrefix.asStateFlow()

  private val _webAppUrl = MutableStateFlow(
    prefs.getString("web_app_url", "YOUR_WEB_APP_URL_HERE") ?: "YOUR_WEB_APP_URL_HERE"
  )
  val webAppUrl: StateFlow<String> = _webAppUrl.asStateFlow()

  private val _photoQueue = MutableStateFlow<List<PhotoItem>>(emptyList())
  val photoQueue: StateFlow<List<PhotoItem>> = _photoQueue.asStateFlow()

  private val _isProcessingImages = MutableStateFlow(false)
  val isProcessingImages: StateFlow<Boolean> = _isProcessingImages.asStateFlow()

  private val _isUploading = MutableStateFlow(false)
  val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

  private val _currentUploadingId = MutableStateFlow<String?>(null)
  val currentUploadingId: StateFlow<String?> = _currentUploadingId.asStateFlow()

  private val _selectedPhotoForPreview = MutableStateFlow<PhotoItem?>(null)
  val selectedPhotoForPreview: StateFlow<PhotoItem?> = _selectedPhotoForPreview.asStateFlow()

  private val _showSettingsDialog = MutableStateFlow(false)
  val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

  private val _statusAlert = MutableStateFlow<String?>(null)
  val statusAlert: StateFlow<String?> = _statusAlert.asStateFlow()

  private val _simulateIfPlaceholder = MutableStateFlow(true)
  val simulateIfPlaceholder: StateFlow<Boolean> = _simulateIfPlaceholder.asStateFlow()

  private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
  val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

  fun login() {
    _isLoggedIn.value = true
    prefs.edit().putBoolean("is_logged_in", true).apply()
  }

  fun logout() {
    _isLoggedIn.value = false
    prefs.edit().putBoolean("is_logged_in", false).apply()
  }

  private var uploadJob: Job? = null
  private var nextSequenceIndex = 1

  val summary: StateFlow<QueueSummary> = _photoQueue.map { list ->
    QueueSummary(
      total = list.size,
      queued = list.count { it.status == UploadStatus.QUEUED },
      uploading = list.count { it.status == UploadStatus.UPLOADING },
      uploaded = list.count { it.status == UploadStatus.UPLOADED },
      failed = list.count { it.status == UploadStatus.FAILED }
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, QueueSummary())

  fun setBatchPrefix(prefix: String) {
    _batchPrefix.value = prefix
    prefs.edit().putString("batch_prefix", prefix).apply()
  }

  fun setWebAppUrl(url: String) {
    _webAppUrl.value = url.trim()
    prefs.edit().putString("web_app_url", url.trim()).apply()
  }

  fun setSimulateIfPlaceholder(enable: Boolean) {
    _simulateIfPlaceholder.value = enable
  }

  fun setShowSettingsDialog(show: Boolean) {
    _showSettingsDialog.value = show
  }

  fun setSelectedPhotoForPreview(photo: PhotoItem?) {
    _selectedPhotoForPreview.value = photo
  }

  fun clearStatusAlert() {
    _statusAlert.value = null
  }

  fun processSelectedUris(context: Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _isProcessingImages.value = true
      val currentPrefix = _batchPrefix.value
      val newItems = mutableListOf<PhotoItem>()

      for (uri in uris) {
        val item = ImageCompressor.processImageUri(
          context = context,
          uri = uri,
          batchPrefix = currentPrefix,
          sequenceIndex = nextSequenceIndex++
        )
        if (item != null) {
          newItems.add(item)
        }
      }

      if (newItems.isNotEmpty()) {
        _photoQueue.value = _photoQueue.value + newItems
        _statusAlert.value = "Added ${newItems.size} compressed photo(s) to queue"
      }
      _isProcessingImages.value = false
    }
  }

  fun processCapturedPhoto(context: Context, file: File) {
    viewModelScope.launch {
      _isProcessingImages.value = true
      val item = ImageCompressor.processFile(
        context = context,
        file = file,
        batchPrefix = _batchPrefix.value,
        sequenceIndex = nextSequenceIndex++
      )
      if (item != null) {
        _photoQueue.value = _photoQueue.value + item
        _statusAlert.value = "Added photo to queue: ${item.fileName}"
      }
      // Clean up raw temp file
      if (file.exists()) {
        file.delete()
      }
      _isProcessingImages.value = false
    }
  }

  fun removePhoto(id: String) {
    val itemToRemove = _photoQueue.value.find { it.id == id }
    if (itemToRemove != null) {
      // delete cached file
      try {
        val file = File(itemToRemove.cachedFilePath)
        if (file.exists()) file.delete()
      } catch (_: Exception) {}
    }
    _photoQueue.value = _photoQueue.value.filterNot { it.id == id }
    if (_selectedPhotoForPreview.value?.id == id) {
      _selectedPhotoForPreview.value = null
    }
  }

  fun retryPhoto(id: String) {
    _photoQueue.value = _photoQueue.value.map {
      if (it.id == id) it.copy(status = UploadStatus.QUEUED, statusMessage = null) else it
    }
  }

  fun clearUploaded() {
    val uploaded = _photoQueue.value.filter { it.status == UploadStatus.UPLOADED }
    uploaded.forEach { item ->
      try {
        val f = File(item.cachedFilePath)
        if (f.exists()) f.delete()
      } catch (_: Exception) {}
    }
    _photoQueue.value = _photoQueue.value.filterNot { it.status == UploadStatus.UPLOADED }
  }

  fun clearAll() {
    _photoQueue.value.forEach { item ->
      try {
        val f = File(item.cachedFilePath)
        if (f.exists()) f.delete()
      } catch (_: Exception) {}
    }
    _photoQueue.value = emptyList()
    nextSequenceIndex = 1
  }

  fun renamePhoto(id: String, newName: String) {
    val clean = newName.trim()
    if (clean.isBlank()) return
    val finalName = if (clean.endsWith(".jpg", ignoreCase = true)) clean else "$clean.jpg"
    _photoQueue.value = _photoQueue.value.map {
      if (it.id == id) it.copy(fileName = finalName) else it
    }
  }

  fun uploadAll() {
    if (_isUploading.value) return
    val pendingPhotos = _photoQueue.value.filter {
      it.status == UploadStatus.QUEUED || it.status == UploadStatus.FAILED
    }
    if (pendingPhotos.isEmpty()) {
      _statusAlert.value = "No queued photos to upload"
      return
    }

    val url = _webAppUrl.value.trim()
    val isPlaceholder = url.isBlank() || url.equals("YOUR_WEB_APP_URL_HERE", ignoreCase = true)

    if (isPlaceholder && !_simulateIfPlaceholder.value) {
      _showSettingsDialog.value = true
      _statusAlert.value = "Please configure your Google Apps Script Web App URL first"
      return
    }

    uploadJob = viewModelScope.launch {
      _isUploading.value = true

      for (photo in pendingPhotos) {
        // Set item to UPLOADING
        _currentUploadingId.value = photo.id
        updatePhotoStatus(photo.id, UploadStatus.UPLOADING, "Uploading...")

        if (isPlaceholder) {
          // Simulation mode when URL hasn't been set yet
          delay(1200)
          val fakeDriveId = "drive_${System.currentTimeMillis()}"
          val fakeUrl = "https://drive.google.com/file/d/$fakeDriveId/view"
          updatePhotoStatus(
            photo.id,
            UploadStatus.UPLOADED,
            "Uploaded (Simulated)",
            fakeDriveId,
            fakeUrl
          )
        } else {
          // Real HTTP POST request to Google Apps Script Web App
          val result = DriveUploader.uploadPhoto(url, photo)
          when (result) {
            is UploadResult.Success -> {
              updatePhotoStatus(
                photo.id,
                UploadStatus.UPLOADED,
                result.message,
                result.fileId,
                result.fileUrl
              )
            }
            is UploadResult.Error -> {
              updatePhotoStatus(photo.id, UploadStatus.FAILED, result.message)
            }
          }
        }
      }

      _currentUploadingId.value = null
      _isUploading.value = false
      val finalUploaded = _photoQueue.value.count { it.status == UploadStatus.UPLOADED }
      val finalFailed = _photoQueue.value.count { it.status == UploadStatus.FAILED }
      _statusAlert.value = "Batch finished: $finalUploaded uploaded, $finalFailed failed"
    }
  }

  fun cancelUpload() {
    uploadJob?.cancel()
    _isUploading.value = false
    _currentUploadingId.value = null
    // Reset any still in UPLOADING back to QUEUED
    _photoQueue.value = _photoQueue.value.map {
      if (it.status == UploadStatus.UPLOADING) {
        it.copy(status = UploadStatus.QUEUED, statusMessage = "Upload cancelled")
      } else it
    }
    _statusAlert.value = "Upload process paused"
  }

  private fun updatePhotoStatus(
    id: String,
    status: UploadStatus,
    message: String?,
    fileId: String? = null,
    fileUrl: String? = null
  ) {
    _photoQueue.value = _photoQueue.value.map {
      if (it.id == id) {
        it.copy(
          status = status,
          statusMessage = message,
          driveFileId = fileId ?: it.driveFileId,
          driveFileUrl = fileUrl ?: it.driveFileUrl
        )
      } else it
    }
  }
}
