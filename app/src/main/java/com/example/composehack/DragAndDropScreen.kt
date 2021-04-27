package com.example.composehack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Sample screen for dragging.
 */
@Preview
@Composable
fun DragAndDropScreen() {
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
