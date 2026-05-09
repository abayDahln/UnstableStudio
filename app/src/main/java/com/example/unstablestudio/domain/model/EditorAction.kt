package com.example.unstablestudio.domain.model

sealed class EditorAction {
    object Undo : EditorAction()
    object Redo : EditorAction()
    object Cut : EditorAction()
    object Copy : EditorAction()
    object Paste : EditorAction()
    object SelectAll : EditorAction()
    object SelectLine : EditorAction()
    object CopyLineUp : EditorAction()
    object CopyLineDown : EditorAction()
    object MoveLineUp : EditorAction()
    object MoveLineDown : EditorAction()
    object ToggleLineComment : EditorAction()
    object ToggleBlockComment : EditorAction()
    object Find : EditorAction()
    object Replace : EditorAction()
    object Save : EditorAction()
    object CloseTab : EditorAction()
    object ToggleSidebar : EditorAction()
    object ToggleWordWrap : EditorAction()
    object ZoomIn : EditorAction()
    object ZoomOut : EditorAction()
    object ZoomReset : EditorAction()
    object ExpandSelection : EditorAction()
    object ShrinkSelection : EditorAction()
    object DeleteLine : EditorAction()
    object GotoLine : EditorAction()
    object DuplicateLine : EditorAction()
    
    data class PerformTextChange(
        val startOffset: Int,
        val deleteCount: Int,
        val insertText: String,
        val cursor: Int
    ) : EditorAction()
}
