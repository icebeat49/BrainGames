package com.payfacto.braingames.core.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `no tags means the phone's language`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(""))
    }

    @Test
    fun `a plain tag maps to its language`() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromTags("fr"))
    }

    @Test
    fun `a regional tag maps to its language`() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromTags("fr-CA"))
    }

    @Test
    fun `only the first tag of a list counts`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en-US,fr"))
    }

    @Test
    fun `a language the app does not offer falls back to the phone's`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("de"))
    }
}
