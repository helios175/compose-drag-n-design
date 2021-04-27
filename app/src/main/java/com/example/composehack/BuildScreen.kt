package com.example.composehack

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BuildScreen() {
  var vertical: Element by remember { mutableStateOf(sampleVertical) }
  DragContainer(modifier = Modifier.fillMaxSize()) {
    HorizontalSplit(
      factor = 0.2f,
      left = {
        Column {
          Draggable<Element>(dragDataProducer = {
            BoxItem("Magenta", Color.Magenta)
          }) { dragging ->
            Box(modifier = Modifier.background(if (dragging) Color.Gray else Color.Magenta)) {
              Text(text = "Magenta")
            }
          }
          Draggable<Element>(dragDataProducer = {
            BoxItem("Yellow", Color.Yellow)
          }) { dragging ->
            Box(modifier = Modifier.background(if (dragging) Color.Gray else Color.Yellow)) {
              Text(text = "Yellow")
            }
          }
          Draggable<Element>(dragDataProducer = {
            BoxItem("Green", Color.Green)
          }) { dragging ->
            Box(modifier = Modifier.background(if (dragging) Color.Gray else Color.Green)) {
              Text(text = "Green")
            }
          }
        }
      },
      right = {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Cyan)
            .padding(20.dp)
        ) {
          vertical.generate(
            modifier = Modifier
              .background(Color.Yellow),
            onTransform = { vertical = it }
          )
        }
      }
    )
  }
}

interface Element {
  @Composable
  fun generate(modifier: Modifier, onTransform: (Element) -> Unit)
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

typealias ElementDefinition = @Composable (modifier: Modifier) -> Unit

class PlaceHolder(
  val text: String
) : Element {
  @Composable
  override fun generate(modifier: Modifier, onTransform: (Element) -> Unit) {
    DragReceiver(onReceive = onTransform) { receiving ->
      Box(modifier = Modifier
        .background(if (receiving) Color.Green else lerp(Color.Black, Color.Transparent, 0.8f))
      ) {
        Text(text)
      }
    }
  }
}

data class Vertical(
  val fillWidth: Boolean,
  val fillHeight: Boolean,
  val elements: List<Element>,
  val extendFrom: Int,
  val extendTo: Int
) : Element {
  @Composable
  override fun generate(modifier: Modifier, onTransform: (Element) -> Unit) {
    Column(modifier = Modifier
      .takeIf(fillWidth) { fillMaxWidth() }
      .takeIf(fillHeight) { fillMaxHeight() }
      .then(modifier)
    ) {
      generate(modifier = Modifier.fillMaxWidth(), onTransform = onTransform)
    }
  }
  
  @Composable
  fun ColumnScope.generate(modifier: Modifier, onTransform: (Element) -> Unit) {

    @Composable
    fun placeHolder(
      text: String,
      index: Int,
      extended: Boolean,
      incrementExtendFrom: Boolean,
      incrementExtendTo: Boolean
    ) {
      val childModifier = modifier
        .takeIf(extended) { weight(1f) }
        .takeIf(!extended) { height(IntrinsicSize.Min) }
      PlaceHolder(text).generate(modifier = childModifier) { newElement ->
        val newElements = elements.toMutableList().apply { add(index, newElement) }
        onTransform(
          this@Vertical.copy(
            elements = newElements,
            extendFrom = if (incrementExtendFrom) extendFrom + 1 else extendFrom,
            extendTo = if (incrementExtendTo) extendTo + 1 else extendTo,
          )
        )
      }
    }

    @Composable
    fun placeElement(index: Int, extended: Boolean) {
      val childModifier = modifier
        .takeIf(extended) { weight(1f) }
        .takeIf(!extended) { height(IntrinsicSize.Min) }
      elements[index].generate(modifier = childModifier, onTransform = {})
    }

    // Top
    for (index in 0 until extendFrom) {
      placeHolder("[top]", index = index, incrementExtendFrom = true, incrementExtendTo = true, extended = false)
      placeElement(index = index, extended = false)
    }
    // Center
    if (extendFrom == extendTo) {
      placeHolder("[center]", index = extendFrom, incrementExtendFrom = false, incrementExtendTo = true, extended = true)
    } else {
      placeHolder("[top]", index = extendFrom, incrementExtendFrom = true, incrementExtendTo = true, extended = false)
      placeElement(index = extendFrom, extended = true)
      placeHolder("[bottom]", index = extendTo, incrementExtendFrom = false, incrementExtendTo = false, extended = false)
    }
    // Bottom
    for (index in extendTo until elements.size) {
      placeElement(index = index, extended = false)
      placeHolder("[bottom]", index = index + 1, incrementExtendFrom = false, incrementExtendTo = false, extended = false)
    }
  }
}

val sampleVertical = Vertical(
  fillWidth = true,
  fillHeight = true,
  elements = listOf(
    BoxItem("Item 1", Color.Red),
    BoxItem("item 2", Color.Green),
    BoxItem("Item 3", Color.Blue),
  ),
  extendFrom = 1,
  extendTo = 1
)

class BoxItem(
  val text: String,
  val color: Color
) : Element {

  @Composable
  override fun generate(modifier: Modifier, onTransform: (Element) -> Unit) {
    Box(modifier.background(color)) {
      Text(text, fontSize = 20.sp)
    }
  }
}

