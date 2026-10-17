package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald

private const val GOOGLE_APPS_SCRIPT_TEMPLATE = """function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    var folderName = "SN Uploads";
    var folders = DriveApp.getFoldersByName(folderName);
    var folder = folders.hasNext() ? folders.next() : DriveApp.createFolder(folderName);
    
    var decoded = Utilities.base64Decode(data.base64);
    var blob = Utilities.newBlob(decoded, data.mimeType || "image/jpeg", data.fileName);
    var file = folder.createFile(blob);
    
    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      fileId: file.getId(),
      url: file.getUrl(),
      message: "Uploaded to Google Drive: " + data.fileName
    })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}"""

@Composable
fun SettingsDialog(
  currentUrl: String,
  simulateIfPlaceholder: Boolean,
  onSaveUrl: (String) -> Unit,
  onToggleSimulate: (Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var inputUrl by remember { mutableStateOf(currentUrl) }
  var copiedScript by remember { mutableStateOf(false) }

  val clipboardManager = remember {
    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "Settings",
          tint = MaterialTheme.colorScheme.primary
        )
        Text(text = "Google Drive Configuration", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Specify your Google Apps Script Web App URL below. When photos are uploaded, each is posted as a text/plain JSON payload containing the base64-encoded image.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = inputUrl,
          onValueChange = { inputUrl = it },
          label = { Text("Web App Script URL") },
          placeholder = { Text("https://script.google.com/macros/s/.../exec") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("web_app_url_input"),
          singleLine = false,
          maxLines = 3,
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Link,
              contentDescription = "URL Icon"
            )
          },
          trailingIcon = {
            IconButton(
              onClick = {
                val clip = clipboardManager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                  val text = clip.getItemAt(0).text?.toString() ?: ""
                  if (text.isNotBlank()) inputUrl = text
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = "Paste from clipboard"
              )
            }
          }
        )

        // Reset to placeholder button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = { inputUrl = "YOUR_WEB_APP_URL_HERE" },
            modifier = Modifier.testTag("reset_url_button")
          ) {
            Text("Reset to Default Placeholder", fontSize = 11.sp)
          }
        }

        // Simulation mode switch
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Simulation Mode (If placeholder)",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Simulates progress if URL is set to YOUR_WEB_APP_URL_HERE",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Switch(
            checked = simulateIfPlaceholder,
            onCheckedChange = onToggleSimulate,
            modifier = Modifier.testTag("simulate_switch")
          )
        }

        // Apps Script Deployment Guide Section
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "Code",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Google Apps Script Code",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }

            IconButton(
              onClick = {
                val clip = ClipData.newPlainText("Google Apps Script", GOOGLE_APPS_SCRIPT_TEMPLATE)
                clipboardManager.setPrimaryClip(clip)
                copiedScript = true
                Toast.makeText(context, "Apps Script code copied!", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = if (copiedScript) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy code",
                tint = if (copiedScript) AccentEmerald else Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF020617))
              .padding(8.dp)
              .horizontalScroll(rememberScrollState())
          ) {
            Text(
              text = GOOGLE_APPS_SCRIPT_TEMPLATE,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              color = Color(0xFFE2E8F0)
            )
          }

          Text(
            text = "Deployment Steps:\n1. Open script.google.com & paste this code\n2. Click 'Deploy' > 'New deployment'\n3. Type: 'Web app'\n4. Execute as: 'Me', Who has access: 'Anyone'\n5. Copy Web App URL into the field above.",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSaveUrl(inputUrl)
          onDismiss()
        },
        modifier = Modifier.testTag("save_settings_button")
      ) {
        Text("Save Settings")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
