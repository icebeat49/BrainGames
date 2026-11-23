package com.payfacto.braingames.feature.kakuro.model

sealed class KakuroCell {
    object Black : KakuroCell()
    data class Clue(val downSum: Int, val acrossSum: Int) : KakuroCell()
    data class Input(val solution: Int, val current: Int = 0) : KakuroCell()
}
