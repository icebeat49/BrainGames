package com.payfacto.braingames.core.sound

enum class SoundEvent {
    /** A tap on a control, or a mark (X / ?) written or erased. */
    TAP,
    CORRECT,
    ERROR,
    WIN,

    /** A crown placed on the board. Says nothing about whether it is right. */
    CROWN_PLACED,

    /** Classic only: the move broke a rule. Never played in Hardcore. */
    CONFLICT,

    /** A hint was applied. */
    HINT,

    /** Hardcore: Check found mistakes. */
    CHECK_FAILED
}
