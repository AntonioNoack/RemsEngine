package me.anno.utils.image

import me.anno.utils.Color.rgba
import me.anno.utils.OS
import me.anno.utils.files.Files.use
import me.anno.utils.hpc.HeavyProcessing.processBalanced
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.min

object ImageWriter {

    inline fun writeRGBImageByte3(
        w: Int,
        h: Int,
        name: String,
        minPerThread: Int,
        crossinline getRGB: (x: Int, y: Int, i: Int) -> Triple<Byte, Byte, Byte>
    ) {
        writeRGBImageInt(w, h, name, minPerThread) { x, y, i ->
            val (r, g, b) = getRGB(x, y, i)
            rgba(r, g, b, -1)
        }
    }

    inline fun writeRGBImageInt3(
        w: Int,
        h: Int,
        name: String,
        minPerThread: Int,
        crossinline getRGB: (x: Int, y: Int, i: Int) -> Triple<Int, Int, Int>
    ) {
        writeRGBImageInt(w, h, name, minPerThread) { x, y, i ->
            val (r, g, b) = getRGB(x, y, i)
            rgba(r, g, b, 255)
        }
    }

    inline fun writeRGBImageInt(
        w: Int,
        h: Int,
        name: String,
        minPerThread: Int,
        crossinline getRGB: (x: Int, y: Int, i: Int) -> Int
    ) {
        writeImageInt(w, h, false, name, minPerThread, getRGB)
    }

    inline fun writeRGBAImageInt(
        w: Int,
        h: Int,
        name: String,
        minPerThread: Int,
        crossinline getRGB: (x: Int, y: Int, i: Int) -> Int
    ) {
        writeImageInt(w, h, true, name, minPerThread, getRGB)
    }

    inline fun writeImageFloat(
        w: Int, h: Int, name: String,
        minPerThread: Int,
        crossinline getRGB: (x: Int, y: Int, i: Int) -> Float
    ) {
        val img = BufferedImage(w, h, 1)
        if (minPerThread < 0) {// multi-threading is forbidden
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val i = x + y * w
                    img.setRGB(x, y, getColor(getRGB(x, y, i)))
                }
            }
        } else {
            processBalanced(0, w * h, minPerThread) { i0, i1 ->
                for (i in i0 until i1) {
                    val x = i % w
                    val y = i / w
                    img.setRGB(x, y, getColor(getRGB(x, y, i)))
                }
            }
        }
        use(OS.desktop.getChild(name).outputStream()) {
            ImageIO.write(img, if (name.endsWith(".jpg")) "jpg" else "png", it)
        }
    }

    fun getColor(x: Float): Int {
        val v = min((abs(x) * 255).toInt(), 255)
        return if (x < 0f) {
            0x10000
        } else {
            0x10101
        } * v
    }

    inline fun writeImageInt(
        w: Int, h: Int, alpha: Boolean, name: String,
        minPerThread: Int,
        crossinline getRGB: (x: Int, y: Int, i: Int) -> Int
    ) {
        val img = BufferedImage(w, h, if (alpha) 2 else 1)
        if (minPerThread !in 0 until w * h) {// multi-threading is forbidden
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val i = x + y * w
                    img.setRGB(x, y, getRGB(x, y, i))
                }
            }
        } else {
            processBalanced(0, w * h, minPerThread) { i0, i1 ->
                for (i in i0 until i1) {
                    val x = i % w
                    val y = i / w
                    img.setRGB(x, y, getRGB(x, y, i))
                }
            }
        }
        use(OS.desktop.getChild(name).outputStream()) {
            ImageIO.write(img, if (name.endsWith(".jpg")) "jpg" else "png", it)
        }
    }

}