package com.example.composehack

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Convenience method to be able to write something like:
 *
 *     Modifier
 *       .background(...)
 *       .takeIf(a < b) {
 *          // part of the chain applied only if a < b
 *          border(...)
 *          .etc(...)
 *          .etc(...)
 *       }
 *       .alpha(...)
 */
inline fun Modifier.takeIf(condition: Boolean, block: Modifier.() -> Modifier): Modifier =
  if (condition) block() else this

fun Offset.toIntOffset() = IntOffset(x.roundToInt(), y.roundToInt())

operator fun IntOffset.minus(size: IntSize) = IntOffset(x - size.width, y - size.height)
