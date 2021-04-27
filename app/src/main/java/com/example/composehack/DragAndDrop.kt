package com.example.composehack

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntSize.Companion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlin.reflect.KClass

@Preview
@Composable
fun HackDragScreen() {
  var targetValue by remember { mutableStateOf("(drop here)") }
  DragContainer(
    modifier = Modifier.fillMaxSize()
  ) {
    Column {
      Box(
        modifier = Modifier
          .size(100.dp)
          .background(color = Color.Gray)
      ) {
        Text("non draggable")
      }
      Draggable(
        dragDataProducer = { "${System.currentTimeMillis()}" }
      ) { dragged ->
        Box(
          modifier = Modifier
            .size(100.dp)
            .background(color = if (dragged) Color.White else Color.Red)
        ) {
          Text("Drag me")
        }
      }
      DragReceiver<String>(
        onReceive = { targetValue = it }
      ) { receiving ->
        Box(
          Modifier
            .size(100.dp)
            .background(color = if (receiving) Color.Green else Color.Cyan)
        ) {
          Text(targetValue)
        }
      }
    }
  }
}

internal class DragInfo {
  var isDragging: Boolean by mutableStateOf(false)
  var dragPosition by mutableStateOf(Offset.Zero)
  var dragOffset by mutableStateOf(Offset.Zero)
  var sourcePosition by mutableStateOf(Offset.Zero)
  var draggableComposable by mutableStateOf<(@Composable () -> Unit)?>(null)
  var draggedData by mutableStateOf<Any?>(null)
  var draggedDataReceiver by mutableStateOf<((Any) -> Unit)?>(null)
  var receivingAt by mutableStateOf<Rect?>(null)
}

internal val LocalDragInfo = compositionLocalOf { DragInfo() }

/**
 * Convenience method to be able to write something like:
 *
 *     Modifier
 *       .background(...)
 *       .takeIf(a < b) {
 *          // part of the chain applied only if a < b
 *          border(...)
 *          .etcetc(...)
 *       }
 *       .alpha(...)
 */
inline fun Modifier.takeIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier =
  if (condition) block() else this

fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())

operator fun IntOffset.minus(size: IntSize) = IntOffset(x - size.width, y - size.height)

@Composable
fun DragContainer(
  modifier: Modifier,
  content: @Composable () -> Unit) {

  val state = remember { DragInfo() }
  CompositionLocalProvider(
    LocalDragInfo provides state
  ) {
    var myWindowPosition by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = modifier
      .background(Color.Yellow)
      .onGloballyPositioned {
        myWindowPosition = it.localToWindow(Offset.Zero)
      }) {
      content()
      if (state.isDragging) {
        var dragSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
          modifier = Modifier
            .offset {
              val offset = (state.dragPosition - myWindowPosition + state.dragOffset)
              offset.toIntOffset() - dragSize / 2
            }
            .zIndex(4f)
            .scale(.5f)
            .takeIf(dragSize != IntSize.Zero)  {
              border(width = 2.dp, color = Color.Black)
            }
            .alpha(if (dragSize == IntSize.Zero) 0f else .5f)
            .onGloballyPositioned {
              dragSize = it.size
            }
        ) {
          state.draggableComposable?.invoke()
        }
      }
    }
  }
}

@Composable
fun <T : Any> Draggable(
  dragDataProducer: () -> T,
  content: @Composable (isBeingDragged: Boolean) -> Unit
) {
  var iAmBeingDragged by remember { mutableStateOf(false) }
  var mySourcePosition by remember { mutableStateOf(Offset.Zero) }
  val dragInfo = LocalDragInfo.current
  Box(
    Modifier
      .onGloballyPositioned {
        mySourcePosition = it.localToWindow(Offset.Zero)
      }
      .pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
          onDragStart = {
            dragInfo.isDragging = true
            dragInfo.sourcePosition = mySourcePosition
            dragInfo.dragPosition = mySourcePosition + it
            dragInfo.draggableComposable = { content(false) }
            dragInfo.draggedData = dragDataProducer()
            iAmBeingDragged = true
          },
          onDragEnd = {
            dragInfo.receivingAt?.let { receivingAt ->
              if (receivingAt.contains(dragInfo.dragPosition + dragInfo.dragOffset)) {
                dragInfo.draggedDataReceiver?.invoke(dragInfo.draggedData!!)
              }
            }
            dragInfo.isDragging = false
            dragInfo.dragOffset = Offset.Zero
            iAmBeingDragged = false
          },
          onDragCancel = {
            dragInfo.dragOffset = Offset.Zero
            dragInfo.isDragging = false
            iAmBeingDragged = false
          },
          onDrag = { change, dragAmount ->
            change.consumeAllChanges()
            dragInfo.dragOffset += Offset(dragAmount.x, dragAmount.y)
          }
        )
      }
  ) {
    content(iAmBeingDragged)
  }
}

@Composable
inline fun <reified T : Any> DragReceiver(
  noinline onReceive: (draggedData: T) -> Unit,
  noinline content: @Composable (receiving: Boolean) -> Unit
) {
  DragReceiver(onReceive = onReceive, klazz = T::class, content = content)
}

@Composable
@PublishedApi
internal fun <T : Any> DragReceiver(
  onReceive: (T) -> Unit,
  klazz: KClass<T>,
  content: @Composable (receiving: Boolean) -> Unit
) {
  var receivingAt by remember { mutableStateOf<Rect?>(null) }
  val dragInfo = LocalDragInfo.current
  val accepts = klazz.java.isInstance(dragInfo.draggedData)
  if (!accepts) {
    // If we don't accept this drag type, don't set up anything (and don't refresh until
    // that data changes
    content(false)
  } else {
    // If we accept that type then yes, check if it changes position
    val dragPosition = dragInfo.dragPosition
    val dragOffsetState = dragInfo.dragOffset
    Box(
      modifier = Modifier
        .onGloballyPositioned { layoutCoordinates ->
          receivingAt =
            layoutCoordinates
            .boundsInWindow()
              .let { if (it.contains(dragPosition + dragOffsetState)) it else null }
        }
    ) {
      if(receivingAt != null) {
        dragInfo.receivingAt = receivingAt
        dragInfo.draggedDataReceiver = { onReceive(klazz.java.cast(it)!!) }
      }
      content(receivingAt != null)
    }
  }
}