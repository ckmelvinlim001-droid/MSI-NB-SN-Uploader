package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PhotoItem
import com.example.model.UploadStatus
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary
import java.io.File

@Composable
fun PhotoQueueGrid(
  photos: List<PhotoItem>,
  currentUploadingId: String?,
  onRemovePhoto: (String) -> Unit,
  onRetryPhoto: (String) -> Unit,
  onPhotoClick: (PhotoItem) -> Unit,
  modifier: Modifier = Modifier
) {
  if (photos.isEmpty()) {
    EmptyQueueState(modifier = modifier)
  } else {
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 160.dp),
      modifier = modifier
        .fillMaxWidth()
        .testTag("photo_queue_grid"),
      contentPadding = PaddingValues(vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(photos, key = { it.id }) { photo ->
        PhotoQueueCard(
          photo = photo,
          isCurrentUploading = photo.id == currentUploadingId,
          onRemove = { onRemovePhoto(photo.id) },
          onRetry = { onRetryPhoto(photo.id) },
          onClick = { onPhotoClick(photo) }
        )
      }
    }
  }
}

@Composable
private fun PhotoQueueCard(
  photo: PhotoItem,
  isCurrentUploading: Boolean,
  onRemove: () -> Unit,
  onRetry: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val borderColor = when (photo.status) {
    UploadStatus.UPLOADING -> SkyBlueLight
    UploadStatus.UPLOADED -> AccentEmerald
    UploadStatus.FAILED -> AccentRose
    UploadStatus.QUEUED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize()
      .border(
        width = if (photo.status == UploadStatus.UPLOADING) 2.dp else 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(16.dp)
      )
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("photo_card_${photo.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column {
      // Thumbnail Box with overlaid badge and remove button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.2f)
          .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        AsyncImage(
          model = File(photo.cachedFilePath),
          contentDescription = photo.fileName,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )

        // Status badge in top-left
        Box(
          modifier = Modifier
            .padding(8.dp)
            .align(Alignment.TopStart)
            .clip(RoundedCornerShape(8.dp))
            .background(
              when (photo.status) {
                UploadStatus.UPLOADED -> AccentEmerald.copy(alpha = 0.92f)
                UploadStatus.UPLOADING -> SkyBluePrimary.copy(alpha = 0.92f)
                UploadStatus.FAILED -> AccentRose.copy(alpha = 0.92f)
                UploadStatus.QUEUED -> Color(0xFF1E293B).copy(alpha = 0.88f)
              }
            )
            .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            when (photo.status) {
              UploadStatus.UPLOADING -> {
                CircularProgressIndicator(
                  modifier = Modifier.size(10.dp),
                  strokeWidth = 1.5.dp,
                  color = Color.White
                )
                Text(
                  text = "Uploading...",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              UploadStatus.UPLOADED -> {
                Icon(
                  imageVector = Icons.Default.CloudDone,
                  contentDescription = "Uploaded",
                  tint = Color.White,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "Uploaded",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              UploadStatus.FAILED -> {
                Icon(
                  imageVector = Icons.Default.Error,
                  contentDescription = "Failed",
                  tint = Color.White,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "Failed",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              UploadStatus.QUEUED -> {
                Icon(
                  imageVector = Icons.Default.HourglassEmpty,
                  contentDescription = "Queued",
                  tint = Color.White,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "Queued",
                  color = Color.White,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        // Remove button in top-right
        IconButton(
          onClick = onRemove,
          modifier = Modifier
            .padding(6.dp)
            .size(28.dp)
            .align(Alignment.TopEnd)
            .clip(CircleShape)
            .background(Color(0x99000000))
            .testTag("remove_button_${photo.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove photo",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }

        // Tap zoom indicator in bottom right
        Box(
          modifier = Modifier
            .padding(6.dp)
            .align(Alignment.BottomEnd)
            .clip(CircleShape)
            .background(Color(0x88000000))
            .padding(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ZoomIn,
            contentDescription = "Preview full size",
            tint = Color.White,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      // Metadata info section
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Filename
        Text(
          text = photo.fileName,
          style = MaterialTheme.typography.bodySmall,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          fontSize = 12.sp
        )

        // Compression info
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${photo.width}×${photo.height}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
          )
          Text(
            text = formatFileSize(photo.compressedSize),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp
          )
        }

        // Saved percent badge
        if (photo.compressionRatioPercent > 0) {
          Text(
            text = "Saved ${photo.compressionRatioPercent}% via 0.8 JPEG",
            style = MaterialTheme.typography.bodySmall,
            color = AccentEmerald,
            fontSize = 9.sp,
            maxLines = 1
          )
        }

        // Error message if failed
        if (photo.status == UploadStatus.FAILED && !photo.statusMessage.isNullOrBlank()) {
          Text(
            text = photo.statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = AccentRose,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )

          OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
              .fillMaxWidth()
              .height(32.dp)
              .testTag("retry_button_${photo.id}"),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Retry",
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Retry", fontSize = 11.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyQueueState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 40.dp, horizontal = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.AddPhotoAlternate,
          contentDescription = "Empty Queue",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(40.dp)
        )
      }

      Text(
        text = "No Photos in Queue",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = "Tap 'Take Photo' to snap camera pictures sequentially or 'From Gallery' to select images. Each will be compressed to 1600px JPEG.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        fontSize = 13.sp
      )
    }
  }
}

private fun formatFileSize(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val kb = bytes / 1024.0
  if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
  val mb = kb / 1024.0
  return String.format(java.util.Locale.US, "%.2f MB", mb)
}
