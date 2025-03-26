package com.example.poc_drag_and_drop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.example.poc_drag_and_drop.ui.theme.POC_drag_and_dropTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            POC_drag_and_dropTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DragDropDemo(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun DragDropDemo(modifier: Modifier) {
    // States for numbers in each box.
    var topNumber by remember { mutableStateOf(10) }
    var bottomNumber by remember { mutableStateOf(0) }

    // States for the drag offset of the red box.
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Drag state flags.
    var isDragging by remember { mutableStateOf(false) }
    var isOverDropTarget by remember { mutableStateOf(false) }

    // Capture the red box's initial global position and measured size.
    var redBoxPosition by remember { mutableStateOf(Offset.Zero) }
    var redBoxSize by remember { mutableStateOf(Size.Zero) }

    // Capture the green box's global bounds.
    var greenBoxRect by remember { mutableStateOf(Rect.Zero) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Red draggable box that wraps its content with 22 dp vertical padding.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    redBoxPosition = coordinates.positionInRoot()
                    redBoxSize = coordinates.size.toSize()
                }
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .zIndex(if (isDragging) 1f else 0f)
                .scale(if (isDragging) 0.9f else 1f)
                .background(Color.Red)
                .padding(vertical = 22.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y

                            // Compute the current global rectangle of the red box.
                            val redRect = Rect(
                                redBoxPosition.x + offsetX,
                                redBoxPosition.y + offsetY,
                                redBoxPosition.x + offsetX + redBoxSize.width,
                                redBoxPosition.y + offsetY + redBoxSize.height
                            )
                            val redArea = redBoxSize.width * redBoxSize.height
                            val intersection = redRect.intersectionArea(greenBoxRect)
                            val overlapPercentage = if (redArea > 0) intersection / redArea else 0f
                            isOverDropTarget = overlapPercentage > 0.3f
                        },
                        onDragEnd = {
                            // Final red box rectangle.
                            val redRect = Rect(
                                redBoxPosition.x + offsetX,
                                redBoxPosition.y + offsetY,
                                redBoxPosition.x + offsetX + redBoxSize.width,
                                redBoxPosition.y + offsetY + redBoxSize.height
                            )
                            val redArea = redBoxSize.width * redBoxSize.height
                            val intersection = redRect.intersectionArea(greenBoxRect)
                            val overlapPercentage = if (redArea > 0) intersection / redArea else 0f

                            if (overlapPercentage > 0.3f) {
                                bottomNumber += topNumber
                            }
                            // Reset the drag state.
                            offsetX = 0f
                            offsetY = 0f
                            isDragging = false
                            isOverDropTarget = false
                        },
                        onDragCancel = {
                            offsetX = 0f
                            offsetY = 0f
                            isDragging = false
                            isOverDropTarget = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$topNumber", color = Color.White)
        }

        // Green drop target box that wraps its content with 22 dp vertical padding.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    greenBoxRect = coordinates.boundsInRoot()
                }
                .then(
                    if (isOverDropTarget) Modifier.border(width = 2.dp, color = Color.Blue)
                    else Modifier
                )
                .background(Color.Green)
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$bottomNumber", color = Color.White)
        }
    }
}

// Extension function to calculate the intersection area between two Rects.
fun Rect.intersectionArea(other: Rect): Float {
    val left = max(this.left, other.left)
    val top = max(this.top, other.top)
    val right = min(this.right, other.right)
    val bottom = min(this.bottom, other.bottom)
    return if (right > left && bottom > top) {
        (right - left) * (bottom - top)
    } else {
        0f
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    POC_drag_and_dropTheme {
        DragDropDemo(Modifier)
    }
}