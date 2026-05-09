package com.example.unstablestudio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ShiftState {
    OFF, ON, CAPS_LOCK
}

@Composable
fun CodingKeyboard(
    onKeyPress: (String, ctrl: Boolean, alt: Boolean) -> Unit,
    onSpecialKey: (SpecialKey) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var shiftState by remember { mutableStateOf(ShiftState.OFF) }
    var ctrlEnabled by remember { mutableStateOf(false) }
    var altEnabled by remember { mutableStateOf(false) }
    var symbolLayer by remember { mutableStateOf(false) }
    
    var lastShiftTapTime by remember { mutableLongStateOf(0L) }

    val numberRow = listOf(
        KeySpec("1", "!"), KeySpec("2", "@"), KeySpec("3", "#"), KeySpec("4", "$"), KeySpec("5", "%"),
        KeySpec("6", "^"), KeySpec("7", "&"), KeySpec("8", "*"), KeySpec("9", "("), KeySpec("0", ")")
    )
    val qwertyRow = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val asdfRow = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val zxcvRow = listOf("z", "x", "c", "v", "b", "n", "m")
    val symbolRows = listOf(
        listOf(KeySpec("-", "_"), KeySpec("/", "\\"), KeySpec(":", ";"), KeySpec(";", ":"), KeySpec("(", "["), KeySpec(")", "]"), KeySpec("$"), KeySpec("&"), KeySpec("@"), KeySpec("\"")),
        listOf(KeySpec(".", ">"), KeySpec(",", "<"), KeySpec("?", "/"), KeySpec("!", "!"), KeySpec("'", "\""), KeySpec("+"), KeySpec("="), KeySpec("*"), KeySpec("#"), KeySpec("%"))
    )

    fun resetModifiers() {
        ctrlEnabled = false
        altEnabled = false
    }

    fun emitText(keySpec: KeySpec) {
        val isShifted = shiftState != ShiftState.OFF
        val output = when {
            keySpec.primary.length == 1 && keySpec.primary[0].isLetter() -> {
                if (isShifted) keySpec.primary.uppercase() else keySpec.primary.lowercase()
            }
            isShifted && keySpec.shifted != null -> keySpec.shifted
            else -> keySpec.primary
        }
        onKeyPress(output, ctrlEnabled, altEnabled)
        if (shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }
        resetModifiers()
    }

    fun emitRaw(text: String) {
        onKeyPress(text, ctrlEnabled, altEnabled)
        if (shiftState == ShiftState.ON) {
            shiftState = ShiftState.OFF
        }
        resetModifiers()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShortcutRow(
                onEscape = { onSpecialKey(SpecialKey.ESCAPE) },
                onTab = { onSpecialKey(SpecialKey.TAB) },
                onCtrlToggle = { ctrlEnabled = !ctrlEnabled },
                ctrlEnabled = ctrlEnabled,
                onAltToggle = { altEnabled = !altEnabled },
                altEnabled = altEnabled,
                onLeft = { onSpecialKey(SpecialKey.ARROW_LEFT) },
                onDown = { onSpecialKey(SpecialKey.ARROW_DOWN) },
                onUp = { onSpecialKey(SpecialKey.ARROW_UP) },
                onRight = { onSpecialKey(SpecialKey.ARROW_RIGHT) },
                onClose = onClose
            )

            if (symbolLayer) {
                KeyRow(keys = numberRow) { emitText(it) }
                symbolRows.forEach { row ->
                    KeyRow(keys = row) { emitText(it) }
                }
                BottomRow(
                    leftLabel = "ABC",
                    leftOnClick = {
                        symbolLayer = false
                        resetModifiers()
                    },
                    middleLeftLabel = ",",
                    middleLeftSecondary = "<",
                    middleLeftOnClick = { emitText(KeySpec(",", "<")) },
                    centerLabel = "Space",
                    centerOnClick = { emitRaw(" ") },
                    middleRightLabel = ".",
                    middleRightSecondary = ">",
                    middleRightOnClick = { emitText(KeySpec(".", ">")) },
                    rightLabel = "Enter",
                    rightOnClick = {
                        onSpecialKey(SpecialKey.ENTER)
                        resetModifiers()
                    }
                )
            } else {
                KeyRow(keys = qwertyRow.map { KeySpec(it) }) { emitText(it) }
                KeyRow(keys = asdfRow.map { KeySpec(it) }, startWeight = 0.5f, endWeight = 0.5f) { emitText(it) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shiftIcon = when (shiftState) {
                        ShiftState.OFF -> Icons.Default.KeyboardArrowUp
                        ShiftState.ON -> Icons.Default.KeyboardDoubleArrowUp
                        ShiftState.CAPS_LOCK -> Icons.Default.KeyboardCapslock
                    }
                    val shiftActive = shiftState != ShiftState.OFF
                    
                    ModifierKey(
                        icon = shiftIcon,
                        active = shiftActive,
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastShiftTapTime < 300) {
                                shiftState = ShiftState.CAPS_LOCK
                            } else {
                                shiftState = when (shiftState) {
                                    ShiftState.OFF -> ShiftState.ON
                                    ShiftState.ON -> ShiftState.OFF
                                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                                }
                            }
                            lastShiftTapTime = now
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                    zxcvRow.forEach { key ->
                        KeyButton(
                            label = if (shiftState != ShiftState.OFF) key.uppercase() else key.lowercase(),
                            onClick = { emitText(KeySpec(key)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModifierKey(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        onClick = {
                            onSpecialKey(SpecialKey.BACKSPACE)
                            resetModifiers()
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
                BottomRow(
                    leftLabel = "?123",
                    leftOnClick = {
                        symbolLayer = true
                        resetModifiers()
                    },
                    middleLeftLabel = ",",
                    middleLeftSecondary = "<",
                    middleLeftOnClick = { emitText(KeySpec(",", "<")) },
                    centerLabel = "Space",
                    centerOnClick = { emitRaw(" ") },
                    middleRightLabel = ".",
                    middleRightSecondary = ">",
                    middleRightOnClick = { emitText(KeySpec(".", ">")) },
                    rightLabel = "Enter",
                    rightOnClick = {
                        onSpecialKey(SpecialKey.ENTER)
                        resetModifiers()
                    }
                )
            }
        }
    }
}

@Composable
private fun ShortcutRow(
    onEscape: () -> Unit,
    onTab: () -> Unit,
    onCtrlToggle: () -> Unit,
    ctrlEnabled: Boolean,
    onAltToggle: () -> Unit,
    altEnabled: Boolean,
    onLeft: () -> Unit,
    onDown: () -> Unit,
    onUp: () -> Unit,
    onRight: () -> Unit,
    onClose: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ModifierKey(label = "Esc", onClick = onEscape, modifier = Modifier.weight(1f), height = 36.dp)
        ModifierKey(label = "Tab", onClick = onTab, modifier = Modifier.weight(1f), height = 36.dp)
        ModifierKey(label = "Ctrl", active = ctrlEnabled, onClick = onCtrlToggle, modifier = Modifier.weight(1f), height = 36.dp)
        ModifierKey(label = "Alt", active = altEnabled, onClick = onAltToggle, modifier = Modifier.weight(1f), height = 36.dp)
        ModifierKey(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = onLeft, modifier = Modifier.weight(1f), height = 36.dp)
        ModifierKey(icon = Icons.Default.KeyboardArrowDown, onClick = onDown, modifier = Modifier.weight(1f), height = 36.dp)
        ModifierKey(icon = Icons.Default.KeyboardArrowUp, onClick = onUp, modifier = Modifier.weight(1f), height = 36.dp)
        ModifierKey(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick = onRight, modifier = Modifier.weight(1f), height = 36.dp)
        ModifierKey(icon = Icons.Default.KeyboardHide, onClick = onClose, modifier = Modifier.weight(1f), height = 36.dp)
    }
}

@Composable
private fun BottomRow(
    leftLabel: String,
    leftOnClick: () -> Unit,
    middleLeftLabel: String,
    middleLeftSecondary: String? = null,
    middleLeftOnClick: () -> Unit,
    centerLabel: String,
    centerOnClick: () -> Unit,
    middleRightLabel: String,
    middleRightSecondary: String? = null,
    middleRightOnClick: () -> Unit,
    rightLabel: String,
    rightOnClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ModifierKey(label = leftLabel, onClick = leftOnClick, modifier = Modifier.weight(1.3f))
        KeyButton(label = middleLeftLabel, secondaryLabel = middleLeftSecondary, onClick = middleLeftOnClick, modifier = Modifier.weight(1f))
        KeyButton(label = centerLabel, onClick = centerOnClick, modifier = Modifier.weight(3.2f))
        KeyButton(label = middleRightLabel, secondaryLabel = middleRightSecondary, onClick = middleRightOnClick, modifier = Modifier.weight(1f))
        ModifierKey(
            label = rightLabel, 
            onClick = rightOnClick, 
            modifier = Modifier.weight(1.5f),
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColorOverride = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun KeyRow(
    keys: List<KeySpec>,
    startWeight: Float = 0f,
    endWeight: Float = 0f,
    onClick: (KeySpec) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (startWeight > 0f) Spacer(modifier = Modifier.weight(startWeight))
        keys.forEach { key ->
            KeyButton(
                label = key.primary,
                secondaryLabel = key.shifted,
                onClick = { onClick(key) },
                modifier = Modifier.weight(1f)
            )
        }
        if (endWeight > 0f) Spacer(modifier = Modifier.weight(endWeight))
    }
}

@Composable
private fun KeyButton(
    label: String,
    secondaryLabel: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp)
                )
            }
            Text(
                text = label,
                fontSize = if (label.length > 2) 14.sp else 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModifierKey(
    label: String? = null,
    icon: ImageVector? = null,
    active: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    contentColorOverride: Color? = null,
    height: androidx.compose.ui.unit.Dp = 48.dp
) {
    val bgColor = backgroundColor ?: if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = contentColorOverride ?: if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
            } else if (label != null) {
                Text(
                    text = label,
                    fontSize = if (label.length > 4) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}

sealed class SpecialKey {
    object CTRL : SpecialKey()
    object ALT : SpecialKey()
    object TAB : SpecialKey()
    object ENTER : SpecialKey()
    object ESCAPE : SpecialKey()
    object BACKSPACE : SpecialKey()
    object DELETE : SpecialKey()
    object ARROW_UP : SpecialKey()
    object ARROW_DOWN : SpecialKey()
    object ARROW_LEFT : SpecialKey()
    object ARROW_RIGHT : SpecialKey()
    object EXPLORER : SpecialKey()
}

private data class KeySpec(
    val primary: String,
    val shifted: String? = null
)
