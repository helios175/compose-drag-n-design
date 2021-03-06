package com.example.composehack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.composehack.DraggableState.NORMAL_DRAGGING

/**
 * Provides the [SelectionInfo] instance for the [BuildScreen] composable.
 * @see PlacedElement
 */
val LocalSelectionInfo =
  compositionLocalOf<SelectionInfo> { error("LocalSelectionInfo not provided") }

/**
 * State for the [BuildScreen].
 */
class SelectionInfo {
  /**
   * What [Element] is selected or null.
   */
  var selectedElement: Element? by mutableStateOf(null)

  /**
   * Lambda to call if the [selectedElement] needs to be removed.
   */
  var onRemove: () -> Unit by mutableStateOf({})

  var onTransform: (Element) -> Unit by mutableStateOf({})

  /**
   * @return `true` if we should show helpers (placeholders, clickable elements and properties).
   * `false` if the design space should look as-in-production.
   *
   * @see PlaceHolder
   * @see PlacedElement
   */
  var showHelpers: Boolean by mutableStateOf(true)
}

/**
 * Main screen for the designer.
 * It contains:
 * ```
 * [ properties for selected element ] [ helpers On/Off ] [ Remove selected ]
 * [ Components menu] [                   design space                      ]
 * ```
 */
@Preview
@Composable
fun BuildScreen() {
  var mainElement: Element by remember { mutableStateOf(initialElement) }
  val selectionInfo = remember { SelectionInfo() }
  CompositionLocalProvider(LocalSelectionInfo provides selectionInfo) {
    DragContainer(modifier = Modifier.fillMaxSize()) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          Button(
            onClick = { print(mainElement) }
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
            Button(onClick = {
              // onRemove will be updated when the UI is recomposed.
              // Only when the X button is pressed go fetch it.
              selectionInfo.onRemove()
              selectionInfo.selectedElement = null
            }) {
              Text("X")
            }
          }
          RenderProperties(element, selectionInfo)
        }
        HorizontalSplit(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
          factor = 0.2f,
          left = {
            Column {
              elementsMenu.forEach { RenderMenuEntry(it) }
            }
          },
          right = {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(20.dp),
              propagateMinConstraints = true
            ) {
              PlacedElement(
                modifier = Modifier, mainElement, onRemove = {}, onTransform = { mainElement = it }
              )
            }
          }
        )
      }
    }
  }
}

@Composable
fun <T : Element> RenderProperties(element: T, selectionInfo: SelectionInfo) {
  val renderer = Renderers.rendererFor(element)
  renderer.Properties(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.LightGray),
    element = element,
    onTransform = {
      // onTransform will be updated when the UI is recomposed.
      // Only when the properties transformation occur (after properties UI is rendered)
      // go fetch it.
      selectionInfo.onTransform(it)
    }
  )
}

/**
 * Main horizontal split:
 * `[menu | design]`
 */
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

val initialElement = Vertical(listOf(), 0, 0)

/**
 * The list of elements available to drag and drop.
 */
private val elementsMenu = listOf(
  MenuEntry("Horizontal", Color.LightGray) { Horizontal(listOf(), 0, 0) },
  MenuEntry("Vertical", Color.LightGray) { Vertical(listOf(), 0, 0) },
  MenuEntry("Magenta", Color.Magenta) { BoxItem("Magenta", Color.Magenta) },
  MenuEntry("Yellow", Color.Yellow) { BoxItem("Yellow", Color.Yellow) },
  MenuEntry("Green", Color.Green) { BoxItem("Green", Color.Green) },
  MenuEntry("Button", Color.Cyan) { ButtonItem("Button") },
  MenuEntry("TextField", Color.LightGray) { TextFieldItem("Text") }
)

/**
 * Entry in the components available for design.
 *
 * @param text the name to show.
 * @param color the background color.
 * @param elementProducer a factory lambda to produce the [Element] when it's dropped into the
 * design.
 */
private class MenuEntry(
  val text: String,
  val color: Color,
  val elementProducer: () -> Element
)

@Composable
private fun RenderMenuEntry(menuEntry: MenuEntry) {
  Draggable(
    modifier = Modifier.fillMaxWidth(),
    dragDataProducer = menuEntry.elementProducer) { state ->
    Box(modifier = Modifier
      .background(if (state == NORMAL_DRAGGING) Color.Gray else menuEntry.color)
    ) {
      Text(text = menuEntry.text)
    }
  }
}

/**
 * Base interface for all the components definitions that can be dragged into the designer.
 */
interface Element {

  /** Name to be displayed in the properties box. */
  val name: String
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
  CompositionLocalProvider(LocalSelectionInfo provides SelectionInfo()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.White),
      contentAlignment = Alignment.Center
    ) {
      PlaceHolder(modifier = Modifier, "top") {}
    }
  }
}

/**
 * All container [Element]s should place their children using this method.
 * This method adds the decorations (selected border and show properties on click) when
 * helpers are on.
 *
 * @see BuildScreen
 */
@Composable
fun PlacedElement(modifier: Modifier, element: Element, onRemove: () -> Unit, onTransform: (Element) -> Unit) {
  val selectionInfo = LocalSelectionInfo.current
  val onClickHelper: () -> Unit
  val showHelpers = selectionInfo.showHelpers
  val onTransformCheckingSelection: (Element) -> Unit = { newElement ->
    if (selectionInfo.selectedElement == element) {
      selectionInfo.selectedElement = newElement
    }
    onTransform(newElement)
  }
  if (element == selectionInfo.selectedElement) {
    // Update the selected element actions for when they are taken (happens after UI is updated)
    selectionInfo.onTransform = onTransformCheckingSelection
    selectionInfo.onRemove = onRemove
  }
  if (showHelpers) {
    onClickHelper = { selectionInfo.selectedElement = element }
  } else {
    onClickHelper = {}
  }
  // Some elements like the TextField might take issue with adding and removing clickable.
  // So we do it in a wrapper box instead.
  Box(
    modifier = modifier
      .takeIf(showHelpers) { clickable { onClickHelper() } }
      .takeIf(selectionInfo.selectedElement == element) {
        background(Color.Red).padding(3.dp)
      },
    propagateMinConstraints = true
  ) {
    val renderer = Renderers.rendererFor(element)
    renderer.Generate(modifier, element, onClickHelper, onTransformCheckingSelection)
  }
}

/**
 * Prints the code output for the [mainElement] tree to the standard output.
 * @see CodeOutput
 */
fun print(mainElement: Element) {
  CodeOutput().apply {
    println("Modifier", mainElement)
  }
    .toString()
    .split('\n')
    .forEach(::println)
}

/**
 * Utility class to print source code. Used in [Renderer.printTo]. Helps with indentation.
 */
class CodeOutput {
  private val sb = StringBuilder()
  private var indent = 0

  /**
   * Runs the [block] with one more indentation than current.
   * Example:
   * ```
   * with(output) {
   *   println("Column {")
   *   indent {
   *     println("Text(...)")
   *     println("Text(...)")
   *   }
   *   println("}"
   * }
   * ```
   */
  fun indent(block: () -> Unit) {
    indent++
    block()
    indent--
  }

  private fun printIndent() {
    repeat(indent) {
      sb.append("  ")
    }
  }

  /**
   * Prints each one of the lines in [text] with the current indentation.
   * @see indent
   */
  fun println(text: String) {
    text.split('\n').forEach {
      printIndent()
      sb.append(it)
      sb.append('\n')
    }
  }

  /**
   * Convenience that knows how to get the proper renderer for an [Element] to print compositions.
   */
  fun println(modifier: String, element: Element) {
    Renderers.rendererFor(element).printTo(modifier, element, this)
  }

  /**
   * @return the generated code so far.
   */
  override fun toString() = sb.toString()
}

object Renderers {
  private val map = mutableMapOf<Class<Element>, Renderer<*>>()

  @Suppress("UNCHECKED_CAST")
  fun <T : Element> rendererFor(element: T): Renderer<T> = map[element.javaClass] as Renderer<T>

  init {
    add(BoxItemRenderer)
    add(ButtonRenderer)
    add(TextFieldRenderer)
    add(HorizontalRenderer)
    add(VerticalRenderer)
  }

  private inline fun <reified T : Element> add(renderer: Renderer<T>) {
    @Suppress("UNCHECKED_CAST")
    map.put(T::class.java as Class<Element>, renderer)
  }
}

interface Renderer<T : Element> {
  @Composable
  fun Generate(
    modifier: Modifier,
    element: T,
    clickHelper: () -> Unit,
    onTransform: (Element) -> Unit
  )

  /** Generates the UI for the properties box. Defaults to nothing. */
  @Composable
  fun Properties(modifier: Modifier, element: T, onTransform: (T) -> Unit) = Unit


  /**
   * Prints the code for this component.
   * @param modifier the modifier value to use after `modifier = `.
   * @param output the [CodeOutput] to print to.
   */
  fun printTo(modifier: String, element: T, output: CodeOutput)
}