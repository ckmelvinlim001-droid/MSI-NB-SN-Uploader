package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QueueSummary
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary

@Composable
fun SummaryCounterBar(
  summary: QueueSummary,
  isUploading: Boolean,
  onClearUploaded: () -> Unit,
  onClearAll: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Row: Title + Quick Cleanup Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "Queue Summary",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "(${summary.total} photos)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (summary.uploaded > 0) {
          TextButton(
            onClick = onClearUploaded,
            modifier = Modifier.testTag("clear_uploaded_button")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Clear uploaded",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Clear Uploaded", fontSize = 12.sp)
          }
        }
        if (summary.total > 0 && !isUploading) {
          TextButton(
            onClick = onClearAll,
            modifier = Modifier.testTag("clear_all_button")
          ) {
            Icon(
              imageVector = Icons.Default.ClearAll,
              contentDescription = "Clear all",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Clear All", fontSize = 12.sp)
          }
        }
      }
    }

    // Counter Badges
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      CounterPill(
        label = "Queued",
        count = summary.queued,
        color = SkyBluePrimary,
        icon = Icons.Default.HourglassTop,
        modifier = Modifier.weight(1f)
      )

      CounterPill(
        label = "Uploading",
        count = summary.uploading,
        color = SkyBlueLight,
        icon = Icons.Default.Sync,
        isLoading = isUploading,
        modifier = Modifier.weight(1f)
      )

      CounterPill(
        label = "Uploaded",
        count = summary.uploaded,
        color = AccentEmerald,
        icon = Icons.Default.CheckCircle,
        modifier = Modifier.weight(1f)
      )

      if (summary.failed > 0) {
        CounterPill(
          label = "Failed",
          count = summary.failed,
          color = AccentRose,
          icon = Icons.Default.Error,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Overall Progress Bar
    if (summary.total > 0) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (isUploading) "Uploading batch to Google Drive..." else "Upload Progress",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${(summary.progressPercent * 100).toInt()}% (${summary.uploaded}/${summary.total})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (summary.isComplete) AccentEmerald else MaterialTheme.colorScheme.primary
          )
        }

        LinearProgressIndicator(
          progress = { summary.progressPercent },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
          color = if (summary.isComplete) AccentEmerald else MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surface,
          strokeCap = StrokeCap.Round
        )
      }
    }
  }
}

@Composable
private fun CounterPill(
  label: String,
  count: Int,
  color: Color,
  icon: ImageVector,
  isLoading: Boolean = false,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(color.copy(alpha = 0.15f))
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    if (isLoading && count > 0) {
      CircularProgressIndicator(
        modifier = Modifier.size(12.dp),
        strokeWidth = 2.dp,
        color = color
      )
    } else {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = color,
        modifier = Modifier.size(13.dp)
      )
    }
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = "$count",
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      color = color
    )
    Spacer(modifier = Modifier.width(3.dp))
    Text(
      text = label,
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1
    )
  }
}
