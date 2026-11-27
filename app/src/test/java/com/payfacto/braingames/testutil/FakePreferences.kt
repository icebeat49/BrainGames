package com.payfacto.braingames.testutil

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.payfacto.braingames.core.data.PreferencesDataSource
import com.payfacto.braingames.core.data.PreferencesRepository
import com.payfacto.braingames.core.stats.StatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory DataStore: no files, no IO dispatcher, updates are visible immediately. */
class FakePreferencesDataStore : DataStore<Preferences> {
    private val current = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = current

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(current.value).also { current.value = it }
}

fun fakePreferencesRepository(): PreferencesRepository =
    PreferencesRepository(PreferencesDataSource(FakePreferencesDataStore()))

/** Stats kept in memory; writes happen at once, before the call returns. */
fun fakeStatsStore(): StatsStore =
    StatsStore(FakePreferencesDataStore(), CoroutineScope(Dispatchers.Unconfined))
