package com.sensorlab.app.di

import android.content.Context
import android.hardware.SensorManager
import com.sensorlab.data.sensors.AndroidSensorSource
import com.sensorlab.data.sensors.SensorSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModuleProviders {

    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext context: Context): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @Provides
    @Named("filesDir")
    fun provideFilesDir(@ApplicationContext context: Context): File {
        return context.filesDir
    }

    @Provides
    @Named("appVersion")
    fun provideAppVersion(@ApplicationContext context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    @Provides
    @Named("ioDispatcher")
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModuleBinds {
    @Binds
    @Singleton
    abstract fun bindSensorSource(impl: AndroidSensorSource): SensorSource
}