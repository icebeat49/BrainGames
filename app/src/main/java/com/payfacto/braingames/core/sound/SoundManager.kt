package com.payfacto.braingames.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.payfacto.braingames.R
import com.payfacto.braingames.core.config.FeatureFlags
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide sound player. The [SoundPool] lives as long as the process and is never
 * released explicitly: an Activity must not release it, because Activities are
 * recreated (rotation, theme change) while this singleton survives.
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = mutableMapOf<SoundEvent, Int>()
    var isMuted: Boolean = false

    init {
        if (FeatureFlags.isSoundEnabled) loadSounds()
    }

    /** Does nothing while sound is turned off by [FeatureFlags.isSoundEnabled] or muted. */
    fun play(event: SoundEvent) {
        if (!FeatureFlags.isSoundEnabled || isMuted) return
        soundIds[event]?.let { soundPool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    private fun loadSounds() {
        val loaded = mutableMapOf<Int, Int>()
        SoundEvent.entries.forEach { event ->
            val resource = resourceFor(event)
            soundIds[event] = loaded.getOrPut(resource) { soundPool.load(context, resource, 1) }
        }
    }

    private companion object {
        /**
         * Sound file per event. Events without a file of their own yet share the closest one:
         * add res/raw/sound_crown.ogg, sound_conflict.ogg, sound_hint.ogg and
         * sound_check_failed.ogg, then point their events at them here.
         */
        fun resourceFor(event: SoundEvent): Int = when (event) {
            SoundEvent.TAP -> R.raw.sound_tap
            SoundEvent.CORRECT -> R.raw.sound_correct
            SoundEvent.ERROR -> R.raw.sound_error
            SoundEvent.WIN -> R.raw.sound_win
            SoundEvent.CROWN_PLACED -> R.raw.sound_tap
            SoundEvent.CONFLICT -> R.raw.sound_error
            SoundEvent.HINT -> R.raw.sound_correct
            SoundEvent.CHECK_FAILED -> R.raw.sound_error
        }
    }
}
