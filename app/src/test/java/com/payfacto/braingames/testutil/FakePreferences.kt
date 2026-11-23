package com.payfacto.braingames.testutil

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.payfacto.braingames.core.data.PreferencesDataSource
import com.payfacto.braingames.core.data.PreferencesRepository
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
