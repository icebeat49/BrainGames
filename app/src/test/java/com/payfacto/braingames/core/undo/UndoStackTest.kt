package com.payfacto.braingames.core.undo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoStackTest {

    @Test
    fun `an empty stack has nothing to undo`() {
        val stack = UndoStack<String>()

        assertFalse(stack.canUndo)
        assertNull(stack.pop())
    }

    @Test
    fun `pop returns snapshots last in, first out`() {
        val stack = UndoStack<String>()
        listOf("a", "b", "c").forEach(stack::push)

        val popped = List(3) { stack.pop() }

        assertEquals(listOf("c", "b", "a"), popped)
        assertFalse(stack.canUndo)
    }

    @Test
    fun `the oldest snapshot is dropped beyond the size limit`() {
        val stack = UndoStack<Int>(maxSize = 3)

        (1..5).forEach(stack::push)

        assertEquals(listOf(3, 4, 5), stack.toList())
    }

    @Test
    fun `a stack rebuilt from toList continues where it was`() {
        val original = UndoStack<Int>()
        (1..3).forEach(original::push)

        val rebuilt = UndoStack(initial = original.toList())

        assertTrue(rebuilt.canUndo)
        assertEquals(3, rebuilt.pop())
        assertEquals(2, rebuilt.size)
    }

    @Test
    fun `clear empties the stack`() {
        val stack = UndoStack<Int>()
        stack.push(1)

        stack.clear()

        assertFalse(stack.canUndo)
    }
}
