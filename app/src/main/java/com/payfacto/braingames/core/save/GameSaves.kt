package com.payfacto.braingames.core.save

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.payfacto.braingames.core.model.GameType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** The one saved game in progress per game type. An interface so ViewModel tests can keep it in memory. */
interface GameSaves {
    /** The current save, or null; emits again whenever it changes. */
    fun saved(game: GameType): Flow<GameSave?>

    suspend fun load(game: GameType): GameSave? = saved(game).first()

    /** Replaces the save. Returns at once; the write finishes in the background. */
    fun save(game: GameType, save: GameSave)

    /** Deletes the save. Returns at once, like [save]. */
    fun clear(game: GameType)
}

/**
 * One DataStore file per game in [directory] (`<game>.save`). Writes run on [scope], which
 * outlives screens, so the last move is written even when the player leaves right after it.
 * Only the newest pending write matters, so a burst of moves writes once.
 *
 * [saved] answers from memory as soon as anything was saved or cleared in this run, so a
 * game reopened right after leaving sees its last move even if the file is still being written.
 */
class DataStoreGameSaves(
    private val directory: File,
    private val scope: CoroutineScope
) : GameSaves {

    private val slots = ConcurrentHashMap<GameType, Slot>()

    override fun saved(game: GameType): Flow<GameSave?> = slotFor(game).saved

    override fun save(game: GameType, save: GameSave) = slotFor(game).put(SaveSlot(save))

    override fun clear(game: GameType) = slotFor(game).put(SaveSlot(null))

    private fun slotFor(game: GameType): Slot = slots.computeIfAbsent(game) {
        Slot(
            DataStoreFactory.create(
                serializer = GameSaveSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler { GameSaveSerializer.defaultValue },
                scope = scope,
                produceFile = { File(directory, "${game.name.lowercase()}.save") }
            )
        )
    }

    private inner class Slot(private val store: DataStore<SaveSlot>) {
        /** Null until the file has been read or something was put. */
        private val latest = MutableStateFlow<SaveSlot?>(null)
        private val pending = Channel<SaveSlot>(Channel.CONFLATED)

        val saved: Flow<GameSave?> = latest.filterNotNull().map { it.save }

        /**
         * A save is a convenience: a file that can't be read counts as no save, and a failed
         * write is skipped (the next move writes again). Neither may stop the game.
         */
        init {
            scope.launch {
                val stored = try {
                    store.data.first()
                } catch (e: IOException) {
                    GameSaveSerializer.defaultValue
                }
                latest.compareAndSet(null, stored)
            }
            scope.launch {
                for (slot in pending) {
                    try {
                        store.updateData { slot }
                    } catch (e: IOException) {
                        // Kept in memory for this run; written with the next change.
                    }
                }
            }
        }

        fun put(slot: SaveSlot) {
            latest.value = slot
            pending.trySend(slot)
        }
    }
}
