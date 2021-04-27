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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
            .background(color = if (receiving) Color.Green else Color.Blue)
        ) {}
        Text(targetValue)
      }
    }
  }
}

@Composable
fun DragContainer(
  modifier: Modifier,
  content: @Composable () -> Unit) {
  val isDragging = remember { mutableStateOf(false) }
  val dragPosition = remember { mutableStateOf(Offset.Zero) }
  val dragOffset = remember { mutableStateOf(Offset.Zero) }
  val sourcePosition = remember { mutableStateOf(Offset.Zero) }
  CompositionLocalProvider(
    LocalDragIsDragging provides isDragging,
    LocalSourcePosition provides sourcePosition,
    LocalDragPosition provides dragPosition,
    LocalDragOffset provides dragOffset,
  ) {
    Box(modifier = modifier.background(Color.Yellow)) {
      content()
      if (isDragging.value) {
        var dragSize by remember { mutableStateOf(IntSize.Zero) }
        Box(
          modifier = Modifier
            .offset {
              IntOffset(
                (dragPosition.value.x - sourcePosition.value.x + dragOffset.value.x - (dragSize.width / 2)).roundToInt(),
                (dragPosition.value.y - sourcePosition.value.y + dragOffset.value.y - (dragSize.height / 2)).roundToInt(),
              )
            }
            .zIndex(4f)
            .scale(.5f)
            .border(width = 2.dp, color = Color.Black)
            .alpha(.5f)
            .onGloballyPositioned {
              dragSize = it.size
            }
        ) {
          LocalDraggedComposable.current.value?.invoke()
        }
      }
    }
  }
}

val LocalDragIsDragging = compositionLocalOf { mutableStateOf(false) }
val LocalDragPosition = compositionLocalOf { mutableStateOf(Offset.Zero) }
val LocalSourcePosition = compositionLocalOf { mutableStateOf(Offset.Zero) }
val LocalDragOffset = compositionLocalOf { mutableStateOf(Offset.Zero) }
val LocalDraggedComposable = compositionLocalOf { mutableStateOf<(@Composable () -> Unit)?>( null) }
val LocalDraggedData = compositionLocalOf { mutableStateOf<Any?>(null) }
val LocalDragReceiver = compositionLocalOf { mutableStateOf<((Any) -> Unit)?>(null) }

@Composable
fun <T : Any> Draggable(
  dragDataProducer: () -> T,
  content: @Composable (isBeingDragged: Boolean) -> Unit
) {
  var iAmBeingDragged by remember { mutableStateOf(false) }
  val sourcePositionState = LocalSourcePosition.current
  val dragPositionState = LocalDragPosition.current
  val isDraggingState = LocalDragIsDragging.current
  val dragOffsetState = LocalDragOffset.current
  val draggedComposableState = LocalDraggedComposable.current
  val dragDraggedDataState = LocalDraggedData.current
  val dragDataReceiver = LocalDragReceiver.current
  Box(
    Modifier
      .onGloballyPositioned {
        sourcePositionState.value = it.localToWindow(Offset.Zero)
      }
      .pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
          onDragStart = {
            isDraggingState.value = true
            dragPositionState.value = sourcePositionState.value + it
            draggedComposableState.value = { content(false) }
            dragDraggedDataState.value = dragDataProducer()
            iAmBeingDragged = true
          },
          onDragEnd = {
            dragOffsetState.value = Offset.Zero
            isDraggingState.value = false
            dragDataReceiver.value?.invoke(dragDraggedDataState.value!!)
            iAmBeingDragged = false
          },
          onDrag = { change, dragAmount ->
            change.consumeAllChanges()
            dragOffsetState.value += Offset(dragAmount.x, dragAmount.y)
          }
        )
      }
  ) {
    content(iAmBeingDragged)
  }
}

@Composable
inline fun <reified T : Any> DragReceiver(
  noinline onReceive: (T) -> Unit,
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
  var receiving by remember { mutableStateOf(false) }
  val dragPositionState = LocalDragPosition.current
  val dragOffsetState = LocalDragOffset.current
  val accepts = klazz.java.isInstance(LocalDraggedData.current.value)
  Box(
    modifier = Modifier
      .onGloballyPositioned { layoutCoordinates ->
        receiving = accepts &&
          layoutCoordinates
          .boundsInWindow()
          .contains(dragPositionState.value + dragOffsetState.value)
      }
  ) {
    val isReceiving = receiving
    if (isReceiving) {
      LocalDragReceiver.current.value = { onReceive(klazz.java.cast(it)!!) }
    }
    content(isReceiving)
  }
}