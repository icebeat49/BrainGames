package com.payfacto.braingames.core.save

import androidx.datastore.core.CorruptionException
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class GameSaveSerializerTest {

    private val save = GameSave(
        difficulty = Difficulty.HARD,
        mode = GameMode.HARDCORE,
        values = mapOf(
            "an_int" to 42,
            "a_long" to 3_500L,
            "a_flag" to true,
            "some_ints" to intArrayOf(1, 0, 9),
            "some_flags" to booleanArrayOf(true, false)
        )
    )

    private fun bytesOf(slot: SaveSlot): ByteArray =
        ByteArrayOutputStream().also { runBlocking { GameSaveSerializer.writeTo(slot, it) } }.toByteArray()

    private fun read(bytes: ByteArray): SaveSlot =
        runBlocking { GameSaveSerializer.readFrom(ByteArrayInputStream(bytes)) }

    @Test
    fun `a save reads back the same`() {
        assertEquals(SaveSlot(save), read(bytesOf(SaveSlot(save))))
    }

    @Test
    fun `no save reads back as no save`() {
        assertEquals(SaveSlot(null), read(bytesOf(SaveSlot(null))))
    }

    @Test
    fun `a file from another format version reads as no save`() {
        val bytes = ByteArrayOutputStream().also { out ->
            DataOutputStream(out).run {
                writeInt(GameSaveSerializer.VERSION + 1)
                writeBoolean(true)
            }
        }.toByteArray()

        assertEquals(SaveSlot(null), read(bytes))
    }

    @Test(expected = CorruptionException::class)
    fun `a truncated file is reported as corrupt`() {
        val bytes = bytesOf(SaveSlot(save))

        read(bytes.copyOf(bytes.size - 3))
    }

    @Test(expected = CorruptionException::class)
    fun `an unknown difficulty is reported as corrupt`() {
        val bytes = ByteArrayOutputStream().also { out ->
            DataOutputStream(out).run {
                writeInt(GameSaveSerializer.VERSION)
                writeBoolean(true)
                writeUTF("IMPOSSIBLE")
                writeUTF(GameMode.CLASSIC.name)
                writeInt(0)
            }
        }.toByteArray()

        read(bytes)
    }
}
