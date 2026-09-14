package com.example.accessibility

open class RashedAccessibilityService : JarvisAccessibilityService() {

    companion object {
        @Volatile
        var instance: RashedAccessibilityService? = null

        fun isRunning(): Boolean = instance != null || JarvisAccessibilityService.isRunning()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}

