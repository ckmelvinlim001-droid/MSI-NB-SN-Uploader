package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QueueSummary
import com.example.ui.theme.AccentEmerald

@Composable
fun UploadAllButton(
  summary: QueueSummary,
  isUploading: Boolean,
  onUploadAll: () -> Unit,
  onCancelUpload: () -> Unit,
  modifier: Modifier = Modifier
) {
  val pendingCount = summary.queued + summary.failed

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    if (isUploading) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(
          onClick = {},
          enabled = false,
          modifier = Modifier
            .weight(1f)
            .height(54.dp),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            disabledContentColor = Color.White
          )
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.5.dp,
            color = Color.White
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Uploading (${summary.uploaded + 1}/${summary.total})...",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White
          )
        }

        OutlinedButton(
          onClick = onCancelUpload,
          modifier = Modifier
            .height(54.dp)
            .testTag("cancel_upload_button"),
          shape = RoundedCornerShape(16.dp)
        ) {
          Icon(
            imageVector = Icons.Default.StopCircle,
            contentDescription = "Cancel upload",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Cancel",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
    } else {
      val isAllDone = summary.total > 0 && summary.isComplete

      Button(
        onClick = onUploadAll,
        enabled = pendingCount > 0,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("upload_all_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isAllDone) AccentEmerald else MaterialTheme.colorScheme.primary,
          contentColor = Color.White,
          disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
      ) {
        Icon(
          imageVector = if (isAllDone) Icons.Default.DoneAll else Icons.Default.CloudUpload,
          contentDescription = "Upload All to Google Drive",
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = when {
            isAllDone -> "All Photos Uploaded to Drive ✓"
            pendingCount > 0 -> "Upload All to Google Drive ($pendingCount)"
            else -> "Upload All to Google Drive"
          },
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }
    }
  }
}
