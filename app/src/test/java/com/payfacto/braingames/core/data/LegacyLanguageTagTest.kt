package com.payfacto.braingames.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.payfacto.braingames.testutil.FakePreferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyLanguageTagTest {

    private val dataStore = FakePreferencesDataStore()
    private val dataSource = PreferencesDataSource(dataStore)

    private suspend fun saveLegacyTag(tag: String) {
        dataStore.edit { it[stringPreferencesKey("language_tag")] = tag }
    }

    @Test
    fun `a saved language is returned once`() = runTest {
        saveLegacyTag("fr")

        assertEquals("fr", dataSource.takeLegacyLanguageTag())
    }

    @Test
    fun `a saved language is gone after it is taken`() = runTest {
        saveLegacyTag("fr")
        dataSource.takeLegacyLanguageTag()

        assertNull(dataSource.takeLegacyLanguageTag())
    }

    @Test
    fun `the saved System choice counts as no language`() = runTest {
        saveLegacyTag("")

        assertNull(dataSource.takeLegacyLanguageTag())
    }

    @Test
    fun `nothing saved means no language`() = runTest {
        assertNull(dataSource.takeLegacyLanguageTag())
    }
}
