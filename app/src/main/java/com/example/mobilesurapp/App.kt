package com.example.mobilesurapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import android.util.Log
import com.google.android.gms.security.ProviderInstaller

@HiltAndroidApp
class MobileSurApp : Application(), Configuration.Provider{
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            Log.d("WorkManagerConfig", "HiltWorkerFactory is being used (Manual Config via Configuration.Provider)!")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        upgradeSecurityProvider()
    }

    private fun upgradeSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(this)
        } catch (e: Exception) {
            Log.e("Security", "Gagal memperbarui Security Provider: ${e.message}")
        }
    }
}