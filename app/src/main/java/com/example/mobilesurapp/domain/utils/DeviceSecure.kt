package com.example.mobilesurapp.domain.utils

import android.app.KeyguardManager
import android.content.Context

fun isDeviceSecure(context: Context): Boolean {
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return keyguardManager.isDeviceSecure
}