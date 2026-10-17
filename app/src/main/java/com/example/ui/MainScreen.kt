package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ActionButtons
import com.example.ui.components.BatchPrefixInput
import com.example.ui.components.PhotoQueueGrid
import com.example.ui.components.SummaryCounterBar
import com.example.ui.components.UploadAllButton
import com.example.ui.dialogs.PhotoPreviewDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.viewmodel.UploaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  viewModel: UploaderViewModel,
  onLogout: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val batchPrefix by viewModel.batchPrefix.collectAsState()
  val webAppUrl by viewModel.webAppUrl.collectAsState()
  val photoQueue by viewModel.photoQueue.collectAsState()
  val isProcessingImages by viewModel.isProcessingImages.collectAsState()
  val isUploading by viewModel.isUploading.collectAsState()
  val currentUploadingId by viewModel.currentUploadingId.collectAsState()
  val summary by viewModel.summary.collectAsState()
  val statusAlert by viewModel.statusAlert.collectAsState()
  val selectedPhotoForPreview by viewModel.selectedPhotoForPreview.collectAsState()
  val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
  val simulateIfPlaceholder by viewModel.simulateIfPlaceholder.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(statusAlert) {
    statusAlert?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearStatusAlert()
    }
  }

  val isPlaceholderUrl = webAppUrl.isBlank() || webAppUrl.equals("YOUR_WEB_APP_URL_HERE", ignoreCase = true)

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Laptop,
                contentDescription = "MSI Laptop",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Text(
                text = "MSI 💻 SN Uploader",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Google Drive Batch Terminal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.setShowSettingsDialog(true) },
            modifier = Modifier.testTag("settings_button")
          ) {
            if (isPlaceholderUrl) {
              BadgedBox(
                badge = {
                  Badge(containerColor = MaterialTheme.colorScheme.error)
                }
              ) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Settings and URL configuration",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            } else {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings and URL configuration",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }

          IconButton(
            onClick = onLogout,
            modifier = Modifier.testTag("logout_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      // Bottom persistent Upload All bar
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(horizontal = 16.dp, vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            UploadAllButton(
              summary = summary,
              isUploading = isUploading,
              onUploadAll = { viewModel.uploadAll() },
              onCancelUpload = { viewModel.cancelUpload() }
            )
          }
        }
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Target URL notice banner if still placeholder
        if (isPlaceholderUrl) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
              .clickable { viewModel.setShowSettingsDialog(true) }
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Notice",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Target URL: YOUR_WEB_APP_URL_HERE",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 11.sp
              )
              Text(
                text = "Tap here to paste your Google Apps Script Web App URL & view deployment script",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontSize = 10.sp
              )
            }
          }
        }

        // 1. Batch / Prefix Name input
        BatchPrefixInput(
          prefix = batchPrefix,
          onPrefixChange = { viewModel.setBatchPrefix(it) }
        )

        // 2. Action buttons: "Take Photo" & "Choose from Gallery"
        ActionButtons(
          isProcessing = isProcessingImages,
          onPhotoCaptured = { file ->
            viewModel.processCapturedPhoto(context, file)
          },
          onPhotosSelected = { uris ->
            viewModel.processSelectedUris(context, uris)
          }
        )

        // 3. Summary counter bar (real-time progress + counters)
        SummaryCounterBar(
          summary = summary,
          isUploading = isUploading,
          onClearUploaded = { viewModel.clearUploaded() },
          onClearAll = { viewModel.clearAll() }
        )

        // 4. Local Visual Queue Grid
        PhotoQueueGrid(
          photos = photoQueue,
          currentUploadingId = currentUploadingId,
          onRemovePhoto = { viewModel.removePhoto(it) },
          onRetryPhoto = { viewModel.retryPhoto(it) },
          onPhotoClick = { viewModel.setSelectedPhotoForPreview(it) },
          modifier = Modifier.weight(1f)
        )
      }
    }
  }

  // Settings & Google Apps Script Setup Dialog
  if (showSettingsDialog) {
    SettingsDialog(
      currentUrl = webAppUrl,
      simulateIfPlaceholder = simulateIfPlaceholder,
      onSaveUrl = { viewModel.setWebAppUrl(it) },
      onToggleSimulate = { viewModel.setSimulateIfPlaceholder(it) },
      onDismiss = { viewModel.setShowSettingsDialog(false) }
    )
  }

  // Photo Full Preview & Rename Dialog
  selectedPhotoForPreview?.let { photo ->
    PhotoPreviewDialog(
      photo = photo,
      onRename = { newName -> viewModel.renamePhoto(photo.id, newName) },
      onRemove = { viewModel.removePhoto(photo.id) },
      onDismiss = { viewModel.setSelectedPhotoForPreview(null) }
    )
  }
}
