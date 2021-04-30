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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
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
) : Element {

  override val name: String get() = "BoxItem"
  var text by mutableStateOf(text)

  @Composable
  override fun Properties(modifier: Modifier) {
    Column(modifier = modifier) {
      TextField(modifier = Modifier.fillMaxWidth(), value = text, onValueChange = { text = it })
    }
  }

  @Composable
  override fun Generate(modifier: Modifier) {
    Box(
      modifier = modifier.background(color),
      propagateMinConstraints = true // we stretch content if we are stretched
    ) {
      Text(text, fontSize = 20.sp, color = textColor)
    }
  }

  @ExperimentalUnsignedTypes
  override fun printTo(modifier: String, output: CodeOutput) {
    output.println("""
    Box(
      modifier = $modifier.background(${color.toCodeString()}}),
      propagateMinConstraints = true
    ) {
      Text(\"$text\", fontSize = 20.sp, color = ${color.toCodeString()})
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
class ButtonItem(initialText: String) : Element {

  var text by mutableStateOf(initialText)

  override val name get() = "Button"

  @Composable
  override fun Generate(modifier: Modifier, onClickHelper: () -> Unit) {
    Button(modifier = modifier, onClick = onClickHelper) {
      Text(text = text)
    }
  }

  override fun printTo(modifier: String, output: CodeOutput) {
    output.println("""
      Button(modifier = $modifier) {
        Text(text = \"$text\")
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
 * Base class for both [Vertical] and [Horizontal] [elements][Element].
 * The code to generate [Column] and [Row] is almost identical except for a few little pieces
 * that are solved through abstracts methods implemented on each concrete class.
 */
abstract class Linear<ContainerScopeT> : Element {
  var elements by mutableStateOf(listOf<Element>())
  var extendFrom by mutableStateOf(0)
  var extendTo by mutableStateOf(0)

  abstract fun Modifier.myDirectionMinSize(): Modifier
  abstract fun Modifier.fillOtherDirection(): Modifier
  abstract fun Modifier.weight1(scope: ContainerScopeT): Modifier
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

  @Composable
  abstract fun CreateContainer(modifier: Modifier, content: @Composable ContainerScopeT.() -> Unit)

  @Composable
  override fun Generate(modifier: Modifier) {
    CreateContainer(modifier) {
      Generate(modifier = Modifier.fillOtherDirection())
    }
  }

  @Composable
  fun ContainerScopeT.Generate(modifier: Modifier) {

    @Composable
    fun placeHolder(
      text: String,
      index: Int,
      extended: Boolean,
      incrementExtendFrom: Boolean,
      incrementExtendTo: Boolean
    ) {
      val childModifier = modifier
        .takeIf(extended) { weight1(this@Generate) }
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
        .takeIf(extended) { weight1(this@Generate) }
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

/**
 * [Element] that produces a [Column].
 * See the [base class][Linear] for details on how its implemented.
 * [Vertical] only provides the specifics for the vertical case.
 */
class Vertical : Linear<ColumnScope>() {

  override val name get() = "Vertical"

  @Composable
  override fun CreateContainer(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, content = content)
  }

  override fun Modifier.myDirectionMinSize() = height(Min)
  override fun Modifier.fillOtherDirection() = fillMaxWidth()
  override fun Modifier.weight1(scope: ColumnScope) = with(scope) { weight(1f) }
  override fun codeForContainer() = "Column"
  override fun codeForFillOtherDirection() = "fillMaxWidth()"
  override fun codeForMyDirectionMinSize() = "height(Min)"
}

/**
 * [Element] that produces a [Row].
 * See the [base class][Linear] for details on how its implemented.
 * [Horizontal] only provides the specifics for the vertical case.
 */
class Horizontal: Linear<RowScope>() {

  override val name get() = "Horizontal"

  @Composable
  override fun CreateContainer(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier = modifier, content = content)
  }

  override fun Modifier.myDirectionMinSize() = width(Min)
  override fun Modifier.fillOtherDirection() = fillMaxHeight()
  override fun Modifier.weight1(scope: RowScope) = with(scope) { weight(1f) }
  override fun codeForContainer() = "Row"
  override fun codeForFillOtherDirection() = "fillMaxHeight()"
  override fun codeForMyDirectionMinSize() = "width(Min)"
}
