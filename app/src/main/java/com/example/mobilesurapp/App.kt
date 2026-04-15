package com.example.mobilesurapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MobileSurApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
