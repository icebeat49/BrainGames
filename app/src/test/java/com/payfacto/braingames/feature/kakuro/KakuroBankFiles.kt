package com.payfacto.braingames.feature.kakuro

import com.payfacto.braingames.feature.kakuro.data.KakuroBankFormat
import com.payfacto.braingames.feature.kakuro.engine.KakuroPuzzle
import java.io.File

/** The bundled bank files, read straight from the sources (unit tests run in the app module). */
object KakuroBankFiles {
    fun file(size: Int) = File("src/main/res/raw/kakuro_bank_$size.txt")

    fun load(size: Int): List<KakuroPuzzle> = KakuroBankFormat.parse(file(size).readText())
}
