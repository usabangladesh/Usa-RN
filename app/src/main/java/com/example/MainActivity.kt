package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.apps.AppController
import com.example.assistant.RashedAssistantEngine
import com.example.assistant.RashedVoiceService
import com.example.device.DeviceController
import com.example.media.MediaController
import com.example.memory.MemoryManager
import com.example.permissions.PermissionManager
import com.example.screen.ScreenAnalyzer
import com.example.security.ConfirmationManager
import com.example.security.SecurityManager
import com.example.tools.ToolDispatcher
import com.example.ui.RashedScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var memoryManager: MemoryManager
    private lateinit var assistantEngine: RashedAssistantEngine

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            assistantEngine.startVoiceSession()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        memoryManager = MemoryManager(this)

        val appController = AppController(this)
        val deviceController = DeviceController(this)
        val mediaController = MediaController(this)
        val screenAnalyzer = ScreenAnalyzer(this)
        val confirmationManager = ConfirmationManager()
        val securityManager = SecurityManager()

        val toolDispatcher = ToolDispatcher(
            context = this,
            appController = appController,
            deviceController = deviceController,
            mediaController = mediaController,
            screenAnalyzer = screenAnalyzer,
            confirmationManager = confirmationManager,
            securityManager = securityManager
        )

        assistantEngine = RashedAssistantEngine(
            context = this,
            toolDispatcher = toolDispatcher,
            confirmationManager = confirmationManager,
            memoryManager = memoryManager,
            permissionManager = permissionManager
        )

        requestRequiredPermissions()

        setContent {
            MyApplicationTheme {
                RashedScreen(
                    assistantEngine = assistantEngine,
                    memoryManager = memoryManager,
                    permissionManager = permissionManager
                )
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!permissionManager.hasRecordAudioPermission()) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (!permissionManager.hasContactsPermission()) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionManager.hasNotificationPermission()) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        assistantEngine.release()
    }
}
