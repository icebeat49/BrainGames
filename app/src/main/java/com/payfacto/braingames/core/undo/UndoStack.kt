package com.payfacto.braingames.core.undo

/**
 * Last-in, first-out history of snapshots taken before each change.
 * Keeps at most [maxSize] entries; the oldest are dropped first.
 *
 * The stack itself is not persisted. A game saves [toList] next to its board and rebuilds
 * the stack from it, so it decides how to encode its own snapshot type.
 */
class UndoStack<T>(
    private val maxSize: Int = DEFAULT_MAX_SIZE,
    initial: List<T> = emptyList()
) {
    private val entries = ArrayDeque(initial.takeLast(maxSize))

    val canUndo: Boolean get() = entries.isNotEmpty()

    val size: Int get() = entries.size

    fun push(snapshot: T) {
        entries.addLast(snapshot)
        if (entries.size > maxSize) entries.removeFirst()
    }

    /** The most recent snapshot, or null when there is nothing to undo. */
    fun pop(): T? = entries.removeLastOrNull()

    fun clear() = entries.clear()

    /** Oldest first, ready to be saved and passed back as `initial`. */
    fun toList(): List<T> = entries.toList()

    companion object {
        const val DEFAULT_MAX_SIZE = 500
    }
}
