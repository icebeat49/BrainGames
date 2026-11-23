package com.payfacto.braingames.feature.crowns

import com.payfacto.braingames.feature.crowns.data.CrownsBankFormat
import com.payfacto.braingames.feature.crowns.model.CrownsPuzzle
import java.io.File

/** The bundled bank files, read straight from the sources (unit tests run in the app module). */
object CrownsBankFiles {
    fun file(size: Int) = File("src/main/res/raw/crowns_bank_$size.txt")

    fun load(size: Int): List<CrownsPuzzle> = CrownsBankFormat.parse(file(size).readText())
}
