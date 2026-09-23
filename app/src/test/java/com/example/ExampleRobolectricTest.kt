package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.NavigationDecision
import com.example.security.NavigationGuard
import org.junit.Assert.assertEquals
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
    assertEquals("SiteLock Browser", appName)
  }

  @Test
  fun `test navigation guard allows permitted domain`() {
    val guard = NavigationGuard()
    val decision = guard.evaluate("https://example.com/page1")
    assertTrue(decision is NavigationDecision.Allow)
  }

  @Test
  fun `test navigation guard blocks external domain`() {
    val guard = NavigationGuard()
    val decision = guard.evaluate("https://google.com")
    assertTrue(decision is NavigationDecision.Block)
  }

  @Test
  fun `test navigation guard blocks file protocol`() {
    val guard = NavigationGuard()
    val decision = guard.evaluate("file:///etc/passwd")
    assertTrue(decision is NavigationDecision.Block)
  }

  @Test
  fun `test navigation guard blocks javascript scheme`() {
    val guard = NavigationGuard()
    val decision = guard.evaluate("javascript:alert(1)")
    assertTrue(decision is NavigationDecision.Block)
  }
}
