package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ActionButtons(
  isProcessing: Boolean,
  onPhotoCaptured: (File) -> Unit,
  onPhotosSelected: (List<Uri>) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // State to hold temporary file for camera capture
  var tempCameraFile by remember { mutableStateOf<File?>(null) }

  // Camera capture launcher
  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    if (success) {
      tempCameraFile?.let { file ->
        if (file.exists() && file.length() > 0) {
          onPhotoCaptured(file)
        }
      }
    }
  }

  // Gallery multi-picker launcher (zero-permission modern photo picker)
  val pickMultipleLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris ->
    if (uris.isNotEmpty()) {
      onPhotosSelected(uris)
    }
  }

  fun launchCamera() {
    try {
      val cameraDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
      val file = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
      tempCameraFile = file
      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      takePictureLauncher.launch(uri)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // "Take Photo" Button
      Button(
        onClick = { launchCamera() },
        enabled = !isProcessing,
        modifier = Modifier
          .weight(1f)
          .height(56.dp)
          .testTag("take_photo_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
      ) {
        Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = "Take Photo",
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Take Photo",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }

      // "Choose from Gallery" Button
      FilledTonalButton(
        onClick = {
          pickMultipleLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        enabled = !isProcessing,
        modifier = Modifier
          .weight(1f)
          .height(56.dp)
          .testTag("choose_gallery_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
      ) {
        Icon(
          imageVector = Icons.Default.PhotoLibrary,
          contentDescription = "Choose from Gallery",
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "From Gallery",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }
    }

    if (isProcessing) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Compressing photo (max 1600px, 0.8 JPEG)...",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }
  }
}
