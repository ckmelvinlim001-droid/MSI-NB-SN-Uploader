package com.example.network

import com.example.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class UploadResult {
  data class Success(val fileId: String?, val fileUrl: String?, val message: String) : UploadResult()
  data class Error(val message: String) : UploadResult()
}

object DriveUploader {

  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(90, TimeUnit.SECONDS)
    .readTimeout(90, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .retryOnConnectionFailure(true)
    .build()

  private val TEXT_PLAIN_MEDIA_TYPE = "text/plain; charset=utf-8".toMediaType()

  suspend fun uploadPhoto(
    webAppUrl: String,
    photoItem: PhotoItem
  ): UploadResult = withContext(Dispatchers.IO) {
    val cleanUrl = webAppUrl.trim()

    if (cleanUrl.isBlank() || cleanUrl.equals("YOUR_WEB_APP_URL_HERE", ignoreCase = true)) {
      return@withContext UploadResult.Error(
        "Please provide a valid Google Apps Script Web App URL in settings."
      )
    }

    if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
      return@withContext UploadResult.Error("URL must start with https://")
    }

    try {
      // Build JSON payload matching exactly: { fileName, mimeType: 'image/jpeg', base64 }
      val jsonPayload = JSONObject().apply {
        put("fileName", photoItem.fileName)
        put("mimeType", "image/jpeg")
        put("base64", photoItem.base64)
      }.toString()

      // Header Content-Type: text/plain;charset=utf-8 per CORS rule for Google Apps Script
      val requestBody = jsonPayload.toRequestBody(TEXT_PLAIN_MEDIA_TYPE)
      val request = Request.Builder()
        .url(cleanUrl)
        .header("Content-Type", "text/plain;charset=utf-8")
        .post(requestBody)
        .build()

      val response = client.newCall(request).execute()
      val responseBodyString = response.body?.string() ?: ""

      if (response.isSuccessful) {
        var fileId: String? = null
        var fileUrl: String? = null
        var msg = "Uploaded successfully to Google Drive"

        try {
          if (responseBodyString.startsWith("{")) {
            val jsonResponse = JSONObject(responseBodyString)
            if (jsonResponse.optString("status") == "error") {
              return@withContext UploadResult.Error(
                jsonResponse.optString("message", "Google Apps Script returned an error")
              )
            }
            fileId = jsonResponse.optString("fileId", null)
            fileUrl = jsonResponse.optString("url", null)
            val customMsg = jsonResponse.optString("message", null)
            if (!customMsg.isNullOrBlank()) msg = customMsg
          }
        } catch (_: Exception) {
          // If response was plain text like 'Success' or HTML
        }

        UploadResult.Success(fileId, fileUrl, msg)
      } else {
        UploadResult.Error("HTTP ${response.code}: ${response.message}")
      }
    } catch (e: IOException) {
      UploadResult.Error("Network error: ${e.localizedMessage ?: "Connection failed"}")
    } catch (e: Exception) {
      UploadResult.Error("Upload error: ${e.localizedMessage ?: "Unexpected error"}")
    }
  }
}
