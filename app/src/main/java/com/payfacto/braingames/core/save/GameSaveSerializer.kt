package com.payfacto.braingames.core.save

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.payfacto.braingames.core.model.Difficulty
import com.payfacto.braingames.core.model.GameMode
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/** What one save file holds: a game in progress, or nothing. */
data class SaveSlot(val save: GameSave?)

/**
 * Compact binary format for a [SaveSlot]:
 * `version, hasSave, [difficulty, mode, count, (key, type, value)…]`.
 *
 * A file written by a different [VERSION] reads as empty, so a format change drops old saves
 * instead of misreading them.
 */
object GameSaveSerializer : Serializer<SaveSlot> {
    const val VERSION = 1

    private const val TYPE_INT = 1
    private const val TYPE_LONG = 2
    private const val TYPE_BOOLEAN = 3
    private const val TYPE_INT_ARRAY = 4
    private const val TYPE_BOOLEAN_ARRAY = 5

    override val defaultValue = SaveSlot(null)

    override suspend fun readFrom(input: InputStream): SaveSlot = try {
        DataInputStream(input).readSlot()
    } catch (e: EOFException) {
        throw CorruptionException("Save file is truncated", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Save file is not readable", e)
    }

    override suspend fun writeTo(t: SaveSlot, output: OutputStream) {
        DataOutputStream(output).run {
            writeSlot(t)
            flush()
        }
    }

    private fun DataInputStream.readSlot(): SaveSlot {
        if (readInt() != VERSION || !readBoolean()) return defaultValue
        val difficulty = Difficulty.valueOf(readUTF())
        val mode = GameMode.valueOf(readUTF())
        val values = buildMap {
            repeat(readInt()) { put(readUTF(), readValue()) }
        }
        return SaveSlot(GameSave(difficulty, mode, values))
    }

    private fun DataInputStream.readValue(): Any = when (val type = readByte().toInt()) {
        TYPE_INT -> readInt()
        TYPE_LONG -> readLong()
        TYPE_BOOLEAN -> readBoolean()
        TYPE_INT_ARRAY -> IntArray(readInt()) { readInt() }
        TYPE_BOOLEAN_ARRAY -> BooleanArray(readInt()) { readBoolean() }
        else -> throw IllegalArgumentException("Unknown value type $type")
    }

    private fun DataOutputStream.writeSlot(slot: SaveSlot) {
        writeInt(VERSION)
        val save = slot.save
        writeBoolean(save != null)
        if (save == null) return
        writeUTF(save.difficulty.name)
        writeUTF(save.mode.name)
        writeInt(save.values.size)
        save.values.forEach { (key, value) ->
            writeUTF(key)
            writeValue(value)
        }
    }

    private fun DataOutputStream.writeValue(value: Any) {
        when (value) {
            is Int -> { writeByte(TYPE_INT); writeInt(value) }
            is Long -> { writeByte(TYPE_LONG); writeLong(value) }
            is Boolean -> { writeByte(TYPE_BOOLEAN); writeBoolean(value) }
            is IntArray -> { writeByte(TYPE_INT_ARRAY); writeInt(value.size); value.forEach(::writeInt) }
            is BooleanArray -> { writeByte(TYPE_BOOLEAN_ARRAY); writeInt(value.size); value.forEach(::writeBoolean) }
            else -> throw IllegalArgumentException("Cannot save ${value::class.simpleName}")
        }
    }
}
