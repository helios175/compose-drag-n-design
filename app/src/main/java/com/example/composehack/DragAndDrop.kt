package com.example.composehack

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.composehack.DraggableState.DRAGGABLE
import com.example.composehack.DraggableState.NORMAL
import com.example.composehack.DraggableState.NORMAL_DRAGGING
import kotlin.reflect.KClass

/**
 * Internal structure maintained by a [DragContainer] and accessible through [LocalDragInfo].
 * [Draggable] and [DragReceiver] will use it and update it.
 */
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

/**
 * Provided by a [DragContainer], this [DragInfo] will contain all state relevant to dragging.
 */
internal val LocalDragInfo = compositionLocalOf { DragInfo() }

/**
 * This container should be used in a screen that allows for dragging. The area covered by this
 * [DragContainer] will be the area where the dragging can happen.
 * This composable also sets up the common state needed for [Draggable] and [DragContainer] to
 * communicated "dragged data".
 *
 * @see Draggable
 * @see DragReceiver
 */
@Composable
fun DragContainer(
  modifier: Modifier,
  content: @Composable () -> Unit) {

  val state = remember { DragInfo() }
  CompositionLocalProvider(
    LocalDragInfo provides state
  ) {
    var myWindowPosition by remember { mutableStateOf(Offset.Zero) }
    Box(
      modifier = modifier
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
            //.scale(.5f)
            .takeIf(dragSize != IntSize.Zero)  {
              border(width = 2.dp, color = Color.Gray)
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

/**
 * Tells which version of a [Draggable] is being generated.
 */
enum class DraggableState {
  /** The draggable element in-place, not being dragged. */
  NORMAL,
  /** The draggable element in-place, when being dragged. */
  NORMAL_DRAGGING,
  /** The draggable element floating. */
  DRAGGABLE
}

/**
 * Generates a dragabble component.
 * There should be placed inside a [DragContainer] which will be the area where things can be
 * dragged.
 *
 * @param modifier the usual modifier for the element. Constraints will be propagated to [content].
 * @param dragDataProducer a lambda producing the data to be communicated to a [DragReceiver].
 * @param content a composable for the draggable element. It will receive a [DraggableState]
 * because this content will be used for the in-place UI and the floating UI as well.
 */
@Composable
fun <T : Any> Draggable(
  modifier: Modifier,
  dragDataProducer: () -> T,
  content: @Composable (state: DraggableState) -> Unit
) {
  var iAmBeingDragged by remember { mutableStateOf(false) }
  var mySourcePosition by remember { mutableStateOf(Offset.Zero) }
  val dragInfo = LocalDragInfo.current
  Box(
    propagateMinConstraints = true, // we stretch content if we are stretched
    modifier = modifier
      .onGloballyPositioned {
        mySourcePosition = it.localToWindow(Offset.Zero)
      }
      .pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
          onDragStart = {
            dragInfo.isDragging = true
            dragInfo.sourcePosition = mySourcePosition
            dragInfo.dragPosition = mySourcePosition + it
            dragInfo.draggableComposable = { content(DRAGGABLE) }
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
    content(if (iAmBeingDragged) NORMAL_DRAGGING else NORMAL)
  }
}

/**
 * Wrapper for a composable that is able to receive a [Draggable] via drag and drop.
 * @param T the type of data that this [DragReceiver] accepts. Other types will be ignored.
 * @param modifier the modifier for the component. Constraints will be propagated to the [content].
 * @param onReceive this method will be called when a draggedData is dropped on us.
 * @param content the composable for the receiver component UI. It receives a boolean indicating
 * if something is being dragged over or not (to for instance, highlight).
 */
@Composable
inline fun <reified T : Any> DragReceiver(
  modifier: Modifier,
  noinline onReceive: (draggedData: T) -> Unit,
  noinline content: @Composable (receiving: Boolean) -> Unit
) {
  DragReceiver(modifier = modifier, onReceive = onReceive, klazz = T::class, content = content)
}

@Composable
@PublishedApi
internal fun <T : Any> DragReceiver(
  modifier: Modifier,
  onReceive: (T) -> Unit,
  klazz: KClass<T>,
  content: @Composable (receiving: Boolean) -> Unit
) {
  var receivingAt by remember { mutableStateOf<Rect?>(null) }
  val dragInfo = LocalDragInfo.current
  val accepts = klazz.java.isInstance(dragInfo.draggedData)
  val dragPosition = dragInfo.dragPosition
  val dragOffsetState = dragInfo.dragOffset
  Box(
    propagateMinConstraints = true, // we stretch content if we are stretched
    modifier = modifier
      .takeIf(accepts) {
        onGloballyPositioned { layoutCoordinates ->
          receivingAt =
            layoutCoordinates
              .boundsInWindow()
              .let { if (it.contains(dragPosition + dragOffsetState)) it else null }
        }
      }
  ) {
    if(receivingAt != null) {
      dragInfo.receivingAt = receivingAt
      dragInfo.draggedDataReceiver = { onReceive(klazz.java.cast(it)!!) }
    }
    content(receivingAt != null)
  }
}