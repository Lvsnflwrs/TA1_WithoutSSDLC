package com.example.mobilesurapp.domain.utils

import android.os.Build
import android.os.Debug
import android.util.Log
import java.io.File
import java.net.Socket
import kotlin.system.exitProcess

object RaspManager {

    fun secureRuntimeCheck() {
        if (isDeviceRooted() || isDebuggerAttached() || isHookingFrameworkDetected()) {
            Log.e("RASP", "Ancaman Keamanan Terdeteksi! Mematikan aplikasi...")
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        }
    }

    private fun isDeviceRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    private fun isHookingFrameworkDetected(): Boolean {

        var fridaPortDetected = false
        try {
            val socket = Socket("127.0.0.1", 27042)
            socket.close()
            fridaPortDetected = true
        } catch (_: Exception) {
        }

        val xposedDetected = System.getenv("CLASSPATH")?.contains("XposedBridge") == true

        return fridaPortDetected || xposedDetected
    }

}