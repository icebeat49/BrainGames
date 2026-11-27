package com.payfacto.braingames.core.di

import android.content.Context
import com.payfacto.braingames.core.save.DataStoreGameSaves
import com.payfacto.braingames.core.save.GameSaves
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/** A scope that lives as long as the app, for work that must not stop when a screen closes. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object SaveModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideGameSaves(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope
    ): GameSaves = DataStoreGameSaves(File(context.filesDir, "saves"), scope)
}
