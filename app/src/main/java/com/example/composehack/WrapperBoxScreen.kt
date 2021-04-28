package com.example.composehack

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

fun Modifier.myDraggable(
  onDragStatusChange: (Boolean) -> Unit,
  draggableDrawing: @Composable () -> Unit
) = composed {
  var iAmBeingDragged by remember { mutableStateOf(false) }
  var mySourcePosition by remember { mutableStateOf(Offset.Zero) }
  val dragInfo = LocalDragInfo.current
  this.then(
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
            dragInfo.draggableComposable = { draggableDrawing() }
            dragInfo.draggedData = { "nada" }
            iAmBeingDragged = true
            onDragStatusChange(true)
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
            onDragStatusChange(false)
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
  )
}

@Composable
fun WrapperBoxScreen() {
  DragContainer(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
    Column(
      modifier = Modifier
        .fillMaxSize(.9f)
        .background(Color.Yellow)
    ) {
      var isDragging by remember { mutableStateOf(false) }
      Box(
        modifier = Modifier
          .background(Color.Gray)
          .wrapContentWidth()
          .weight(1f)
          .myDraggable(onDragStatusChange = { isDragging = it }) {
            Box(
              modifier = Modifier
                .background(Color.Green)
                .padding(20.dp)
            ) {
              Text("Drag me")
            }
          }
          .padding(20.dp)
      ) {
        Box(
          modifier = Modifier
            .alpha(.4f)
            .background(Color.Red)
            .fillMaxWidth()
            .fillMaxHeight()
        ) {
          Text(if (isDragging) "THANKS!" else "Drag me!!!")
        }
      }
    }
  }
}
