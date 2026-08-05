package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("BARRA CLOUD", appName)
  }

  @Test
  fun `verify SHA-1 hash calculation for thumbnail protocol`() {
    val relPath = "/Foto Keluarga/IMG_001.jpg"
    val hash = com.example.util.ThumbHashUtil.calculateSha1Hash(relPath)
    assertEquals(40, hash.length)
    assert(com.example.util.ThumbHashUtil.isThumbsPath("/mnt/exthdd/.thumbs/$hash.jpg"))
  }
}
