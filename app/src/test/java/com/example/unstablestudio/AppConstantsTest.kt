package com.example.unstablestudio

import com.example.unstablestudio.core.config.AppConstants
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for configuration constants.
 */
class AppConstantsTest {
    
    @Test
    fun testLspConstants_areValid() {
        assertEquals("tcp", AppConstants.Lsp.DEFAULT_MODE)
        assertEquals("127.0.0.1", AppConstants.Lsp.DEFAULT_HOST)
        assertEquals(9999, AppConstants.Lsp.DEFAULT_PORT)
        assertTrue(AppConstants.Lsp.DID_CHANGE_DEBOUNCE_MS > 0)
    }
    
    @Test
    fun testEditorConstants_areValid() {
        assertTrue(AppConstants.Editor.MIN_FONT_SIZE < AppConstants.Editor.MAX_FONT_SIZE)
        assertTrue(AppConstants.Editor.DEFAULT_FONT_SIZE in AppConstants.Editor.MIN_FONT_SIZE..AppConstants.Editor.MAX_FONT_SIZE)
        assertTrue(AppConstants.Editor.FONT_SIZE_STEPS > 0)
    }
    
    @Test
    fun testPerformanceConstants_areValid() {
        assertTrue(AppConstants.Performance.FPS_POWER_SAVING < AppConstants.Performance.FPS_STANDARD)
        assertTrue(AppConstants.Performance.FPS_STANDARD < AppConstants.Performance.FPS_SMOOTH)
        assertEquals(3, AppConstants.Performance.FPS_OPTIONS.size)
    }
}
