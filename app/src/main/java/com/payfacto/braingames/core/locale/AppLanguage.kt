package com.payfacto.braingames.core.locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * The languages offered in Settings. The choice itself is stored by the platform
 * (Android 13+) or by AppCompat (`autoStoreLocales`, Android 12 and below), which also
 * recreate the screens when it changes. [SYSTEM] follows the phone's language.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    FRENCH("fr");

    companion object {
        /** The current choice. Call on the main thread, while an Activity exists. */
        fun current(): AppLanguage = fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

        /** Maps a comma-separated tag list ("fr-CA,en") to an offered language, by its first entry. */
        fun fromTags(tags: String): AppLanguage {
            val language = tags.substringBefore(',').substringBefore('-').lowercase()
            return entries.firstOrNull { it != SYSTEM && it.tag == language } ?: SYSTEM
        }

        /** Applies [language] to the whole app. Call on the main thread. */
        fun apply(language: AppLanguage) {
            val locales = if (language == SYSTEM) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(language.tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }

        /** The locale the app shows: the chosen one, or the phone's when [SYSTEM]. */
        fun displayLocale(): Locale =
            AppCompatDelegate.getApplicationLocales()[0]
                ?: ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
                ?: Locale.getDefault()

        /**
         * [context] with its resources in [displayLocale]. Needed for the Application context,
         * which does not follow the app language on Android 12 and below.
         */
        fun localizedContext(context: Context): Context {
            val config = Configuration(context.resources.configuration).apply { setLocale(displayLocale()) }
            return context.createConfigurationContext(config)
        }
    }
}
