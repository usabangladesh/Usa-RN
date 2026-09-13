package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.intent.IntentRouter
import com.example.intent.ParsedIntent
import com.example.security.SecurityManager
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
    fun `read string from context matches Rashed AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Rashed AI", appName)
    }

    @Test
    fun `intent router parses emergency stop in Bengali and English`() {
        val bengaliStop = IntentRouter.parseUserCommand("থামো")
        assertTrue(bengaliStop is ParsedIntent.EmergencyStop)

        val englishStop = IntentRouter.parseUserCommand("stop")
        assertTrue(englishStop is ParsedIntent.EmergencyStop)
    }

    @Test
    fun `intent router parses battery command`() {
        val batteryIntent = IntentRouter.parseUserCommand("আমার battery কত?")
        assertTrue(batteryIntent is ParsedIntent.DirectTool)
        assertEquals("getBatteryStatus", (batteryIntent as ParsedIntent.DirectTool).toolName)
    }

    @Test
    fun `intent router parses lock screen command`() {
        val lockIntent = IntentRouter.parseUserCommand("ফোনটা lock করো")
        assertTrue(lockIntent is ParsedIntent.DirectTool)
        assertEquals("lockDevice", (lockIntent as ParsedIntent.DirectTool).toolName)
    }

    @Test
    fun `security manager blocks password extraction`() {
        val sec = SecurityManager()
        val isBlocked = sec.isRestricted("extractPassword", "what is my password")
        assertTrue(isBlocked)
    }
}
