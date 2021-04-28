package com.example.composehack

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.composehack.DraggableState.NORMAL_DRAGGING

@Composable
fun BuildScreen() {
  var mainElement: Element by remember { mutableStateOf(sampleVertical) }
  DragContainer(modifier = Modifier.fillMaxSize()) {
    HorizontalSplit(
      factor = 0.2f,
      left = {
        Column {
          Draggable<Element>(
            modifier = Modifier.fillMaxWidth(),
            dragDataProducer = {
              Horizontal(listOf(), 0, 0)
            }) { state ->
            Box(modifier = Modifier
              .background(if (state == NORMAL_DRAGGING) Color.Gray else Color.LightGray)
            ) {
              Text(text = "Horizontal")
            }
          }
          Draggable<Element>(
            modifier = Modifier.fillMaxWidth(),
            dragDataProducer = {
              Vertical(listOf(), 0, 0)
            }) { state ->
            Box(modifier = Modifier
              .background(if (state == NORMAL_DRAGGING) Color.Gray else Color.LightGray)
            ) {
              Text(text = "Vertical")
            }
          }
          Draggable<Element>(
            modifier = Modifier.fillMaxWidth(),
            dragDataProducer = {
            BoxItem("Magenta", Color.Magenta)
          }) { state ->
            Box(modifier = Modifier
              .background(if (state == NORMAL_DRAGGING) Color.Gray else Color.Magenta)
            ) {
              Text(text = "Magenta")
            }
          }
          Draggable<Element>(
            modifier = Modifier.fillMaxWidth(),
            dragDataProducer = {
            BoxItem("Yellow", Color.Yellow)
          }) { state ->
            Box(modifier = Modifier
              .background(if (state == NORMAL_DRAGGING) Color.Gray else Color.Yellow)
            ) {
              Text(text = "Yellow")
            }
          }
          Draggable<Element>(
            modifier = Modifier.fillMaxWidth(),
            dragDataProducer = {
            BoxItem("Green", Color.Green)
          }) { state ->
            Box(modifier = Modifier
              .background(if (state == NORMAL_DRAGGING) Color.Gray else Color.Green)
            ) {
              Text(text = "Green")
            }
          }
        }
      },
      right = {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
        ) {
          Log.d("TESTING123", "$mainElement")
          mainElement.generate(
            modifier = Modifier,
            onTransform = { mainElement = it }
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

class PlaceHolder(
  val text: String
) : Element {
  @Composable
  override fun generate(modifier: Modifier, onTransform: (Element) -> Unit) {
    DragReceiver(modifier, onReceive = onTransform) { receiving ->
      Box(
        modifier = Modifier
          .takeIf(receiving) { background(Color.Green) }
          .border(2.dp, Color.LightGray)
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(text, color = Color.LightGray)
      }
    }
  }
}

@Preview
@Composable
fun PreviewPlaceHolder() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White),
    contentAlignment = Alignment.Center
  ) {
    PlaceHolder("top").generate(modifier = Modifier) {}
  }
}

abstract class Linear<ContainerScopeT> : Element {
  abstract val elements: List<Element>
  abstract val extendFrom: Int
  abstract val extendTo: Int

  abstract fun copyMySelf(
    elements: List<Element>,
    extendFrom: Int,
    extendTo: Int
  ): Linear<ContainerScopeT>

  abstract fun Modifier.myDirectionMinSize(): Modifier
  abstract fun Modifier.fillOtherDirection(): Modifier
  abstract fun Modifier.weight1(scope: ContainerScopeT): Modifier
  @Composable
  abstract fun createContainer(modifier: Modifier, content: @Composable ContainerScopeT.() -> Unit)

  @Composable
  override fun generate(modifier: Modifier, onTransform: (Element) -> Unit) {
    createContainer(modifier) {
      generate(modifier = Modifier.fillOtherDirection(), onTransform = onTransform)
    }
  }

  @Composable
  fun ContainerScopeT.generate(modifier: Modifier, onTransform: (Element) -> Unit) {

    @Composable
    fun placeHolder(
      text: String,
      index: Int,
      extended: Boolean,
      incrementExtendFrom: Boolean,
      incrementExtendTo: Boolean
    ) {
      val childModifier = modifier
        .takeIf(extended) { weight1(this@generate) }
        .takeIf(!extended) { myDirectionMinSize() }
      PlaceHolder(text)
        .generate(modifier = childModifier) { newElement ->
          val newElements = elements.toMutableList().apply { add(index, newElement) }
          onTransform(copyMySelf(
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
        .takeIf(extended) { weight1(this@generate) }
        .takeIf(!extended) { myDirectionMinSize() }
      elements[index].generate(modifier = childModifier, onTransform = { newElement ->
        val newElements = elements.toMutableList().apply { this[index] = newElement }
        val newMe = copyMySelf(newElements, extendFrom, extendTo)
        /* my */ onTransform(newMe)
      })
    }

    // Top
    for (index in 0 until extendFrom) {
      placeHolder("T", index = index, incrementExtendFrom = true, incrementExtendTo = true, extended = false)
      placeElement(index = index, extended = false)
    }
    // Center
    placeHolder("T", index = extendFrom, incrementExtendFrom = true, incrementExtendTo = true, extended = false)
    if (extendFrom == extendTo) {
      placeHolder("C", index = extendFrom, incrementExtendFrom = false, incrementExtendTo = true, extended = true)
    } else {
      placeElement(index = extendFrom, extended = true)
    }
    placeHolder("B", index = extendTo, incrementExtendFrom = false, incrementExtendTo = false, extended = false)
    // Bottom
    for (index in extendTo until elements.size) {
      placeElement(index = index, extended = false)
      placeHolder("B", index = index + 1, incrementExtendFrom = false, incrementExtendTo = false, extended = false)
    }
  }

}

data class Vertical(
  override val elements: List<Element>,
  override val extendFrom: Int,
  override val extendTo: Int
) : Linear<ColumnScope>() {
  override fun copyMySelf(elements: List<Element>, extendFrom: Int, extendTo: Int) =
    Vertical(elements, extendFrom, extendTo)

  @Composable
  override fun createContainer(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, content = content)
  }

  override fun Modifier.myDirectionMinSize() = height(IntrinsicSize.Min)
  override fun Modifier.fillOtherDirection() = fillMaxWidth()
  override fun Modifier.weight1(scope: ColumnScope) = with(scope) { weight(1f) }
}

data class Horizontal(
  override val elements: List<Element>,
  override val extendFrom: Int,
  override val extendTo: Int
) : Linear<RowScope>() {
  override fun copyMySelf(elements: List<Element>, extendFrom: Int, extendTo: Int) =
    Horizontal(elements, extendFrom, extendTo)

  @Composable
  override fun createContainer(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier, content = content)
  }

  override fun Modifier.myDirectionMinSize() = width(IntrinsicSize.Min)
  override fun Modifier.fillOtherDirection() = fillMaxHeight()
  override fun Modifier.weight1(scope: RowScope) = with(scope) { weight(1f) }
}

val sampleVertical = Vertical(
  elements = listOf(
    BoxItem("Item 1", Color.Red),
    BoxItem("item 2", Color.Green),
    BoxItem("Item 3", Color.Blue, textColor = Color.White),
  ),
  extendFrom = 1,
  extendTo = 1
)

class BoxItem(
  val text: String,
  val color: Color,
  val textColor: Color = Color.Black
) : Element {

  @Composable
  override fun generate(modifier: Modifier, onTransform: (Element) -> Unit) {
    Box(
      modifier = modifier.background(color),
      propagateMinConstraints = true // we stretch content if we are stretched
    ) {
      Text( text, fontSize = 20.sp, color = textColor)
    }
  }
}


