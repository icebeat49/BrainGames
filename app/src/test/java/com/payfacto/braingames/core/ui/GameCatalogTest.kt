package com.payfacto.braingames.core.ui

import com.payfacto.braingames.core.model.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCatalogTest {

    private val infos = GameType.entries.associateWith(GameCatalog::infoFor)

    @Test
    fun `every game type has a title and a description`() {
        infos.forEach { (game, info) ->
            assertTrue("$game has no title", info.titleRes != 0)
            assertTrue("$game has no description", info.descriptionRes != 0)
        }
    }

    @Test
    fun `no two games share a title`() {
        val titles = infos.values.map { it.titleRes }

        assertEquals(titles.size, titles.toSet().size)
    }
}
