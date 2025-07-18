package me.anno.tests.image

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.posMod
import me.anno.utils.Color
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.r
import me.anno.utils.OS.desktop
import me.anno.utils.OS.res

fun createUVCheckerImage(): IntImage {

    // load texture with numbers
    val numbers = ImageCache[res.getChild("textures/dig8.png")].waitFor()!!
    val cw = 27 // character size in <numbers>
    val ch = 37
    val co = 17 // character offset from top, ~ch/2

    val innerSize = 10
    val innerFields = 9 // should be odd

    val numFields = 7

    val sizeOfField = innerFields * (innerSize + 1) + 1
    val w = numFields * (sizeOfField + 1) + 1

    val image = IntImage(w, w, false)
    val canvas = image.data

    val colors = intArrayOf(
        0x0b5d7b or Color.black,
        0x5cd5c0 or Color.black,
        0xffe27e or Color.black,
        0xfe8e6f or Color.black,
        0xd22a65 or Color.black,
        0x282828 or Color.black,
        0x7f7f7f or Color.black,
        0xd6d6d6 or Color.black,
    )

    // second step: generate background
    for (iy in 0 until numFields) {
        for (ix in 0 until numFields) {
            val colorIdx = posMod(ix - iy, colors.size)
            val color = colors[colorIdx]
            val x0 = ix * (sizeOfField + 1) + 1
            val y0 = iy * (sizeOfField + 1) + 1
            val y1 = y0 + sizeOfField
            for (y in y0 until y1) {
                val i0 = y * w + x0
                canvas.fill(color, i0, i0 + sizeOfField)
            }
            // draw soft stripes
            val contrastColor = if (colorIdx in 1..4 || colorIdx == 7) Color.black else -1
            val softColor = mixARGB(color, contrastColor, 0.1f)
            for (j in 1 until innerFields) {
                // draw vertical & horizontal stripes
                val i0 = (y0 + j * (innerSize + 1)) * w + x0
                canvas.fill(softColor, i0, i0 + sizeOfField)
                val j0 = y0 * w + x0 + j * (innerSize + 1)
                for (dy in 0 until sizeOfField) {
                    canvas[j0 + dy * w] = softColor
                }
            }
        }
    }

    // draw stripes
    val lineColor = Color.black
    for (i in 0..numFields) {
        val k = i * (sizeOfField + 1)
        canvas.fill(lineColor, k * w, (k + 1) * w)
        var j = k + w
        for (y in 1 until w - 1) {
            canvas[j] = lineColor
            j += w
        }
    }

    // blend numbers over background
    for (iy in 0 until numFields) {
        for (ix in 0 until numFields) {
            val colorIdx = (ix - iy + colors.size) % colors.size
            val contrastColor = if (colorIdx in 1..4 || colorIdx == 7) Color.black else -1
            fun drawChar(srcI: Int, dstI: Int, cw: Int, ch: Int) {
                for (dy in 0 until ch) {
                    val srcJ = srcI + dy * numbers.width
                    val dstJ = dstI + dy * w
                    for (dx in 0 until cw) {
                        val v = numbers.getRGB(srcJ + dx).r() // mix factor
                        val dstIdx = dstJ + dx
                        canvas[dstIdx] = mixARGB(canvas[dstIdx], contrastColor, v)
                    }
                }
            }

            val x0 = ix * (sizeOfField + 1) + 1
            val y0 = iy * (sizeOfField + 1) + 1
            val cx = x0 + sizeOfField / 2
            val cy = y0 + sizeOfField / 2

            // write letter (y), then number (x)
            drawChar(ch * numbers.width + cw * iy, (cy) * w + cx - (co * w) - cw, cw, ch)
            drawChar(cw * ix, (cy) * w + cx - (co * w), cw, ch)
        }
    }

    return image
}

fun main() {
    OfficialExtensions.initForTests()
    createUVCheckerImage()
        .write(desktop.getChild("UVChecker.png"))
    Engine.requestShutdown()
}