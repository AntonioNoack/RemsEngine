package me.anno.bugs.todo

import me.anno.Time
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.posMod
import me.anno.maths.geometry.MarchingSquares
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.desktop
import me.anno.utils.structures.Iterators.map
import me.anno.utils.structures.Iterators.mapNotNull
import me.anno.utils.structures.Iterators.toList
import me.anno.utils.types.Strings.indexOf2
import me.anno.utils.types.Strings.isNotBlank2
import org.joml.AABBf
import org.joml.Vector2f
import kotlin.random.Random

fun main() {
    reproduceMarchingSquares()
    // visualizeContours()
}

fun reproduceMarchingSquares() {
    val lines = desktop.getChild("floats.txt")
        .readLinesSync(512)
        .map { line ->
            line.split(", ").map {
                it.toFloat()
            }
        }
        .toList()
    // write thingy as image
    val pixels = FloatImage(lines[0].size, lines.size, 1)
    for ((y, line) in lines.withIndex()) {
        for ((x, v) in line.withIndex()) {
            pixels.setValue(x, y, 0, v)
        }
    }
    pixels.normalize01()
    pixels.write(desktop.getChild("float.png"))

    // test contour calculation
    val contours = MarchingSquares.march(
        pixels.width, pixels.height, pixels.data, 0.5f,
        AABBf(0f, 0f, 0f, (pixels.width - 1).toFloat(), (pixels.height - 1).toFloat(), 0f)
    )
    println("contours: $contours")
    visualizeContours(contours, "contour0.png")
}

fun visualizeContours() {
    val lines = desktop.getChild("contour.txt")
        .readLinesSync(512)
        .mapNotNull { it.split("Contour:").getOrNull(1) }
        .map { line ->
            line.split(' ').filter { it.isNotBlank2() }.map {
                val i0 = it.indexOf2('(')
                val i1 = it.indexOf2(',')
                val i2 = it.indexOf2(')')
                val x = it.substring(i0 + 1, i1).toFloat()
                val y = it.substring(i1 + 1, i2).toFloat()
                Vector2f(x, y)
            }
        }
        .toList()
    visualizeContours(lines, "contour.png")
}


fun visualizeContours(lines: List<List<Vector2f>>, name: String) {
    // draw these lines onto a texture
    val random = Random(name.hashCode().toLong())
    ImageWriter.writeLines(512, name,
        lines.flatMap { strip ->
            val color = random.nextInt() or 0x777777
            val arrowI = strip.indices.maxBy {
                strip[it].distanceSquared(strip[posMod(it + 1, strip.size)])
            }
            strip.indices.map {
                ImageWriter.ColoredLine(
                    strip[it], strip[posMod(it + 1, strip.size)],
                    color.withAlpha(160), color.withAlpha(200)
                )
                // add a small arrow to show the direction
            } + arrow(strip[arrowI], strip[posMod(arrowI + 1, strip.size)], color)
        })
}

fun arrow(from: Vector2f, to: Vector2f, color: Int): List<ImageWriter.ColoredLine> {
    return listOf(-1, 1).map {
        val s = 0.3f * it
        val dx = (from.y - to.y) * s
        val dy = (to.x - from.x) * s
        ImageWriter.ColoredLine(
            to.lerp(from, 0.4f, Vector2f()).add(dx, dy),
            to, color.withAlpha(255), 0
        )
    }
}