package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.QueueSummary
import com.example.util.ImageCompressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MSI SN Uploader", appName)
  }

  @Test
  fun `test credentials validation logic`() {
    fun isValid(id: String, pw: String): Boolean {
      return id.trim().equals("ALLIT", ignoreCase = true) && pw.trim() == "123456"
    }

    assertTrue(isValid("ALLIT", "123456"))
    assertTrue(isValid("allit", "123456"))
    assertTrue(isValid(" AllIt ", " 123456 "))
    assertFalse(isValid("ALLIT", "wrong"))
    assertFalse(isValid("other", "123456"))
  }

  @Test
  fun `test filename generation with prefix`() {
    val name = ImageCompressor.generateFileName("PO-1234", 1)
    assertTrue(name.startsWith("PO-1234_"))
    assertTrue(name.endsWith("_001.jpg"))
  }

  @Test
  fun `test filename generation with empty prefix`() {
    val name = ImageCompressor.generateFileName("", 2)
    assertTrue(name.startsWith("SN_"))
    assertTrue(name.endsWith("_002.jpg"))
  }

  @Test
  fun `test queue summary calculation`() {
    val summary = QueueSummary(total = 5, queued = 2, uploading = 1, uploaded = 2, failed = 0)
    assertEquals(0.4f, summary.progressPercent, 0.01f)
    assertEquals(false, summary.isComplete)

    val completed = QueueSummary(total = 3, queued = 0, uploading = 0, uploaded = 3, failed = 0)
    assertEquals(1.0f, completed.progressPercent, 0.01f)
    assertEquals(true, completed.isComplete)
  }
}
