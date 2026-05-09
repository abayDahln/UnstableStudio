package com.example.unstablestudio.domain.undo

import java.util.Stack

class UndoManager(private val maxStackSize: Int = 100) {
    private val undoStack = Stack<UndoCommand>()
    private val redoStack = Stack<UndoCommand>()

    fun pushCommand(command: UndoCommand) {
        if (undoStack.isNotEmpty()) {
            val last = undoStack.peek()
            if (last.canMerge(command)) {
                undoStack.pop()
                undoStack.push(last.merge(command))
                redoStack.clear()
                return
            }
        }

        undoStack.push(command)
        if (undoStack.size > maxStackSize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val command = undoStack.pop()
            command.undo()
            redoStack.push(command)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val command = redoStack.pop()
            command.redo()
            undoStack.push(command)
        }
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
}
