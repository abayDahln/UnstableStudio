package com.example.unstablestudio.ui.components

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.text.Content

object TypingAssist {

    private val PAIRS = mapOf(
        '(' to ')',
        '[' to ']',
        '{' to '}',
        '"' to '"',
        '\'' to '\''
    )

    fun handleKeyPress(editor: CodeEditor, text: String): Boolean {
        if (text.length != 1) {
            editor.insertText(text, text.length)
            return true
        }
        val char = text[0]
        val cursor = editor.cursor
        val content = editor.text

        // Handle selection wrapping
        if (cursor.isSelected && char in PAIRS.keys) {
            val closer = PAIRS[char]!!
            val startLine = cursor.leftLine
            val startCol = cursor.leftColumn
            val endLine = cursor.rightLine
            val endCol = cursor.rightColumn
            
            content.insert(endLine, endCol, closer.toString())
            content.insert(startLine, startCol, char.toString())
            // Restore selection roughly (Sora handles selection differently after insert)
            editor.setSelectionRegion(startLine, startCol + 1, endLine, if (startLine == endLine) endCol + 1 else endCol)
            return true
        }

        // Handle over-typing
        if (!cursor.isSelected && char in PAIRS.values) {
            val line = cursor.leftLine
            val col = cursor.leftColumn
            if (col < content.getLineString(line).length) {
                val nextChar = content.getLineString(line)[col]
                if (nextChar == char) {
                    cursor.set(line, col + 1)
                    return true
                }
            }
        }

        // Handle auto-closing
        if (!cursor.isSelected && char in PAIRS.keys) {
            val closer = PAIRS[char]!!
            val line = cursor.leftLine
            val col = cursor.leftColumn
            
            // Only auto-close if at end of line or before whitespace/closer
            val lineText = content.getLineString(line)
            val shouldAutoClose = col >= lineText.length || 
                                lineText[col].isWhitespace() || 
                                (PAIRS.values.contains(lineText[col]))

            if (shouldAutoClose) {
                content.insert(line, col, char.toString() + closer.toString())
                cursor.set(line, col + 1)
                return true
            }
        }
        
        // If not special assist, just insert the character
        editor.insertText(char.toString(), 1)
        return true
    }

    fun handleBackspace(editor: CodeEditor): Boolean {
        val cursor = editor.cursor
        val content = editor.text
        if (cursor.isSelected) return false

        val line = cursor.leftLine
        val col = cursor.leftColumn
        val lineText = content.getLineString(line)

        if (col > 0 && col < lineText.length) {
            val prev = lineText[col - 1]
            val next = lineText[col]
            if (PAIRS[prev] == next) {
                content.delete(line, col - 1, line, col + 1)
                return true
            }
        }
        return false
    }

    fun handleEnter(editor: CodeEditor): Boolean {
        val cursor = editor.cursor
        val content = editor.text
        val line = cursor.leftLine
        val col = cursor.leftColumn
        val lineText = content.getLineString(line)

        val currentIndentation = getIndentation(lineText)
        var nextIndentation = currentIndentation

        val isAfterOpeningBrace = col > 0 && lineText[col - 1] == '{'
        val isBeforeClosingBrace = col < lineText.length && lineText[col] == '}'

        if (isAfterOpeningBrace) {
            nextIndentation += "    " // 4 spaces for indent
        }

        if (isAfterOpeningBrace && isBeforeClosingBrace) {
            content.insert(line, col, "\n$nextIndentation\n$currentIndentation")
            cursor.set(line + 1, nextIndentation.length)
        } else {
            content.insert(line, col, "\n$nextIndentation")
            cursor.set(line + 1, nextIndentation.length)
        }
        return true
    }

    private fun getIndentation(line: String): String {
        val sb = StringBuilder()
        for (c in line) {
            if (c == ' ' || c == '\t') {
                sb.append(c)
            } else {
                break
            }
        }
        return sb.toString()
    }
}
