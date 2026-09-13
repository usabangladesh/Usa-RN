package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.intent.IntentRouter
import com.example.intent.ParsedIntent
import com.example.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `Section 35 Test 1 - YouTube search Ronaldo`() {
        val intent = IntentRouter.parseUserCommand("Rashed, YouTube খুলে Ronaldo search করো।")
        assertTrue(intent is ParsedIntent.DirectTool)
        val tool = intent as ParsedIntent.DirectTool
        assertEquals("searchYouTube", tool.toolName)
        assertTrue(tool.args.optString("query").contains("Ronaldo", ignoreCase = true))
    }

    @Test
    fun `Section 35 Test 2 - Play YouTube video`() {
        val intent = IntentRouter.parseUserCommand("Ronaldo-এর ভিডিওটা চালাও।")
        assertTrue(intent is ParsedIntent.DirectTool)
        val tool = intent as ParsedIntent.DirectTool
        assertEquals("playYouTubeVideo", tool.toolName)
        assertTrue(tool.args.optString("videoTitle").contains("Ronaldo", ignoreCase = true))
    }

    @Test
    fun `Section 35 Test 3 - Back navigation`() {
        val intent = IntentRouter.parseUserCommand("Back করো।")
        assertTrue(intent is ParsedIntent.DirectTool)
        assertEquals("accessibilityBack", (intent as ParsedIntent.DirectTool).toolName)
    }

    @Test
    fun `Section 35 Test 4 - WhatsApp contact chat`() {
        val intent = IntentRouter.parseUserCommand("WhatsApp খুলে Rahim-এর chat খোলো।")
        assertTrue(intent is ParsedIntent.DirectTool)
        val tool = intent as ParsedIntent.DirectTool
        assertEquals("openWhatsAppConversation", tool.toolName)
        assertEquals("Rahim", tool.args.optString("contactName"))
    }

    @Test
    fun `Section 35 Test 5 - WhatsApp prepare message with confirmation`() {
        val intent = IntentRouter.parseUserCommand("Rahim-কে লিখো আমি পরে আসব।")
        assertTrue(intent is ParsedIntent.DirectTool)
        val tool = intent as ParsedIntent.DirectTool
        assertEquals("prepareMessage", tool.toolName)
        assertEquals("Rahim", tool.args.optString("recipient"))
        assertTrue(tool.args.optString("messageText").contains("আমি পরে আসব"))
    }

    @Test
    fun `Section 35 Test 6 - Volume down`() {
        val intent = IntentRouter.parseUserCommand("Volume কমাও।")
        assertTrue(intent is ParsedIntent.DirectTool)
        val tool = intent as ParsedIntent.DirectTool
        assertEquals("volumeControl", tool.toolName)
        assertEquals("down", tool.args.optString("action"))
    }

    @Test
    fun `Section 35 Test 7 - Media pause`() {
        val intent = IntentRouter.parseUserCommand("গান pause করো।")
        assertTrue(intent is ParsedIntent.DirectTool)
        val tool = intent as ParsedIntent.DirectTool
        assertEquals("mediaControl", tool.toolName)
        assertEquals("pause", tool.args.optString("command"))
    }

    @Test
    fun `Section 35 Scroll navigation down and up`() {
        val scrollDown = IntentRouter.parseUserCommand("নিচে scroll করো")
        assertTrue(scrollDown is ParsedIntent.DirectTool)
        assertEquals("accessibilityScroll", (scrollDown as ParsedIntent.DirectTool).toolName)
        assertEquals("down", scrollDown.args.optString("direction"))

        val scrollUp = IntentRouter.parseUserCommand("উপরে scroll করো")
        assertTrue(scrollUp is ParsedIntent.DirectTool)
        assertEquals("accessibilityScroll", (scrollUp as ParsedIntent.DirectTool).toolName)
        assertEquals("up", scrollUp.args.optString("direction"))
    }

    @Test
    fun `security manager blocks password extraction`() {
        val sec = SecurityManager()
        val isBlocked = sec.isRestricted("extractPassword", "what is my password")
        assertTrue(isBlocked)
    }
}
