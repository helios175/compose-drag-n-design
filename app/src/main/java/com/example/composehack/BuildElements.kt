package com.example.composehack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp

/**
 * Sample [Element] for a box with color and text.
 */
data class BoxItem(
  val text: String,
  val color: Color,
  val textColor: Color = Color.Black
) : Element {

  override val name: String get() = "BoxItem"
}

object BoxItemRenderer : Renderer<BoxItem> {

  @Composable
  override fun Properties(modifier: Modifier, element: BoxItem, onTransform: (BoxItem) -> Unit) {
    Column(modifier = modifier) {
      TextField(modifier = Modifier.fillMaxWidth(), value = element.text, onValueChange = {
        onTransform(element.copy(text = it))
      })
    }
  }

  @Composable
  override fun Generate(
    modifier: Modifier,
    element: BoxItem,
    clickHelper: () -> Unit,
    onTransform: (Element) -> Unit
  ) {
    Box(
      modifier = modifier.background(element.color),
      propagateMinConstraints = true // we stretch content if we are stretched
    ) {
      Text(element.text, fontSize = 20.sp, color = element.textColor)
    }
  }
  @ExperimentalUnsignedTypes
  override fun printTo(modifier: String, element: BoxItem, output: CodeOutput) {
    output.println("""
    Box(
      modifier = $modifier.background(${element.color.toCodeString()}}),
      propagateMinConstraints = true
    ) {
      Text("${element.text}", fontSize = 20.sp, color = ${element.color.toCodeString()})
    }
    """.trimIndent()
    )
  }
}

@ExperimentalUnsignedTypes
private fun Color.toCodeString() = "Color(0x${toArgb().toUInt().toString(16)})"

/**
 * [Element] that produces a Material [Button].
 */
data class ButtonItem(
  val text: String
  ) : Element {

  override val name get() = "Button"
  }

object ButtonRenderer : Renderer<ButtonItem> {

  @Composable
  override fun Generate(
    modifier: Modifier,
    element: ButtonItem,
    clickHelper: () -> Unit,
    onTransform: (Element) -> Unit
  ) {
    Button(modifier = modifier, onClick = clickHelper) {
      Text(text = element.text)
    }
  }

  @Composable
  override fun Properties(modifier: Modifier, element: ButtonItem, onTransform: (ButtonItem) -> Unit) {
    Column(modifier = modifier) {
      TextField(modifier = Modifier.fillMaxWidth(), value = element.text, onValueChange = {
        onTransform(element.copy(text = it))
      })
    }
  }

  override fun printTo(modifier: String, element: ButtonItem, output: CodeOutput) {
    output.println("""
      Button(modifier = $modifier) {
        Text(text = "${element.text}")
      }
      """.trimIndent()
    )
  }
}

/**
 * [Element] that produces a Material [TextField].
 */
data class TextFieldItem(
  val text: String
  ) : Element {

  override val name get() = "TextField"
  }

object TextFieldRenderer : Renderer<TextFieldItem> {

  @Composable
  override fun Properties(modifier: Modifier, element: TextFieldItem, onTransform: (TextFieldItem) -> Unit) {
    Column(modifier = modifier) {
      TextField(modifier = Modifier.fillMaxWidth(), value = element.text, onValueChange = {
        onTransform(element.copy(text = it))
      })
    }
  }

  @Composable
  override fun Generate(
    modifier: Modifier,
    element: TextFieldItem,
    clickHelper: () -> Unit,
    onTransform: (Element) -> Unit
  ) {
    TextField(modifier = modifier
      // Use the "onFocus" as a proxy for "I'm clicked"
      .onFocusChanged { if (it.isFocused) clickHelper() },
      value = element.text,
      onValueChange = {
        onTransform(element.copy(text = it))
      },
    )
  }

  override fun printTo(modifier: String, element: TextFieldItem, output: CodeOutput) {
    output.println("""
      TextField(modifier = $modifier, value = "${element.text}", onValueChange = {...})
    """.trimIndent())
  }
}

/**
 * Base class for both [Vertical] and [Horizontal] [elements][Element].
 * The code to generate [Column] and [Row] is almost identical except for a few little pieces
 * that are solved through abstracts methods implemented on each concrete class.
 */
abstract class Linear<T : Linear<T>> : Element {
  abstract val elements: List<Element>
  abstract val extendFrom: Int
  abstract val extendTo: Int

  abstract fun copyMySelf(elements: List<Element>, extendFrom: Int, extendTo: Int): T
}

@Composable
fun <T: Linear<T>> GenerateLinearContent(
  element: T,
  onTransform: (Element) -> Unit,
  weight1: Modifier.() -> Modifier,
  fillOtherDirection: Modifier.() -> Modifier,
  myDirectionMinSize: Modifier.() -> Modifier,
) {

  @Composable
  fun placeHolder(
    text: String,
    index: Int,
    extended: Boolean,
    incrementExtendFrom: Boolean,
    incrementExtendTo: Boolean
  ) {
    val childModifier = Modifier
      .fillOtherDirection()
      .takeIf(extended) { weight1() }
      .takeIf(!extended) { myDirectionMinSize() }
    PlaceHolder(modifier = childModifier, text = text) { newElement ->
      onTransform(
        element.copyMySelf(
        elements =element.elements.toMutableList().apply { add(index, newElement) },
          extendFrom = if (incrementExtendFrom) { element.extendFrom+1 } else element.extendFrom,
          extendTo = if (incrementExtendTo) { element.extendTo+1 } else element.extendTo
        )
      )
    }
  }

  @Composable
  fun placeElement(index: Int, extended: Boolean) {
    val childModifier = Modifier
      .fillOtherDirection()
      .takeIf(extended) { weight1() }
      .takeIf(!extended) { myDirectionMinSize() }
    val top = index < element.extendFrom
    val center = index in element.extendFrom until element.extendTo
    PlacedElement(childModifier,
      element = element.elements[index],
      onRemove = {
      onTransform(
        element.copyMySelf(
          elements = element.elements.toMutableList().apply { removeAt(index) },
          extendFrom = if (top) element.extendFrom-1 else element.extendFrom,
          extendTo = if (top || center) element.extendTo-1 else element.extendTo
        )
      )
    }, onTransform = { newElement ->
      onTransform(
        element.copyMySelf(
          elements = element.elements.toMutableList().apply { this[index] = newElement },
          extendFrom = element.extendFrom,
          extendTo = element.extendTo
        )
      )
    })
  }

  // Top
  for (index in 0 until element.extendFrom) {
    placeHolder("T", index = index, incrementExtendFrom = true, incrementExtendTo = true, extended = false)
    placeElement(index = index, extended = false)
  }
  // Center
  placeHolder("T", index = element.extendFrom, incrementExtendFrom = true, incrementExtendTo = true, extended = false)
  if (element.extendFrom == element.extendTo) {
    placeHolder("C", index = element.extendFrom, incrementExtendFrom = false, incrementExtendTo = true, extended = true)
  } else {
    placeElement(index = element.extendFrom, extended = true)
  }
  placeHolder("B", index = element.extendTo, incrementExtendFrom = false, incrementExtendTo = false, extended = false)
  // Bottom
  for (index in element.extendTo until element.elements.size) {
    placeElement(index = index, extended = false)
    placeHolder("B", index = index + 1, incrementExtendFrom = false, incrementExtendTo = false, extended = false)
  }
}

fun linearPrintTo(
  element: Linear<*>,
  codeForContainer: String,
  codeForFillOtherDirection: String,
  codeForMyDirectionMinSize: String,
  output: CodeOutput
) {

  val weight1 = "weight(1f)"
  with (output) {
    println("$codeForContainer(Modifier.$codeForFillOtherDirection) {")
    indent {
      element.elements.forEachIndexed { index, child ->
        val extended = index in element.extendFrom until element.extendTo
        val childModifier = "Modifier.${if (extended) weight1 else codeForMyDirectionMinSize}"
        println(childModifier, child)
      }
    }
    println("}")
  }
}

/**
 * [Element] that produces a [Column].
 * See the [base class][Linear] for details on how its implemented.
 * [Vertical] only provides the specifics for the vertical case.
 */
class Vertical(
  override val elements: List<Element>,
  override val extendFrom: Int,
  override val extendTo: Int
) : Linear<Vertical>() {

  override val name get() = "Vertical"

  override fun copyMySelf(elements: List<Element>, extendFrom: Int, extendTo: Int): Vertical = Vertical(elements, extendFrom, extendTo)
}

object VerticalRenderer : Renderer<Vertical> {

  @Composable
  override fun Generate(
    modifier: Modifier,
    element: Vertical,
    clickHelper: () -> Unit,
    onTransform: (Element) -> Unit
  ) {
    Column(modifier) {
      GenerateLinearContent(
        element = element,
        onTransform = onTransform,
        weight1 = { weight(1f) },
        fillOtherDirection = { fillMaxWidth() },
        myDirectionMinSize = { height(Min) }
      )
    }
  }

  override fun printTo(modifier: String, element: Vertical, output: CodeOutput) {
    linearPrintTo(
      element,
      codeForContainer = "Column",
      codeForFillOtherDirection = "fillMaxWidth()",
      codeForMyDirectionMinSize = "height(Min)",
      output
    )
  }
}

/**
 * [Element] that produces a [Row].
 * See the [base class][Linear] for details on how its implemented.
 * [Horizontal] only provides the specifics for the vertical case.
 */
data class Horizontal(
  override val elements: List<Element>,
  override val extendFrom: Int,
  override val extendTo: Int
): Linear<Horizontal>() {

  override val name get() = "Horizontal"

  override fun copyMySelf(elements: List<Element>, extendFrom: Int, extendTo: Int) = Horizontal(elements, extendFrom = extendFrom, extendTo = extendTo)
}

object HorizontalRenderer : Renderer<Horizontal> {

  @Composable
  override fun Generate(
    modifier: Modifier,
    element: Horizontal,
    clickHelper: () -> Unit,
    onTransform: (Element) -> Unit
  ) {
    Row(modifier) {
      GenerateLinearContent<Horizontal>(
        element = element,
        onTransform= onTransform,
        weight1 = { weight(1f) },
        fillOtherDirection = { fillMaxHeight() },
        myDirectionMinSize = { width(Min) }
      )
    }
  }

  override fun printTo(modifier: String, element: Horizontal, output: CodeOutput) {
    linearPrintTo(
      element,
      codeForContainer = "Row",
      codeForFillOtherDirection = "fillMaxHeight()",
      codeForMyDirectionMinSize = "width(Min)",
      output = output
    )
  }
}