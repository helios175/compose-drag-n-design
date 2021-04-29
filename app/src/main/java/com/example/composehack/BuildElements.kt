package com.example.composehack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

interface Element {

  val name: String

  @Composable
  fun properties(modifier: Modifier) = Unit

  @Composable
  fun generate(modifier: Modifier)
}

@Composable
fun PlaceHolder(
  modifier: Modifier,
  text: String,
  onTransform: (Element) -> Unit
) {
  if (!LocalSelectionInfo.current.showHelpers) return

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

@Preview
@Composable
fun PreviewPlaceHolder() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White),
    contentAlignment = Alignment.Center
  ) {
    PlaceHolder(modifier = Modifier, "top") {}
  }
}

abstract class Linear<ContainerScopeT> : Element {
  var elements by mutableStateOf(listOf<Element>())
  var extendFrom by mutableStateOf(0)
  var extendTo by mutableStateOf(0)

  abstract fun Modifier.myDirectionMinSize(): Modifier
  abstract fun Modifier.fillOtherDirection(): Modifier
  abstract fun Modifier.weight1(scope: ContainerScopeT): Modifier
  @Composable
  abstract fun createContainer(modifier: Modifier, content: @Composable ContainerScopeT.() -> Unit)

  @Composable
  override fun generate(modifier: Modifier) {
    createContainer(modifier) {
      generate(modifier = Modifier.fillOtherDirection())
    }
  }

  @Composable
  fun ContainerScopeT.generate(modifier: Modifier) {

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
      PlaceHolder(modifier = childModifier, text = text) { newElement ->
        elements = elements.toMutableList().apply { add(index, newElement) }
        if (incrementExtendFrom) { extendFrom++ }
        if (incrementExtendTo) { extendTo++ }
      }
    }

    @Composable
    fun placeElement(index: Int, extended: Boolean) {
      val childModifier = modifier
        .takeIf(extended) { weight1(this@generate) }
        .takeIf(!extended) { myDirectionMinSize() }
      val top = index < extendFrom
      val center = index in extendFrom until extendTo
      PlacedElement(childModifier, elements[index], onRemove = {
        elements = elements.toMutableList().apply { removeAt(index) }
        if (top) extendFrom--
        if (top || center) extendTo--
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

class Vertical : Linear<ColumnScope>() {

  override val name get() = "Vertical"

  @Composable
  override fun createContainer(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, content = content)
  }

  override fun Modifier.myDirectionMinSize() = height(Min)
  override fun Modifier.fillOtherDirection() = fillMaxWidth()
  override fun Modifier.weight1(scope: ColumnScope) = with(scope) { weight(1f) }
}

class Horizontal: Linear<RowScope>() {

  override val name get() = "Horizontal"

  @Composable
  override fun createContainer(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier, content = content)
  }

  override fun Modifier.myDirectionMinSize() = width(Min)
  override fun Modifier.fillOtherDirection() = fillMaxHeight()
  override fun Modifier.weight1(scope: RowScope) = with(scope) { weight(1f) }
}

class BoxItem(
  text: String,
  val color: Color,
  val textColor: Color = Color.Black
) : Element {

  override val name: String get() = "BoxItem"
  var text by mutableStateOf(text)

  @Composable
  override fun properties(modifier: Modifier) {
    Column(modifier = modifier) {
      TextField(modifier = Modifier.fillMaxWidth(), value = text, onValueChange = { text = it })
    }
  }

  @Composable
  override fun generate(modifier: Modifier) {
    Box(
      modifier = modifier.background(color),
      propagateMinConstraints = true // we stretch content if we are stretched
    ) {
      Text(text, fontSize = 20.sp, color = textColor)
    }
  }
}

@Composable
fun PlacedElement(modifier: Modifier, element: Element, onRemove: () -> Unit) {
  val selectionInfo = LocalSelectionInfo.current
  element.generate(
    modifier = modifier
      .takeIf(selectionInfo.showHelpers) {
        clickable {
          selectionInfo.selectedElement = element
          selectionInfo.onRemove = onRemove
        }
      }
      .takeIf(selectionInfo.selectedElement == element) {
        background(Color.Red).padding(3.dp)
      }
    )
}
