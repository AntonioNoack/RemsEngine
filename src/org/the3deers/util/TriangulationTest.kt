package org.the3deers.util

import me.anno.image.raw.BIImage
import me.anno.maths.Maths.mix
import me.anno.utils.OS.desktop
import org.joml.Vector2f
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

fun main() {

    val size = 512
    val offset = size / 2f
    val outer = 50

    val random = Random(1234L)
    val points = Array<Vector2f>(outer) {
        val angle = 6.28f * it / outer
        Vector2f(cos(angle), sin(angle))
            .mul(mix(0.5f, 1f, random.nextFloat()))
            .add(1f, 1f).mul(offset)
    }

    val data = FloatArray(points.size * 2)
    for (i in points.indices) {
        val point = points[i]
        data[i * 2] = point.x
        data[i * 2 + 1] = point.y
    }

    val triangles = EarCut.earcut(data, 2)!!

    val bi = BufferedImage(size, size, 1)
    val gi = bi.graphics as Graphics2D
    gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    for (triIndex in 0 until triangles.size / 3) {

        gi.color = Color(0x777777 or random.nextInt() or (255 shl 24))

        val i = triIndex * 3
        fun drawLine(a: Vector2f, b: Vector2f) {
            gi.drawLine(a.x.toInt(), a.y.toInt(), b.x.toInt(), b.y.toInt())
        }

        fun drawLine(a: Int, b: Int) {
            drawLine(points[triangles[a]], points[triangles[b]])
        }

        drawLine(i, i + 1)
        drawLine(i + 1, i + 2)
        drawLine(i + 2, i)

    }

    BIImage(bi).write(desktop.getChild("triangulation.png"))

}