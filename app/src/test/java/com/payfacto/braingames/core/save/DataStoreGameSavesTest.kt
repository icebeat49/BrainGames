package com.payfacto.braingames.core.save

import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import com.payfacto.braingames.core.model.GameType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreGameSavesTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val save = GameSave(Difficulty.EASY, GameMode.CLASSIC, mapOf("cells" to intArrayOf(1, 2, 3)))

    /** One app run: its own scope, as the real store lives as long as the app. */
    private inner class AppRun {
        private val job = Job()
        val saves = DataStoreGameSaves(folder.root, CoroutineScope(Dispatchers.IO + job))

        fun close() = runBlocking { job.cancelAndJoin() }
    }

    private fun fileOf(game: GameType) = File(folder.root, "${game.name.lowercase()}.save")

    /** Waits until the file on disk holds [expected]. */
    private fun awaitFile(game: GameType, expected: SaveSlot) = runBlocking {
        withTimeout(5_000) {
            while (readFile(game) != expected) delay(10)
        }
    }

    private suspend fun readFile(game: GameType): SaveSlot? =
        fileOf(game).takeIf { it.exists() }?.inputStream()?.use { GameSaveSerializer.readFrom(it) }

    @Test
    fun `a save is there after the app restarts`() {
        val first = AppRun()
        first.saves.save(GameType.SUDOKU, save)
        awaitFile(GameType.SUDOKU, SaveSlot(save))
        first.close()

        val second = AppRun()
        val loaded = runBlocking { second.saves.load(GameType.SUDOKU) }
        second.close()

        assertEquals(save, loaded)
    }

    @Test
    fun `a save is visible at once, before the file is written`() {
        val run = AppRun()

        run.saves.save(GameType.CROWNS, save)
        val loaded = runBlocking { run.saves.load(GameType.CROWNS) }
        run.close()

        assertEquals(save, loaded)
    }

    /**
     * Only checked in memory: on the Windows JVM DataStore can't replace an existing file
     * (its rename refuses to overwrite), while on Android it can.
     */
    @Test
    fun `clearing removes the save`() {
        val run = AppRun()
        run.saves.save(GameType.KAKURO, save)
        run.saves.clear(GameType.KAKURO)

        assertNull(runBlocking { run.saves.load(GameType.KAKURO) })
        run.close()
    }

    @Test
    fun `each game has its own save`() {
        val run = AppRun()
        run.saves.save(GameType.SUDOKU, save)

        assertNull(runBlocking { run.saves.load(GameType.KAKURO) })
        run.close()
    }

    @Test
    fun `a damaged file reads as no save`() {
        fileOf(GameType.SUDOKU).writeBytes(byteArrayOf(0, 0, 0, GameSaveSerializer.VERSION.toByte(), 1, 0))

        val run = AppRun()
        val loaded = runBlocking { run.saves.load(GameType.SUDOKU) }
        run.close()

        assertNull(loaded)
    }
}
