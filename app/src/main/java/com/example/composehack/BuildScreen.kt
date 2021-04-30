package com.example.composehack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.composehack.DraggableState.NORMAL_DRAGGING

@Composable
fun emptyPropertiesPanel() {
  Text("<none selected>")
}

val LocalSelectionInfo = compositionLocalOf<SelectionInfo> { error("LocalSelectionInfo not provided") }

class SelectionInfo {
  var selectedElement: Element? by mutableStateOf(null)
  var onRemove: () -> Unit by mutableStateOf({})
  var showHelpers: Boolean by mutableStateOf(true)
}

@Composable
fun BuildScreen() {
  var mainElement: Element by remember { mutableStateOf(initialElement) }
  var selectionInfo = remember { SelectionInfo() }
  CompositionLocalProvider(LocalSelectionInfo provides selectionInfo) {
    DragContainer(modifier = Modifier.fillMaxSize()) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          Button(
            onClick = {
              CodeOutput().apply {
                mainElement.printTo("Modifier", this)
              }
                .toString()
                .split('\n')
                .forEach(::println)
            }
          ) {
            Text("Print")
          }
          Button(
            onClick = {
              selectionInfo.showHelpers = !selectionInfo.showHelpers
              selectionInfo.selectedElement = null
            }
          ) {
            Text("Helpers ${if (selectionInfo.showHelpers) "ON" else "OFF"}")
          }
        }
        selectionInfo.selectedElement?.let { element ->
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(modifier = Modifier.weight(1f), text = "[${element.name}] properties:")
            Button(onClick = selectionInfo.onRemove) {
              Text("X")
            }
          }
          element.properties(modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray))
        }
        HorizontalSplit(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
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
              mainElement.generate(modifier = Modifier)
            }
          }
        )
      }
    }
  }
}

@Composable
fun HorizontalSplit(
  modifier: Modifier,
  factor: Float,
  left: @Composable () -> Unit,
  right: @Composable () -> Unit
) {
  Row(modifier = modifier) {
    Box(modifier = Modifier
      .fillMaxHeight()
      .weight(factor)) { left() }
    Box(modifier = Modifier
      .fillMaxHeight()
      .weight(1f - factor)) { right() }
  }
}

val initialElement = Vertical()
//  elements = listOf(
//    BoxItem("Item 1", Color.Red),
//    BoxItem("item 2", Color.Green),
//    BoxItem("Item 3", Color.Blue, textColor = Color.White),
//  ),
//  extendFrom = 1,
//  extendTo = 1
//)

private val elementsMenu = listOf(
  MenuItem("Horizontal", Color.LightGray) { Horizontal() },
  MenuItem("Vertical", Color.LightGray) { Vertical() },
  MenuItem("Magenta", Color.Magenta) { BoxItem("Magenta", Color.Magenta) },
  MenuItem("Yellow", Color.Yellow) { BoxItem("Yellow", Color.Yellow) },
  MenuItem("Green", Color.Green) { BoxItem("Green", Color.Green) }
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
