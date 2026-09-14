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

    @Test
    fun `ApiKeyManager saves retrieves and masks API key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testKey = "AQ.TestCustomKey1234567890XYZ"
        com.example.gemini.ApiKeyManager.saveCustomApiKey(context, testKey)

        assertTrue(com.example.gemini.ApiKeyManager.isConfigured(context))
        assertEquals(testKey, com.example.gemini.ApiKeyManager.getActiveApiKey(context))
        assertTrue(com.example.gemini.ApiKeyManager.getMaskedKey(context).startsWith("AQ.Tes..."))

        com.example.gemini.ApiKeyManager.clearCustomApiKey(context)
        assertEquals(null, com.example.gemini.ApiKeyManager.getCustomApiKey(context))
    }

    @Test
    fun `Bangla conversational queries route to DirectSpeech in pure Bangla`() {
        val greeting = IntentRouter.parseUserCommand("কেমন আছো রাশেদ?")
        assertTrue(greeting is ParsedIntent.DirectSpeech)
        assertTrue((greeting as ParsedIntent.DirectSpeech).message.contains("ভালো আছি"))

        val identity = IntentRouter.parseUserCommand("তোমার নাম কী?")
        assertTrue(identity is ParsedIntent.DirectSpeech)
        assertTrue((identity as ParsedIntent.DirectSpeech).message.contains("Rashed AI"))

        val thanks = IntentRouter.parseUserCommand("অনেক ধন্যবাদ")
        assertTrue(thanks is ParsedIntent.DirectSpeech)
        assertTrue((thanks as ParsedIntent.DirectSpeech).message.contains("ধন্যবাদ"))
    }

    @Test
    fun `Universal App Commands route accurately in Bangla, Banglish and English`() {
        // App launches
        val wa = IntentRouter.parseUserCommand("WhatsApp খোলো")
        assertTrue(wa is ParsedIntent.UniversalCommand)

        val yt = IntentRouter.parseUserCommand("YouTube খোলো")
        assertTrue(yt is ParsedIntent.UniversalCommand)

        val fb = IntentRouter.parseUserCommand("Facebook খোলো")
        assertTrue(fb is ParsedIntent.UniversalCommand)

        val chat = IntentRouter.parseUserCommand("Rahim-এর chat খোলো")
        assertTrue(chat is ParsedIntent.UniversalCommand)

        val chatBanglish = IntentRouter.parseUserCommand("Rahim er chat kholo")
        assertTrue(chatBanglish is ParsedIntent.UniversalCommand)

        // In-app actions
        val search = IntentRouter.parseUserCommand("Search অপশন খোলো")
        assertTrue(search is ParsedIntent.UniversalCommand)

        val ronaldoSearch = IntentRouter.parseUserCommand("Ronaldo লিখে search করো")
        assertTrue(ronaldoSearch is ParsedIntent.UniversalCommand)

        val searchBanglish = IntentRouter.parseUserCommand("Search koro Ronaldo")
        assertTrue(searchBanglish is ParsedIntent.UniversalCommand)

        val scrollDown = IntentRouter.parseUserCommand("নিচে যাও")
        assertTrue(scrollDown is ParsedIntent.UniversalCommand)

        val scrollUp = IntentRouter.parseUserCommand("উপরে যাও")
        assertTrue(scrollUp is ParsedIntent.UniversalCommand)

        val scrollEn = IntentRouter.parseUserCommand("Scroll down")
        assertTrue(scrollEn is ParsedIntent.UniversalCommand)

        val back = IntentRouter.parseUserCommand("Back করো")
        assertTrue(back is ParsedIntent.UniversalCommand)

        val backPage = IntentRouter.parseUserCommand("আগের পেজে যাও")
        assertTrue(backPage is ParsedIntent.UniversalCommand)

        val goBackEn = IntentRouter.parseUserCommand("Go back")
        assertTrue(goBackEn is ParsedIntent.UniversalCommand)

        val openThis = IntentRouter.parseUserCommand("এটা open করো")
        assertTrue(openThis is ParsedIntent.UniversalCommand)

        val clickThis = IntentRouter.parseUserCommand("এইটা click করো")
        assertTrue(clickThis is ParsedIntent.UniversalCommand)

        val typeHere = IntentRouter.parseUserCommand("এখানে টাইপ করো")
        assertTrue(typeHere is ParsedIntent.UniversalCommand)

        val typeText = IntentRouter.parseUserCommand("টাইপ করো: আমি এখন আসছি")
        assertTrue(typeText is ParsedIntent.UniversalCommand)

        val typeBanglish = IntentRouter.parseUserCommand("Type koro ami aschi")
        assertTrue(typeBanglish is ParsedIntent.UniversalCommand)

        // Multi-step compound command
        val compound1 = IntentRouter.parseUserCommand("WhatsApp খোলো, Rahim-এর chat খোলো এবং টাইপ করো আমি আসছি")
        assertTrue(compound1 is ParsedIntent.UniversalCommand)

        val compound2 = IntentRouter.parseUserCommand("YouTube খোলো এবং Ronaldo search করো")
        assertTrue(compound2 is ParsedIntent.UniversalCommand)
    }

    @Test
    fun `Stop and Cancel keywords halt execution immediately`() {
        val stop1 = IntentRouter.parseUserCommand("থামো")
        assertEquals(ParsedIntent.EmergencyStop, stop1)

        val stop2 = IntentRouter.parseUserCommand("বন্ধ হও")
        assertEquals(ParsedIntent.EmergencyStop, stop2)

        val stop3 = IntentRouter.parseUserCommand("Listening বন্ধ করো")
        assertEquals(ParsedIntent.EmergencyStop, stop3)

        val stop4 = IntentRouter.parseUserCommand("Stop")
        assertEquals(ParsedIntent.EmergencyStop, stop4)
    }

    @Test
    fun `Confirmation and send keywords parse as UserConfirmed`() {
        val yes = IntentRouter.parseUserCommand("হ্যাঁ")
        assertEquals(ParsedIntent.UserConfirmed, yes)

        val send = IntentRouter.parseUserCommand("পাঠাও")
        assertEquals(ParsedIntent.UserConfirmed, send)

        val sendEn = IntentRouter.parseUserCommand("send")
        assertEquals(ParsedIntent.UserConfirmed, sendEn)
    }
}
