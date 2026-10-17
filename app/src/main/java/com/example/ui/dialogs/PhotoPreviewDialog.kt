package com.example.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PhotoItem
import com.example.model.UploadStatus
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import java.io.File
import java.util.Locale

@Composable
fun PhotoPreviewDialog(
  photo: PhotoItem,
  onRename: (String) -> Unit,
  onRemove: () -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var isEditingName by remember { mutableStateOf(false) }
  var editedName by remember { mutableStateOf(photo.fileName) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Photo Details",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = photo.status.name,
          color = when (photo.status) {
            UploadStatus.UPLOADED -> AccentEmerald
            UploadStatus.FAILED -> AccentRose
            UploadStatus.UPLOADING -> MaterialTheme.colorScheme.primary
            UploadStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
          },
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // High-res preview image
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
          AsyncImage(
            model = File(photo.cachedFilePath),
            contentDescription = photo.fileName,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
          )
        }

        // Filename section (editable)
        if (isEditingName) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = editedName,
              onValueChange = { editedName = it },
              modifier = Modifier.weight(1f),
              label = { Text("Filename") },
              singleLine = true
            )
            Button(
              onClick = {
                onRename(editedName)
                isEditingName = false
              }
            ) {
              Text("Save")
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Target Filename:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = photo.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            IconButton(onClick = { isEditingName = true }) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Rename",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // Technical Compression Stats Table
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          StatRow("Dimensions", "${photo.width} × ${photo.height} px (max 1600w)")
          StatRow("Compressed Size", "${formatBytes(photo.compressedSize)} (JPEG 0.8)")
          StatRow("Original Size", formatBytes(photo.originalSize))
          if (photo.compressionRatioPercent > 0) {
            StatRow("Data Reduction", "${photo.compressionRatioPercent}% reduction")
          }
          StatRow("Base64 Length", "${photo.base64.length} chars (prefix stripped)")
        }

        // If uploaded to Drive, show open link
        if (!photo.driveFileUrl.isNullOrBlank()) {
          Button(
            onClick = {
              try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(photo.driveFileUrl))
                context.startActivity(intent)
              } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
          ) {
            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open in Drive")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open in Google Drive")
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Close")
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onRemove()
          onDismiss()
        },
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Remove photo",
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Remove")
      }
    }
  )
}

@Composable
private fun StatRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 11.sp
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 11.sp
    )
  }
}

private fun formatBytes(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val kb = bytes / 1024.0
  if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
  val mb = kb / 1024.0
  return String.format(Locale.US, "%.2f MB", mb)
}
