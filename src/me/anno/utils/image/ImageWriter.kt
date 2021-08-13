package me.anno.utils.image

import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.Maths.mixARGB
import me.anno.utils.OS
import me.anno.utils.files.Files.use
import me.anno.utils.hpc.HeavyProcessing.processBalanced
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
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

    fun writeImageFloat(
        w: Int, h: Int, name: String,
        normalize: Boolean,
        minColor: Int,
        zeroColor: Int,
        maxColor: Int,
        nanColor: Int,
        values: FloatArray
    ) {
        if (normalize) normalizeValues(values)
        val img = BufferedImage(w, h, 1)
        val buffer = img.data.dataBuffer
        for (i in 0 until w * h) {
            val v = values[i]
            val color = when {
                v.isFinite() -> mixARGB(zeroColor, if (v < 0) minColor else maxColor, abs(v))
                v.isNaN() -> nanColor
                else -> if (v < 0f) minColor else maxColor // todo special colors for infinity?
            }
            buffer.setElem(i, color)
        }
        val file = OS.desktop.getChild(name)
        file.getParent()?.mkdirs()
        use(file.outputStream()) {
            ImageIO.write(img, if (name.endsWith(".jpg")) "jpg" else "png", it)
        }
    }

    fun writeImageFloatMSAA(
        w: Int, h: Int, name: String,
        normalize: Boolean,
        minColor: Int,
        zeroColor: Int,
        maxColor: Int,
        nanColor: Int,
        samples: Int,
        values: FloatArray
    ) {
        if (normalize) normalizeValues(values)
        val img = BufferedImage(w, h, 1)
        val buffer = img.data.dataBuffer
        for (i in 0 until w * h) {
            var r = 0
            var g = 0
            var b = 0
            val j = i * samples
            for (sample in 0 until samples) {
                val v = values[j + sample]
                val color = when {
                    v.isFinite() -> mixARGB(zeroColor, if (v < 0) minColor else maxColor, abs(v))
                    v.isNaN() -> nanColor
                    else -> if (v < 0f) minColor else maxColor
                }
                r += color.r()
                g += color.g()
                b += color.b()
            }
            buffer.setElem(i, rgba(r, g, b, 255))
        }
        val file = OS.desktop.getChild(name)
        file.getParent()?.mkdirs()
        use(file.outputStream()) {
            ImageIO.write(img, if (name.endsWith(".jpg")) "jpg" else "png", it)
        }
    }

    inline fun writeImageFloat(
        w: Int, h: Int, name: String,
        minPerThread: Int,
        normalize: Boolean,
        minColor: Int = 0xff0000,
        zeroColor: Int = 0,
        maxColor: Int = 0xffffff,
        nanColor: Int = 0x0077ff,
        crossinline getRGB: (x: Int, y: Int, i: Int) -> Float
    ) {
        val values = FloatArray(w * h)
        if (minPerThread < 0) {// multi-threading is forbidden
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val i = x + y * w
                    values[i] = getRGB(x, y, i)
                }
            }
        } else {
            processBalanced(0, w * h, minPerThread) { i0, i1 ->
                for (i in i0 until i1) {
                    val x = i % w
                    val y = i / w
                    values[i] = getRGB(x, y, i)
                }
            }
        }
        return writeImageFloat(w, h, name, normalize, minColor, zeroColor, maxColor, nanColor, values)
    }


    inline fun writeImageFloatMSAA(
        w: Int, h: Int, name: String,
        minPerThread: Int,
        normalize: Boolean,
        minColor: Int = 0xff0000,
        zeroColor: Int = 0,
        maxColor: Int = 0xffffff,
        nanColor: Int = 0x0077ff,
        crossinline getRGB: (x: Float, y: Float) -> Float
    ) {
        val samples = 8
        val positions = floatArrayOf(
            0.058824f, 0.419608f,
            0.298039f, 0.180392f,
            0.180392f, 0.819608f,
            0.419608f, 0.698039f,
            0.580392f, 0.298039f,
            0.941176f, 0.058824f,
            0.698039f, 0.941176f,
            0.819608f, 0.580392f
        )
        val values = FloatArray(w * h * samples)
        if (minPerThread < 0) {// multi-threading is forbidden
            for (y in 0 until h) {
                val yf = y.toFloat()
                for (x in 0 until w) {
                    val k = (x + y * w) * samples
                    val xf = x.toFloat()
                    for (j in 0 until samples) {
                        values[k + j] = getRGB(
                            xf + positions[j * 2],
                            yf + positions[j * 2 + 1]
                        )
                    }
                }
            }
        } else {
            processBalanced(0, w * h, minPerThread) { i0, i1 ->
                for (i in i0 until i1) {
                    val x = i % w
                    val y = i / w
                    val xf = x.toFloat()
                    val yf = y.toFloat()
                    val k = i * samples
                    for (j in 0 until samples) {
                        values[k + j] = getRGB(
                            xf + positions[j * 2],
                            yf + positions[j * 2 + 1]
                        )
                    }
                }
            }
        }
        return writeImageFloatMSAA(w, h, name, normalize, minColor, zeroColor, maxColor, nanColor, samples, values)
    }

    fun getColor(x: Float): Int {
        if (x.isNaN()) return 0x0000ff
        val v = min((abs(x) * 255).toInt(), 255)
        return if (x < 0f) {
            0x10000
        } else {
            0x10101
        } * v
    }

    fun getColor(v: Float, minColor: Int, zeroColor: Int, maxColor: Int, nanColor: Int): Int {
        return when {
            v.isFinite() -> mixARGB(zeroColor, if (v < 0) minColor else maxColor, abs(v))
            v.isNaN() -> nanColor
            else -> if (v < 0f) minColor else maxColor
        }
    }

    fun normalizeValues(values: FloatArray) {
        var max = 0f
        for (i in values.indices) {
            val v = values[i]
            if (v.isFinite()) {
                max = max(max, abs(v))
            }
        }
        if (max > 0f) {
            max = 1f / max
            for (i in values.indices) {
                values[i] *= max
            }
        }
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