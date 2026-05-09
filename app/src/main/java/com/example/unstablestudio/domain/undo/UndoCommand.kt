package com.example.unstablestudio.domain.undo

interface UndoCommand {
    fun undo()
    fun redo()
    fun canMerge(other: UndoCommand): Boolean
    fun merge(other: UndoCommand): UndoCommand
}

data class TextChangeCommand(
    val startOffset: Int,
    val oldText: String,
    val newText: String,
    val oldCursor: Int,
    val newCursor: Int,
    val onApply: (offset: Int, count: Int, text: String, cursor: Int) -> Unit
) : UndoCommand {
    
    override fun undo() {
        // Remove new text, insert old text
        onApply(startOffset, newText.length, oldText, oldCursor)
    }

    override fun redo() {
        // Remove old text, insert new text
        onApply(startOffset, oldText.length, newText, newCursor)
    }

    private enum class Category { ALPHANUMERIC, WHITESPACE, SYMBOL }

    private fun getCategory(text: String): Category {
        if (text.isEmpty()) return Category.SYMBOL
        val char = text.first()
        return when {
            char.isLetterOrDigit() -> Category.ALPHANUMERIC
            char.isWhitespace() -> Category.WHITESPACE
            else -> Category.SYMBOL
        }
    }

    override fun canMerge(other: UndoCommand): Boolean {
        if (other !is TextChangeCommand) return false
        
        // Merge if typing is sequential (insert after insert) and belongs to same category
        val isTypingSequentially = oldText.isEmpty() && other.oldText.isEmpty() &&
                other.startOffset == this.startOffset + this.newText.length &&
                getCategory(this.newText) == getCategory(other.newText)
                
        // Merge if deleting is sequential (backspace) and belongs to same category
        val isDeletingSequentially = newText.isEmpty() && other.newText.isEmpty() &&
                other.startOffset + other.oldText.length == this.startOffset &&
                getCategory(this.oldText) == getCategory(other.oldText)

        return isTypingSequentially || isDeletingSequentially
    }

    override fun merge(other: UndoCommand): UndoCommand {
        val next = other as TextChangeCommand
        return if (next.startOffset > this.startOffset) {
            // Typing forward
            copy(
                newText = this.newText + next.newText,
                newCursor = next.newCursor
            )
        } else {
            // Deleting backward
            copy(
                startOffset = next.startOffset,
                oldText = next.oldText + this.oldText,
                newCursor = next.newCursor
            )
        }
    }
}
