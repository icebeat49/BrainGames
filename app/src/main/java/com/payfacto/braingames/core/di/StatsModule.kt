package com.payfacto.braingames.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.payfacto.braingames.core.stats.StatsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/** Its own file, apart from settings and saves, so a reset or a problem in one never touches the others. */
private val Context.statsDataStore: DataStore<Preferences> by preferencesDataStore(name = "stats")

@Module
@InstallIn(SingletonComponent::class)
object StatsModule {
    @Provides
    @Singleton
    fun provideStatsStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope
    ): StatsStore = StatsStore(context.statsDataStore, scope)
}
