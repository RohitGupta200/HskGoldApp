package org.cap.gold.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * A centralized status dialog that can be used from any screen.
 *
 * - Shows a circular icon with a check (success=true) or a cross (success=false)
 * - Renders message and subMessage when provided (non-empty)
 * - Look and feel matches the provided design: rounded white card, soft elevation,
 *   large icon on top, bold title, colored subtitle.
 */
class StatusDialogState(
    initialVisible: Boolean = false,
    initialSuccess: Boolean = true,
    initialMessage: String? = null,
    initialSubMessage: String? = null
) {
    val visible: MutableState<Boolean> = mutableStateOf(initialVisible)
    val success: MutableState<Boolean> = mutableStateOf(initialSuccess)
    val message: MutableState<String?> = mutableStateOf(initialMessage)
    val subMessage: MutableState<String?> = mutableStateOf(initialSubMessage)

    fun show(success: Boolean, message: String? = null, subMessage: String? = null) {
        this.success.value = success
        this.message.value = message
        this.subMessage.value = subMessage
        this.visible.value = true
    }
    fun hide() { visible.value = false }
}

@Composable
fun rememberStatusDialogState(
    initialVisible: Boolean = false,
    initialSuccess: Boolean = true,
    initialMessage: String? = null,
    initialSubMessage: String? = null
): StatusDialogState = remember {
    StatusDialogState(
        initialVisible = initialVisible,
        initialSuccess = initialSuccess,
        initialMessage = initialMessage,
        initialSubMessage = initialSubMessage
    )
}

// CompositionLocal to access the dialog state anywhere under the provider
val LocalStatusDialogState = staticCompositionLocalOf<StatusDialogState> {
    error("StatusDialogState not provided")
}

// Add-Field dialog state and Local
class AddFieldDialogState(
    initialVisible: Boolean = false
) {
    val visible: MutableState<Boolean> = mutableStateOf(initialVisible)
    private var onAdd: ((String, String) -> Unit)? = null

    fun show(onAdd: (String, String) -> Unit) {
        this.onAdd = onAdd
        this.visible.value = true
    }

    fun hide() {
        this.visible.value = false
        this.onAdd = null
    }

    internal fun submit(label: String, value: String) {
        onAdd?.invoke(label, value)
    }
}

@Composable
fun rememberAddFieldDialogState(initialVisible: Boolean = false): AddFieldDialogState = remember {
    AddFieldDialogState(initialVisible)
}

val LocalAddFieldDialogState = staticCompositionLocalOf<AddFieldDialogState> {
    error("AddFieldDialogState not provided")
}

/**
 * Provides a centralized StatusDialog state and renders the dialog above [content].
 * Usage: wrap your app root once, then call LocalStatusDialogState.current.show(...) from any screen.
 */
@Composable
fun ProvideStatusDialog(
    state: StatusDialogState = rememberStatusDialogState(),
    content: @Composable () -> Unit
) {
    val addFieldState = rememberAddFieldDialogState()
    CompositionLocalProvider(
        LocalStatusDialogState provides state,
        LocalAddFieldDialogState provides addFieldState
    ) {
        content()
        StatusDialog(
            visible = state.visible.value,
            success = state.success.value,
            message = state.message.value,
            subMessage = state.subMessage.value,
            onDismissRequest = { state.hide() }
        )
        AddFieldDialog(
            state = addFieldState,
            onDismissRequest = { addFieldState.hide() }
        )
    }
}

@Composable
fun StatusDialog(
    visible: Boolean,
    success: Boolean,
    message: String?,
    subMessage: String?,
    onDismissRequest: () -> Unit = {},
    // Styling hooks (optional)
    iconSize: Dp = 80.dp,
    cornerRadius: Dp = 24.dp,
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(cornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon bubble
                StatusIcon(
                    success = success,
                    iconSize = iconSize,
                    successColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                    errorColor = MaterialTheme.colorScheme.error.copy(alpha = 1f)
                )

                Spacer(Modifier.height(18.dp))

                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B1B1B)
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                if (!subMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = subMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            // Highlight subtext with primary color as in screenshot
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AddFieldDialog(
    state: AddFieldDialogState,
    onDismissRequest: () -> Unit = {}
) {
    if (!state.visible.value) return

    var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Add a Field",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B1B1B)
                    )
                )

                Spacer(Modifier.height(12.dp))

                TextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Label") }
                )

                Spacer(Modifier.height(12.dp))

                TextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Value") }
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        state.submit(label.trim(), value.trim())
                        state.hide()
                        label = ""
                        value = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(
    success: Boolean,
    iconSize: Dp,
    successColor: Color,
    errorColor: Color,
) {
    val bg = if (success) successColor else errorColor

    Box(
        modifier = Modifier
            .size(iconSize)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(iconSize * 0.6f)) {
            if (success) {
                // White check mark
                val path = Path()
                val s = size.minDimension // DrawScope.size (px)
                val start = Offset(x = s * 0.20f, y = s * 0.55f)
                val mid = Offset(x = s * 0.45f, y = s * 0.75f)
                val end = Offset(x = s * 0.80f, y = s * 0.35f)
                path.moveTo(start.x, start.y)
                path.lineTo(mid.x, mid.y)
                path.lineTo(end.x, end.y)
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = (s * 0.10f).coerceAtMost(8.dp.toPx()), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                // White cross
                val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                val r = size.minDimension * 0.30f
                drawLine(
                    color = Color.White,
                    start = Offset(center.x - r, center.y - r),
                    end = Offset(center.x + r, center.y + r),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(center.x + r, center.y - r),
                    end = Offset(center.x - r, center.y + r),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
