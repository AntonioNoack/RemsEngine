package me.anno.fonts

import org.joml.Vector2f
import java.awt.Font

class StringPart(
    val xPos: Float,
    val yPos: Float,
    val text: String,
    val font: Font,
    var lineWidth: Float,
    val codepointLength: Int = text.codePoints().count().toInt()
) {
    operator fun plus(v: Vector2f) = StringPart(
        xPos + v.x, yPos + v.y, text, font, lineWidth, codepointLength
    )
}