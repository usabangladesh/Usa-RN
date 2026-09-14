package com.example.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import java.net.URLEncoder

data class ResolvedContact(
    val displayName: String,
    val phoneNumber: String?
)

class AppController(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    // Common app mapping for Bangla, English, Banglish, and Hindi names
    private val knownAppPackages = mapOf(
        "whatsapp" to "com.whatsapp",
        "হোয়াটসঅ্যাপ" to "com.whatsapp",
        "হোয়াটসঅ্যাপ" to "com.whatsapp",
        "व्हाट्सएप" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "ইউটিউব" to "com.google.android.youtube",
        "यूट्यूब" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "ক্রোম" to "com.android.chrome",
        "क्रोम" to "com.android.chrome",
        "facebook" to "com.facebook.katana",
        "ফেসবুক" to "com.facebook.katana",
        "फेसबुक" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "ইনস্টাগ্রাম" to "com.instagram.android",
        "ইন্সটাগ্রাম" to "com.instagram.android",
        "instra" to "com.instagram.android",
        "gmail" to "com.google.android.gm",
        "জিমেইল" to "com.google.android.gm",
        "জি-মেইল" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "ম্যাপ" to "com.google.android.apps.maps",
        "ম্যাপস" to "com.google.android.apps.maps",
        "গুগল ম্যাপ" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "স্পটিফাই" to "com.spotify.music",
        "play store" to "com.android.vending",
        "প্লে স্টোর" to "com.android.vending",
        "प्ले स्टोर" to "com.android.vending"
    )

    fun findApp(appName: String): String? {
        val normalized = appName.trim().lowercase()
        for ((key, pkg) in knownAppPackages) {
            if (normalized.contains(key)) return pkg
        }
        val found = findPackageByLabel(normalized)
        if (found != null) return found
        if (normalized.contains(".") && isPackageInstalledInternal(normalized)) return normalized
        return null
    }

    fun isAppInstalled(packageNameOrName: String): Boolean {
        val targetPkg = findApp(packageNameOrName) ?: packageNameOrName
        return isPackageInstalledInternal(targetPkg)
    }

    private fun isPackageInstalledInternal(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun focusApp(appName: String): Result<String> {
        return openApp(appName)
    }

    fun openApp(appName: String): Result<String> {
        val normalized = appName.trim().lowercase()

        // 1. Check special system targets
        if (normalized.contains("camera") || normalized.contains("ক্যামেরা") || normalized.contains("कैमरा")) {
            return openCamera()
        }
        if (normalized.contains("setting") || normalized.contains("সেটিংস") || normalized.contains("সেটিং") || normalized.contains("सेटिंग")) {
            if (normalized.contains("bluetooth") || normalized.contains("ব্লুটুথ")) {
                return openBluetoothSettings()
            }
            if (normalized.contains("wifi") || normalized.contains("ওয়াইফাই") || normalized.contains("ওয়াইফাই")) {
                return openWifiSettings()
            }
            return openSettings("main")
        }

        // 2. Resolve package
        val targetPackage = findApp(normalized)

        if (targetPackage == null || !isPackageInstalledInternal(targetPackage)) {
            val displayTitle = appName.replaceFirstChar { it.uppercase() }
            return Result.failure(Exception("$displayTitle ফোনে install করা নেই।"))
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try {
                context.startActivity(launchIntent)
                Result.success("ঠিক আছে, $appName খুলে দেওয়া হয়েছে।")
            } catch (e: Exception) {
                Result.failure(Exception("$appName খোলার সময় সমস্যা হয়েছে: ${e.localizedMessage}"))
            }
        } else {
            Result.failure(Exception("$appName এর জন্য কোনো launch intent পাওয়া যায়নি।"))
        }
    }

    fun searchYouTube(query: String): Result<String> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val ytPackage = "com.google.android.youtube"
        val isYtInstalled = isPackageInstalledInternal(ytPackage)

        val intent = if (isYtInstalled) {
            Intent(Intent.ACTION_SEARCH).apply {
                `package` = ytPackage
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        return try {
            context.startActivity(intent)
            Result.success("YouTube-এ \"$query\" অনুসন্ধান করা হচ্ছে।")
        } catch (e: Exception) {
            // Fallback via web view URL
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                Result.success("YouTube-এ \"$query\" অনুসন্ধান করা হচ্ছে।")
            } catch (e2: Exception) {
                Result.failure(Exception("YouTube-এ অনুসন্ধান করা সম্ভব হয়নি: ${e2.localizedMessage}"))
            }
        }
    }

    fun openWebsite(rawUrl: String): Result<String> {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return Result.failure(Exception("Website URL cannot be blank."))
        }
        val targetUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            if (trimmed.contains(".") && !trimmed.contains(" ")) {
                "https://$trimmed"
            } else {
                "https://www.google.com/search?q=" + URLEncoder.encode(trimmed, "UTF-8")
            }
        } else {
            trimmed
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("Opened $targetUrl in your browser!")
        } catch (e: Exception) {
            Result.failure(Exception("Could not open website: ${e.localizedMessage}"))
        }
    }

    fun searchWeb(query: String): Result<String> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return Result.failure(Exception("Search query cannot be empty."))
        }
        return try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success("Searching for \"$trimmed\" on the web.")
        } catch (e: Exception) {
            Result.failure(Exception("Could not perform web search: ${e.localizedMessage}"))
        }
    }

    fun playYouTubeVideo(videoQuery: String): Result<String> {
        // First try searching/opening YouTube
        val res = searchYouTube(videoQuery)
        if (res.isFailure) return res

        // If Accessibility service is active, attempt to click visible matching video
        val clicked = com.example.accessibility.AccessibilityActionEngine.clickMatchingVideo(videoQuery)
        return if (clicked) {
            Result.success("\"$videoQuery\" ভিডিওটি চালু করা হয়েছে।")
        } else {
            Result.success("YouTube-এ \"$videoQuery\" এর ফলাফল প্রদর্শিত হয়েছে।")
        }
    }

    fun openWifiSettings(): Result<String> = openSettings("wifi")

    fun openBluetoothSettings(): Result<String> = openSettings("bluetooth")

    fun sendMessage(recipient: String, messageText: String): Result<String> {
        return openWhatsAppConversation(recipient, messageText)
    }

    private fun findPackageByLabel(query: String): String? {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
        for (info in resolveInfos) {
            val label = info.loadLabel(packageManager).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                return info.activityInfo.packageName
            }
        }
        return null
    }

    fun openSettings(type: String): Result<String> {
        val action = when (type.lowercase()) {
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "battery" -> Intent.ACTION_POWER_USAGE_SUMMARY
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            Result.success("$type Settings খুলে দেওয়া হয়েছে।")
        } catch (e: Exception) {
            Result.failure(Exception("Settings খুলতে ব্যর্থ হয়েছে: ${e.localizedMessage}"))
        }
    }

    fun openCamera(): Result<String> {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            Result.success("Camera খুলে দেওয়া হয়েছে।")
        } catch (_: Exception) {
            // Fallback to Samsung camera package or general camera
            val samsungCameraIntent = packageManager.getLaunchIntentForPackage("com.sec.android.app.camera")
            if (samsungCameraIntent != null) {
                samsungCameraIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(samsungCameraIntent)
                Result.success("Camera খুলে দেওয়া হয়েছে।")
            } else {
                Result.failure(Exception("Camera app খুঁজে পাওয়া যায়নি।"))
            }
        }
    }

    fun findContactByName(contactName: String): ResolvedContact? {
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")

        var cursor: Cursor? = null
        return try {
            cursor = resolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                ResolvedContact(displayName = name, phoneNumber = number)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    fun openWhatsAppConversation(recipient: String, messageText: String? = null): Result<String> {
        if (!isPackageInstalledInternal("com.whatsapp")) {
            return Result.failure(Exception("WhatsApp install করা নেই। তাই মেসেজ পাঠানো সম্ভব নয়।"))
        }

        // Check if recipient is a phone number or contact name
        val digitsOnly = recipient.replace("[^0-9+]".toRegex(), "")
        val targetPhone = if (digitsOnly.length >= 7) {
            digitsOnly.removePrefix("+")
        } else {
            val contact = findContactByName(recipient)
            contact?.phoneNumber?.replace("[^0-9+]".toRegex(), "")?.removePrefix("+")
        }

        val textEncoded = messageText?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        val uriString = if (!targetPhone.isNullOrEmpty()) {
            "https://api.whatsapp.com/send?phone=$targetPhone&text=$textEncoded"
        } else {
            // General WhatsApp share intent with text
            "whatsapp://send?text=$textEncoded"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            `package` = "com.whatsapp"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            Result.success("WhatsApp-এ $recipient এর জন্য কথোপকথন খোলা হয়েছে।")
        } catch (e: Exception) {
            Result.failure(Exception("WhatsApp খুলতে ব্যর্থ হয়েছে: ${e.localizedMessage}"))
        }
    }
}
