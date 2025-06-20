package me.anno.bench.sdftexture

import me.anno.Engine
import me.anno.bench.sdftexture.ContourOptimizer.optimizeContours
import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.Contour.Companion.calculateContours
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField.computeDistances
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.utils.Clock
import me.anno.utils.Color.black
import me.anno.utils.OS.desktop
import me.anno.utils.structures.lists.Lists.count2
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import kotlin.random.Random

// todo make SDF textures work on the texture-ascii-characters

// optimize SDF texture generation
//  1) make it faster by reducing done work and reducing branch misses -> 3x speedup achieved
//  2) run it on the GPU -> not any faster
fun main() {

    LogManager.disableInfoLogs("Saveable,ExtensionManager,DefaultConfig")
    OfficialExtensions.initForTests()
    val clock = Clock("SDFTextureBench")
    val font = Font("Verdana", 100f)

    val slowestChar = '@'
    val roundEdges = false

    val text = slowestChar.toString()
    val contours0 = calculateContours(font, text)
    drawContour("contour0.png", contours0)

    val maxError = 0.1f
    val contours1 = optimizeContours(contours0, maxError)
    drawContour("contour1.png", contours1)

    println(formatContours(contours0))
    println(formatContours(contours1))

    var needsVisualCheck = true
    clock.benchmark(1, 2, "SDFChars[${slowestChar}]") {
        val field = computeDistances(contours1, roundEdges)!!
        if (needsVisualCheck) { // visual check
            needsVisualCheck = false
            val image = FloatImage(field.w, field.h, 1, field.getDistances()!!)
            image.flipY()
            image.mul(0.2f).write(desktop.getChild("char.png"))
        }
    }

    Engine.requestShutdown()
}

fun formatContours(contours: List<Contour>): String {
    return contours.map { c ->
        val linear = c.segments.count2 { it is LinearSegment }
        val square = c.segments.count2 { it is QuadraticSegment }
        val lengths = c.segments.map { it.length() }
        "[$linear + $square + ${c.segments.size - linear - square}, ${lengths.min()}-${lengths.max()}, ${lengths.average()}]"
    }.toString()
}

fun drawContour(name: String, contours: List<Contour>) {
    val random = Random(1234)
    val lines = contours.flatMap { it.segments }
        .map { segment ->
            if (segment is LinearSegment) listOf(segment.p0, segment.p1)
            else {
                val n = 2 + (segment.length() * 0.2f).toInt()
                (0..n).map { i ->
                    val t = i.toFloat() / n
                    segment.getPointAt(t, Vector2f())
                }
            }
        }
        .map { pts ->
            (1 until pts.size).map { i ->
                Pair(pts[i - 1], pts[i])
            }
        }
        .flatMap { pts ->
            val color = random.nextInt() or black or 0x333333
            pts.map { (a, b) ->
                ImageWriter.ColoredLine(a, b, color)
            }
        }
    ImageWriter.writeLines(512, name, lines)
}