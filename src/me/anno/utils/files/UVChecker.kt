package me.anno.utils.files

import me.anno.config.DefaultStyle.black
import me.anno.image.ImageCPUCache
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.io.zip.InnerTmpFile
import me.anno.maths.Maths
import me.anno.utils.Color.r

val UVChecker = lazy {

    // generate uv checker texture to save 500kB of storage space

    val lineColor = black

    // load texture with numbers
    val numbers = ImageCPUCache.getImage(FileReference.getReference("res://dig8.png"), false)!!

    val innerSize = 10
    val innerFields = 9 // should be odd

    val numFields = 7

    val sizeOfField = innerFields * (innerSize + 1) + 1
    val w = numFields * (sizeOfField + 1) + 1

    val image = IntImage(w, w, false)
    val canvas = image.data

    val colors = intArrayOf(
        0x0b5d7b,
        0x5cd5c0,
        0xffe27e,
        0xfe8e6f,
        0xd22a65,
        0x282828,
        0x7f7f7f,
        0xd6d6d6,
    )

    // second step: generate background
    for (iy in 0 until numFields) {
        for (ix in 0 until numFields) {
            val colorIdx = (ix - iy + colors.size) % colors.size
            val color = colors[colorIdx]
            val x0 = ix * (sizeOfField + 1) + 1
            val y0 = iy * (sizeOfField + 1) + 1
            val y1 = y0 + sizeOfField
            for (y in y0 until y1) {
                val i0 = y * w + x0
                canvas.fill(color, i0, i0 + sizeOfField)
            }
            // draw soft stripes
            val contrastColor = if (colorIdx in 1..3) black else -1
            val softColor = Maths.mixARGB(color, contrastColor, 0.1f)
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
            val contrastColor = if (colorIdx in 1..3) black else -1

            fun drawChar(srcI: Int, dstI: Int, cw: Int, ch: Int) {
                for (dy in 0 until ch) {
                    val srcJ = srcI + dy * numbers.width
                    val dstJ = dstI + dy * w
                    for (dx in 0 until cw) {
                        val v = numbers.getRGB(srcJ + dx).r() // mix factor
                        val dstIdx = dstJ + dx
                        canvas[dstIdx] = Maths.mixARGB(canvas[dstIdx], contrastColor, v)
                    }
                }
            }

            val x0 = ix * (sizeOfField + 1) + 1
            val y0 = iy * (sizeOfField + 1) + 1
            val cx = x0 + sizeOfField / 2
            val cy = y0 + sizeOfField / 2

            // write letter (y), then number (x)
            drawChar(37 * numbers.width + 27 * iy, (cy) * w + cx - (17 * w) - 27, 27, 37)
            drawChar(27 * ix, (cy) * w + cx - (17 * w), 27, 37)
        }
    }

    InnerTmpFile.InnerTmpImageFile(image)

}