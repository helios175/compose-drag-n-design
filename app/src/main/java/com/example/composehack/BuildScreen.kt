package com.example.composehack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.composehack.DraggableState.NORMAL_DRAGGING

@Composable
fun BuildScreen() {
  var mainElement: Element by remember { mutableStateOf(initialElement) }
  DragContainer(modifier = Modifier.fillMaxSize()) {
    HorizontalSplit(
      factor = 0.2f,
      left = {
        Column {
          elementsMenu.forEach { RenderMenuItem(it) }
        }
      },
      right = {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
        ) {
          mainElement.generate(
            modifier = Modifier,
            onTransform = { mainElement = it }
          )
        }
      }
    )
  }
}

@Composable
fun HorizontalSplit(
  factor: Float,
  left: @Composable () -> Unit,
  right: @Composable () -> Unit
) {
  Row(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier
      .fillMaxHeight()
      .weight(factor)) { left() }
    Box(modifier = Modifier
      .fillMaxHeight()
      .weight(1f - factor)) { right() }
  }
}

val initialElement = Vertical(
  elements = listOf(
    BoxItem("Item 1", Color.Red),
    BoxItem("item 2", Color.Green),
    BoxItem("Item 3", Color.Blue, textColor = Color.White),
  ),
  extendFrom = 1,
  extendTo = 1
)

private val elementsMenu = listOf<MenuItem>(
  MenuItem("Horizontal", Color.LightGray) { Horizontal(listOf(), 0, 0) },
  MenuItem("Vertical", Color.LightGray) { Vertical(listOf(), 0, 0) },
  MenuItem("Magenta", Color.Magenta) { BoxItem("Magenta", Color.Magenta) },
  MenuItem("Yellow", Color.Yellow) { BoxItem("Magenta", Color.Yellow) },
  MenuItem("Green", Color.Magenta) { BoxItem("Green", Color.Green) }
)

private class MenuItem(
  val text: String,
  val color: Color,
  val elementProducer: () -> Element
)

@Composable
private fun RenderMenuItem(menuItem: MenuItem) {
  Draggable(
    modifier = Modifier.fillMaxWidth(),
    dragDataProducer = menuItem.elementProducer) { state ->
    Box(modifier = Modifier
      .background(if (state == NORMAL_DRAGGING) Color.Gray else menuItem.color)
    ) {
      Text(text = menuItem.text)
    }
  }
}
