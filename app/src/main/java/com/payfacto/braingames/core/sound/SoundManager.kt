package com.payfacto.braingames.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.payfacto.braingames.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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
        soundIds[SoundEvent.TAP]     = soundPool.load(context, R.raw.sound_tap, 1)
        soundIds[SoundEvent.CORRECT] = soundPool.load(context, R.raw.sound_correct, 1)
        soundIds[SoundEvent.ERROR]   = soundPool.load(context, R.raw.sound_error, 1)
        soundIds[SoundEvent.WIN]     = soundPool.load(context, R.raw.sound_win, 1)
    }

    fun play(event: SoundEvent) {
        if (isMuted) return
        soundIds[event]?.let { soundPool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun release() {
        soundPool.release()
    }
}
