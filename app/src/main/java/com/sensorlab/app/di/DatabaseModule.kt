package com.sensorlab.app.di

import android.content.Context
import androidx.room.Room
import com.sensorlab.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sensorlab_db"
        ).build()
    }

    @Provides
    fun provideSessionDao(db: AppDatabase) = db.sessionDao()

    @Provides
    fun provideSessionSensorDao(db: AppDatabase) = db.sessionSensorDao()

    @Provides
    fun provideMarkerDao(db: AppDatabase) = db.markerDao()

    @Provides
    fun provideLabelPresetDao(db: AppDatabase) = db.labelPresetDao()
}

