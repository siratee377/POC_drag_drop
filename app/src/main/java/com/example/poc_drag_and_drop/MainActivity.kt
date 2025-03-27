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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
                    DragDropDemoMutualDynamicLazy(Modifier.padding(innerPadding), listOf(5,10,2,7))
                }
            }
        }
    }
}

/**
 * Displays a dynamic list of draggable money boxes using LazyColumn.
 *
 * Each box can be dragged onto any other box (when >30% overlap) to transfer its money.
 */
@Composable
fun DragDropDemoMutualDynamicLazy(modifier: Modifier, initialItems: List<Int>) {
    // Convert the input list to a mutable state list so that updates are reflected.
    val amounts = remember { mutableStateListOf(*initialItems.toTypedArray()) }
    // Map to store each box’s global bounds (keyed by index).
    val boxBounds = remember { mutableStateMapOf<Int, Rect>() }
    // Global candidate target index (updated by the currently dragged box).
    var candidateTargetIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        itemsIndexed(amounts) { index, amount ->
            DraggableMoneyBox(
                index = index,
                amount = amount,
                onTransfer = { source, target ->
                    // On drop, add the source box’s money to the target box.
                    amounts[target] = amounts[target] + amounts[source]
                },
                boxBounds = boxBounds,
                globalCandidateTargetIndex = candidateTargetIndex,
                onCandidateTargetChange = { candidateTargetIndex = it }
            )
        }
    }
}

/**
 * A draggable money box composable.
 *
 * Each box tracks its own drag offset, position, and size.
 * During a drag, it computes its current global bounds and checks for >30% overlap
 * with other boxes (using the shared [boxBounds]). If a candidate target is found,
 * it notifies the parent via [onCandidateTargetChange].
 *
 * On drag end, if a candidate target exists, [onTransfer] is called.
 */
@Composable
fun DraggableMoneyBox(
    index: Int,
    amount: Int,
    onTransfer: (sourceIndex: Int, targetIndex: Int) -> Unit,
    boxBounds: MutableMap<Int, Rect>,
    globalCandidateTargetIndex: Int?,
    onCandidateTargetChange: (Int?) -> Unit
) {
    // Local drag state.
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    // Store the box’s global position and size.
    var boxPosition by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(Size.Zero) }

    // Use rememberUpdatedState to ensure the latest candidate target value is used.
    val currentCandidateTarget by rememberUpdatedState(newValue = globalCandidateTargetIndex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                boxPosition = coords.positionInRoot()
                boxSize = coords.size.toSize()
                // Update the shared bounds map.
                boxBounds[index] = Rect(
                    left = boxPosition.x,
                    top = boxPosition.y,
                    right = boxPosition.x + boxSize.width,
                    bottom = boxPosition.y + boxSize.height
                )
            }
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .zIndex(if (isDragging) 1f else 0f)
            .scale(if (isDragging) 0.9f else 1f)
            // Show a blue border if this box is the candidate target (and not being dragged).
            .then(
                if (!isDragging && globalCandidateTargetIndex == index)
                    Modifier.border(2.dp, Color.Blue)
                else Modifier
            )
            .background(Color.Gray)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        // Compute this box's current global rectangle.
                        val currentRect = Rect(
                            left = boxPosition.x + offsetX,
                            top = boxPosition.y + offsetY,
                            right = boxPosition.x + offsetX + boxSize.width,
                            bottom = boxPosition.y + offsetY + boxSize.height
                        )
                        // Look for the best candidate target among all other boxes.
                        var bestCandidate: Int? = null
                        var bestOverlap = 0f
                        val area = boxSize.width * boxSize.height
                        boxBounds.forEach { (otherIndex, otherRect) ->
                            if (otherIndex == index) return@forEach
                            val intersection = currentRect.intersectionArea(otherRect)
                            val overlap = if (area > 0) intersection / area else 0f
                            if (overlap > 0.3f && overlap > bestOverlap) {
                                bestOverlap = overlap
                                bestCandidate = otherIndex
                            }
                        }
                        // Update the candidate target.
                        onCandidateTargetChange(bestCandidate)
                    },
                    onDragEnd = {
                        // Use the latest candidate target value.
                        currentCandidateTarget?.let { target ->
                            onTransfer(index, target)
                        }
                        // Reset drag state.
                        offsetX = 0f
                        offsetY = 0f
                        isDragging = false
                        onCandidateTargetChange(null)
                    },
                    onDragCancel = {
                        offsetX = 0f
                        offsetY = 0f
                        isDragging = false
                        onCandidateTargetChange(null)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$amount", color = Color.White)
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
        DragDropDemoMutualDynamicLazy(Modifier, listOf(5,10,12,7))
    }
}