package com.example.composehack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp

/**
 * Sample [Element] for a box with color and text.
 */
class BoxItem(
  text: String,
  val color: Color,
  val textColor: Color = Color.Black
) : Element<BoxItem> {

  override val name: String get() = "BoxItem"
  var text by mutableStateOf(text)

  @Composable
  override fun Properties(modifier: Modifier) {
    Column(modifier = modifier) {
      TextField(modifier = Modifier.fillMaxWidth(), value = text, onValueChange = { text = it })
    }
  }

  override val Generate: GenerateFunction<BoxItem>
    get() = {
        modifier: Modifier,
        element: BoxItem,
        _: () -> Unit,
        _: (Element<*>) -> Unit -> GenerateBoxItem(modifier, element)
    }

  @ExperimentalUnsignedTypes
  override fun printTo(modifier: String, output: CodeOutput) {
    output.println("""
    Box(
      modifier = $modifier.background(${color.toCodeString()}}),
      propagateMinConstraints = true
    ) {
      Text("$text", fontSize = 20.sp, color = ${color.toCodeString()})
    }
    """.trimIndent()
    )
  }
}

@Composable
private fun GenerateBoxItem(
  modifier: Modifier,
  element: BoxItem,
) {
  Box(
    modifier = modifier.background(element.color),
    propagateMinConstraints = true // we stretch content if we are stretched
  ) {
    Text(element.text, fontSize = 20.sp, color = element.textColor)
  }
}

@ExperimentalUnsignedTypes
private fun Color.toCodeString() = "Color(0x${toArgb().toUInt().toString(16)})"

/**
 * [Element] that produces a Material [Button].
 */
class ButtonItem(initialText: String) : Element<ButtonItem> {

  var text by mutableStateOf(initialText)

  override val name get() = "Button"

  override val Generate: GenerateFunction<ButtonItem>
    get() = { modifier: Modifier,
      element: ButtonItem,
      onClickHelper: () -> Unit,
      _: (Element<*>) -> Unit ->
      Button(modifier = modifier, onClick = onClickHelper) {
        Text(text = element.text)
      }
    }

  override fun printTo(modifier: String, output: CodeOutput) {
    output.println("""
      Button(modifier = $modifier) {
        Text(text = "$text")
      }
      """.trimIndent()
    )
  }

  @Composable
  override fun Properties(modifier: Modifier) {
    Column(modifier = modifier) {
      TextField(modifier = Modifier.fillMaxWidth(), value = text, onValueChange = { text = it })
    }
  }
}

/**
 * [Element] that produces a Material [TextField].
 */
class TextFieldItem(initialText: String) : Element<TextFieldItem> {

  var text: String by mutableStateOf(initialText)

  override val name get() = "TextField"

  @Composable
  override fun Properties(modifier: Modifier) {
    Column(modifier = modifier) {
      TextField(modifier = Modifier.fillMaxWidth(), value = text, onValueChange = { text = it })
    }
  }

  override val Generate: GenerateFunction<TextFieldItem>
    get() = {
        modifier: Modifier,
      element: TextFieldItem,
      onClickHelper: () -> Unit,
      _: (Element<*>) -> Unit ->
      TextField(modifier = modifier
        // Use the "onFocus" as a proxy for "I'm clicked"
        .onFocusChanged { if (it.isFocused) onClickHelper() },
        value = element.text,
        onValueChange = { element.text = it },
      )
    }

  override fun printTo(modifier: String, output: CodeOutput) {
    output.println("""
      TextField(modifier = $modifier, value = "$text", onValueChange = {...})
    """.trimIndent())
  }
}

/**
 * Base class for both [Vertical] and [Horizontal] [elements][Element].
 * The code to generate [Column] and [Row] is almost identical except for a few little pieces
 * that are solved through abstracts methods implemented on each concrete class.
 */
abstract class Linear<T : Linear<T>> : Element<T> {
  abstract val elements: List<Element<*>>
  abstract val extendFrom: Int
  abstract val extendTo: Int

  abstract fun copyMySelf(elements: List<Element<*>>, extendFrom: Int, extendTo: Int): T

  abstract fun codeForContainer(): String
  abstract fun codeForFillOtherDirection(): String
  abstract fun codeForMyDirectionMinSize(): String

  override fun printTo(modifier: String, output: CodeOutput) {
    val container = codeForContainer()
    val fillOtherDirection = codeForFillOtherDirection()
    val myDirectionMinSize = codeForMyDirectionMinSize()
    val weight1 = "weight(1f)"
    with (output) {
      println("$container(Modifier.$fillOtherDirection) {")
      indent {
        elements.forEachIndexed { index, element ->
          val extended = index in extendFrom until extendTo
          val childModifier = "Modifier.${if (extended) weight1 else myDirectionMinSize}"
          element.printTo(childModifier, this)
        }
      }
      println("}")
    }
  }
}

@Composable
fun <T: Linear<T>, ContainerScopeT> ContainerScopeT.GenerateLinearContent(
  modifier: Modifier,
  element: T,
  //onClickHelper: () -> Unit,
  onTransform: (Element<*>) -> Unit,
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
    val childModifier = Modifier.fillOtherDirection()
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
    val childModifier = Modifier.fillOtherDirection()
      .takeIf(extended) { weight1() }
      .takeIf(!extended) { myDirectionMinSize() }
    val top = index < element.extendFrom
    val center = index in element.extendFrom until element.extendTo
    element.elements[index].PlacedElement(childModifier, onRemove = {
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

/**
 * [Element] that produces a [Column].
 * See the [base class][Linear] for details on how its implemented.
 * [Vertical] only provides the specifics for the vertical case.
 */
class Vertical(
  override val elements: List<Element<*>>,
  override val extendFrom: Int,
  override val extendTo: Int
) : Linear<Vertical>() {

  override val name get() = "Vertical"

  override fun copyMySelf(elements: List<Element<*>>, extendFrom: Int, extendTo: Int): Vertical = Vertical(elements, extendFrom, extendTo)

  override val Generate: GenerateFunction<Vertical>
    get() = {
        modifier: Modifier,
        element: Vertical,
        _: () -> Unit,
        onTransform: (Element<*>) -> Unit ->
      Column(modifier) {
        val scope = this
      GenerateLinearContent<Vertical, ColumnScope>(
        modifier = modifier,
        element = element,
        onTransform= onTransform,
        weight1 = { weight(1f) },
        fillOtherDirection = { fillMaxWidth() },
        myDirectionMinSize = { height(Min) }
      )
    }
  }

  override fun codeForContainer() = "Column"
  override fun codeForFillOtherDirection() = "fillMaxWidth()"
  override fun codeForMyDirectionMinSize() = "height(Min)"
}

/**
 * [Element] that produces a [Row].
 * See the [base class][Linear] for details on how its implemented.
 * [Horizontal] only provides the specifics for the vertical case.
 */
data class Horizontal(
  override val elements: List<Element<*>>,
  override val extendFrom: Int,
  override val extendTo: Int
): Linear<Horizontal>() {

  override val name get() = "Horizontal"

  override fun copyMySelf(elements: List<Element<*>>, extendFrom: Int, extendTo: Int) = Horizontal(elements, extendFrom = extendFrom, extendTo = extendTo)

  override val Generate: GenerateFunction<Horizontal>
    get() = {
        modifier: Modifier,
        element: Horizontal,
        _: () -> Unit,
        onTransform: (Element<*>) -> Unit ->
      Row(modifier) {
        GenerateLinearContent<Horizontal, RowScope>(
          modifier = modifier,
          element = element,
          onTransform= onTransform,
          weight1 = { weight(1f) },
          fillOtherDirection = { fillMaxHeight() },
          myDirectionMinSize = { width(Min) }
        )
      }
    }

  override fun codeForContainer() = "Row"
  override fun codeForFillOtherDirection() = "fillMaxHeight()"
  override fun codeForMyDirectionMinSize() = "width(Min)"
}
