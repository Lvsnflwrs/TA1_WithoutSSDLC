package com.example.mobilesurapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mobilesurapp.database.dao.UserDao
import com.example.mobilesurapp.database.dao.PendingSyncDao
import com.example.mobilesurapp.model.User
import com.example.mobilesurapp.model.PendingSyncData
import androidx.room.TypeConverters
import com.example.mobilesurapp.database.converters.FloatArrayConverter

@Database(entities = [User::class, PendingSyncData::class], version = 1, exportSchema = false)
@TypeConverters(FloatArrayConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun pendingSyncDao(): PendingSyncDao
}