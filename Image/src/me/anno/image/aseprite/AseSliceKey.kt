package me.anno.image.aseprite

import org.joml.Vector2i
import org.joml.Vector4i

class AseSliceKey(
    val frame: Int,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val center: Vector4i?, // cx, cy, cw, ch
    val pivot: Vector2i? // px, py
)