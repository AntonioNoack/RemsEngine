package me.anno.image

import me.anno.Time
import me.anno.image.colormap.ColorMap
import me.anno.image.colormap.LinearColorMap
import me.anno.image.raw.FloatImage
import me.anno.image.raw.IntImage
import me.anno.image.utils.GaussianBlur
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.ui.base.components.Padding
import me.anno.utils.Color
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.OS
import me.anno.utils.callbacks.F2F
import me.anno.utils.callbacks.I3F
import me.anno.utils.callbacks.I3I
import me.anno.utils.hpc.HeavyProcessing.processBalanced2d
import me.anno.utils.types.Floats.toIntOr
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * utility function to write data / images to files using advanced color mappings
 * */
@Suppress("unused")
object ImageWriter {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(ImageWriter::class)

    private const val PARALLEL_TILE_SIZE = 8

    @JvmStatic
    private fun getFile(name: String): FileReference {
        val name2 = if (name.endsWith("png") || name.endsWith("jpg")) name else "$name.png"
        val file = OS.desktop.getChild(name2)
        file.getParent().tryMkdirs()
        return file
    }

    @JvmStatic
    fun writeImage(name: String, img: BufferedImage) {
        val file = getFile(name)
        val pixels = img.getRGB(0, 0, img.width, img.height, null, 0, img.width)
        IntImage(img.width, img.height, pixels, true).write(file)
    }

    @JvmStatic
    fun writeImage(name: String, img: Image) {
        img.write(getFile(name))
    }

    @JvmStatic
    fun writeRGBImageInt(
        w: Int, h: Int, name: String,
        minPerThread: Int, getRGB: I3I // x,y,i -> color
    ): Unit = writeImageInt(w, h, false, name, minPerThread, getRGB)

    @JvmStatic
    fun writeRGBAImageInt(
        w: Int, h: Int, name: String,
        minPerThread: Int, getRGB: I3I
    ): Unit = writeImageInt(w, h, true, name, minPerThread, getRGB)

    @JvmStatic
    fun writeImageFloat(
        w: Int, h: Int, name: String,
        normalize: Boolean, values: FloatArray
    ): Unit = writeImageFloat(w, h, name, normalize, LinearColorMap.default, values)

    @JvmStatic
    fun writeImageFloat(
        w: Int, h: Int, name: String,
        normalize: Boolean, colorMap: ColorMap, values: FloatArray
    ): Unit = writeImageFloatWithOffsetAndStride(w, h, 0, w, name, normalize, colorMap, values)

    @JvmStatic
    fun writeImageFloatWithOffsetAndStride(
        w: Int, h: Int,
        offset: Int, stride: Int,
        name: String,
        normalize: Boolean,
        colorMap: ColorMap,
        values: FloatArray
    ) {
        val image = FloatImage(w, h, 1, values, colorMap, offset, stride)
        if (normalize) image.normalize01()
        image.write(getFile(name))
    }

    @JvmStatic
    fun writeImageFloatMSAA(
        w: Int, h: Int, name: String,
        normalize: Boolean,
        colorMap: ColorMap,
        samples: Int,
        values: FloatArray
    ) {
        if (samples <= 1) {
            return writeImageFloat(w, h, name, normalize, colorMap, values)
        }
        val cm = if (normalize) colorMap.normalized(values) else colorMap
        val imgType = if (colorMap.hasAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB
        val img = BufferedImage(w, h, imgType)
        val buffer = img.raster.dataBuffer
        val alpha = if (colorMap.hasAlpha) 0 else (0xff shl 24)
        for (i in 0 until w * h) {
            var r = 0
            var g = 0
            var b = 0
            var a = 0
            val j = i * samples
            for (sample in 0 until samples) {
                val color = cm.getColor(values[j + sample])
                r += color.r()
                g += color.g()
                b += color.b()
                a += color.a()
            }
            val color = alpha or Color.rgba(r / samples, g / samples, b / samples, a / samples)
            buffer.setElem(i, color)
        }
        writeImage(name, img)
    }

    @JvmStatic
    fun writeImageFloat(
        w: Int, h: Int, name: String,
        minPerThread: Int, normalize: Boolean,
        getRGB: I3F // x,y,i -> v
    ): Unit = writeImageFloat(w, h, name, minPerThread, normalize, LinearColorMap.default, getRGB)

    @JvmStatic
    fun writeImageFloat(
        w: Int, h: Int, name: String,
        minPerThread: Int,
        normalize: Boolean,
        colorMap: ColorMap,
        getRGB: I3F // x,y,i -> v
    ) {
        val values = FloatArray(w * h)
        processBalanced2d(0, 0, w, h, PARALLEL_TILE_SIZE, minPerThread) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                var i = x0 + y * w
                for (x in x0 until x1) {
                    values[i] = getRGB.call(x, y, i)
                    i++
                }
            }
        }
        writeImageFloat(w, h, name, normalize, colorMap, values)
    }

    @JvmField
    val MSAAx8 = floatArrayOf(
        0.058824f, 0.419608f,
        0.298039f, 0.180392f,
        0.180392f, 0.819608f,
        0.419608f, 0.698039f,
        0.580392f, 0.298039f,
        0.941176f, 0.058824f,
        0.698039f, 0.941176f,
        0.819608f, 0.580392f
    )

    @JvmStatic
    fun writeImageFloatMSAA(
        w: Int, h: Int, name: String,
        minPerThread: Int,
        normalize: Boolean,
        getValue: F2F // x,y -> v
    ): Unit = writeImageFloatMSAA(w, h, name, minPerThread, normalize, LinearColorMap.default, getValue)

    @JvmStatic
    fun writeImageFloatMSAA(
        w: Int, h: Int, name: String,
        minPerThread: Int,
        normalize: Boolean,
        colorMap: ColorMap,
        getValue: F2F
    ) {
        val samples = 8
        val values = FloatArray(w * h * samples)
        processBalanced2d(
            0, 0, w, h, PARALLEL_TILE_SIZE,
            minPerThread / (PARALLEL_TILE_SIZE * PARALLEL_TILE_SIZE)
        ) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                var i = x0 + y * w
                for (x in x0 until x1) {
                    val xf = x.toFloat()
                    val yf = y.toFloat()
                    val k = i * samples
                    for (j in 0 until samples) {
                        val j2 = j shl 1
                        values[k + j] = getValue.call(
                            xf + MSAAx8[j2],
                            yf + MSAAx8[j2 + 1]
                        )
                    }
                    i++
                }
            }
        }
        return writeImageFloatMSAA(w, h, name, normalize, colorMap, samples, values)
    }

    @JvmStatic
    fun getColor(x: Float): Int {
        if (x.isNaN()) return 0x0000ff
        val v = min((abs(x) * 255f).toInt(), 255)
        return if (x < 0f) {
            0x10000
        } else {
            0x10101
        } * v
    }

    @JvmStatic
    fun getColor(v: Float, minColor: Int, zeroColor: Int, maxColor: Int, nanColor: Int): Int {
        return when {
            v.isFinite() -> Color.mixARGB(zeroColor, if (v < 0) minColor else maxColor, abs(v))
            v.isNaN() -> nanColor
            v < 0f -> minColor
            else -> maxColor
        }
    }

    @JvmStatic
    fun writeImageInt(
        w: Int, h: Int, alpha: Boolean, name: String,
        minPerThread: Int,
        getRGB: I3I
    ) {
        val img = IntImage(w, h, alpha)
        val buffer = img.data
        processBalanced2d(
            0, 0, w, h, PARALLEL_TILE_SIZE,
            max(minPerThread / (PARALLEL_TILE_SIZE * PARALLEL_TILE_SIZE), 1)
        ) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                var i = y * w + x0
                for (x in x0 until x1) {
                    buffer[i] = getRGB.call(x, y, i)
                    i++
                }
            }
        }
        writeImage(name, img)
    }

    @JvmStatic
    fun writeImageInt(
        w: Int, h: Int, alpha: Boolean,
        name: String,
        pixels: IntArray
    ) = writeImage(name, IntImage(w, h, pixels, alpha))

    @JvmStatic
    private fun addPoint(image: FloatArray, w: Int, x: Int, y: Int, v: Float) {
        if (x in 0 until w && y >= 0) {
            val index = x + y * w
            if (index < image.size) {
                image[index] += v
            }
        }
    }

    @JvmStatic
    fun writeImageCurve(
        wr: Int, hr: Int,
        autoScale: Boolean, scaleIndependently: Boolean,
        backgroundColor: Int, lineColor: Int,
        thickness: Int, points: List<Vector2f>, name: String
    ) {
        val w = wr + 2 * thickness
        val h = hr + 2 * thickness
        val image = FloatArray(w * h)
        // do all line sections
        var ctr = 0f
        val t0 = Time.nanoTime

        val transform = Matrix3x2f()
        if (autoScale) {
            val bounds = AABBf()
            for (p in points) {
                if (p.isFinite) bounds.union(p)
            }
            transform.translate(w / 2f, h / 2f)
            val sx = w / bounds.deltaX
            val sy = h / bounds.deltaY
            if (scaleIndependently) {
                transform.scale(sx, sy)
            } else {
                transform.scale(min(sx, sy))
            }
            transform.scale(0.95f)
            transform.translate(-bounds.centerX, -bounds.centerY)
            for (p in points) transform.transformPosition(p)
        }
        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]
            val distance = p0.distance(p1)
            if (!distance.isFinite()) continue
            val steps = ceil(distance)
            val invSteps = 1f / steps
            val stepSize = distance * invSteps
            ctr += steps
            for (j in 0 until steps.toIntOr()) {
                // add point
                val f = j * invSteps
                val x = Maths.mix(p0.x, p1.x, f) + thickness
                val y = Maths.mix(p0.y, p1.y, f) + thickness
                val xi = floor(x)
                val yi = floor(y)
                val fx = x - xi
                val fy = y - yi
                val gx = 1f - fx
                val gy = 1f - fy
                val s0 = stepSize * gx
                val s1 = stepSize * fx
                val w0 = s0 * gy
                val w1 = s0 * fy
                val w2 = s1 * gy
                val w3 = s1 * fy
                val ix = xi.toIntOr()
                val iy = yi.toIntOr()
                addPoint(image, w, ix, iy, w0)
                addPoint(image, w, ix, iy + 1, w1)
                addPoint(image, w, ix + 1, iy, w2)
                addPoint(image, w, ix + 1, iy + 1, w3)
            }
        }
        // bokeh-blur would be nicer, and correcter,
        // but this is a pretty good trade-off between visuals and performance :)
        GaussianBlur.gaussianBlur(image, w, h, 0, w, thickness, true)
        val t1 = Time.nanoTime
        // nanoseconds per pixel
        // ~ 24ns/px for everything, including copy;
        // for 2048² pixels, and thickness = 75
        LOGGER.info("${(t1 - t0).toFloat() / (w * h)}ns/px")
        val cm = LinearColorMap(backgroundColor, backgroundColor, lineColor)
        writeImageFloatWithOffsetAndStride(wr, hr, thickness * (w + 1), w, name, false, cm, image)
    }

    @JvmStatic
    fun writeImageProfile(
        values: FloatArray, h: Int,
        name: String,
        normalize: Boolean,
        map: ColorMap = LinearColorMap.default,
        background: Int = -1,
        foreground: Int = 0xff shl 24,
        alpha: Boolean = false,
        padding: Padding = Padding((values.size + h) / 50 + 2)
    ) {
        val map2 = if (normalize) map.normalized(values) else map
        writeImageProfile(values, h, name, map2, background, foreground, alpha, padding)
    }

    @JvmStatic
    fun writeImageProfile(
        values: FloatArray, h: Int,
        name: String,
        map: ColorMap = LinearColorMap.default,
        background: Int = -1,
        foreground: Int = 0xff shl 24,
        alpha: Boolean = false,
        padding: Padding = Padding((values.size + h) / 50 + 2)
    ) {
        // todo different backgrounds for positive & negative values?
        // todo on steep sections, use more points
        // todo option to make the colored line thicker
        // to do write x and y axis notations? (currently prefer no)
        // define padding
        // todo support stretching (interpolation/averaging) of values?
        val w = values.size
        val w2 = w + padding.width
        val h2 = h + padding.height
        val pixels = IntArray(w2 * h2)
        val min = map.min
        val max = map.max
        // to do better (memory aligned) writes?
        val my = h - 1
        pixels.fill(background)
        for (x in 0 until w) {
            val offset = x + padding.left + padding.top * w2
            val v = values[x]
            if (v in min..max) {
                val color = map.getColor(v)
                val cy = Maths.clamp((Maths.unmix(max, min, v) * h).toInt(), 0, my)
                // draw background until there ... done automatically
                // draw colored pixel
                pixels[offset + cy * w2] = color
                // draw foreground from there
                for (y in cy + 1 until h) {
                    pixels[offset + y * w2] = foreground
                }
            }
        }
        writeImageInt(w2, h2, alpha, name, pixels)
    }

    data class ColoredLine(val a: Vector2f, val b: Vector2f, val lineColor: Int, val pointColor: Int = lineColor)

    @JvmStatic
    fun writeTriangles(size: Int, name: String, points: List<Vector2f>) {
        return writeTriangles(size, name, points, IntArray(points.size) { it })
    }

    @JvmStatic
    fun writeTriangles(size: Int, name: String, points: List<Vector2f>, indices: IntArray) {
        val bounds = AABBf()
        for (p in points) {
            bounds.union(p.x, p.y, 0f)
        }

        val s = size / max(bounds.deltaX, bounds.deltaY)

        val ox = bounds.centerX - (size / 2f) / s
        val oy = bounds.centerY - (size / 2f) / s

        val bi = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val gfx = bi.graphics as Graphics2D
        val random = Random(1234L)
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        // draw all points
        for (p in points) {
            gfx.color = java.awt.Color(0x777777 or random.nextInt() or (255 shl 24))
            val ax = ((p.x - ox) * s).toIntOr()
            val ay = ((p.y - oy) * s).toIntOr()
            gfx.drawOval(ax, ay, 1, 1)
        }
        // draw all triangles
        for (triIndex in 0 until indices.size / 3) {

            gfx.color = java.awt.Color(0x777777 or random.nextInt() or (255 shl 24))

            val i = triIndex * 3

            val a = points[indices[i]]
            val b = points[indices[i + 1]]
            val c = points[indices[i + 2]]

            val ax = ((a.x - ox) * s).toIntOr()
            val ay = ((a.y - oy) * s).toIntOr()
            val bx = ((b.x - ox) * s).toIntOr()
            val by = ((b.y - oy) * s).toIntOr()
            val cx = ((c.x - ox) * s).toIntOr()
            val cy = ((c.y - oy) * s).toIntOr()

            gfx.drawLine(ax, ay, bx, by)
            gfx.drawLine(bx, by, cx, cy)
            gfx.drawLine(cx, cy, ax, ay)

            gfx.color = java.awt.Color(gfx.color.rgb and 0x77ffffff, true)
            gfx.fill(Polygon(intArrayOf(ax, bx, cx), intArrayOf(ay, by, cy), 3))

            val px = (ax + bx + cx) / 3
            val py = (ay + by + cy) / 3

            gfx.fillOval(px - 2, py - 2, 5, 5)
        }
        OS.desktop.getChild(name).outputStream().use {
            ImageIO.write(bi, "png", it)
        }
    }

    @JvmStatic
    fun writeLines(size: Int, name: String, lines: List<ColoredLine>) {

        val bounds = AABBf()
        for ((a, b) in lines) {
            bounds.union(a.x, a.y, 0f)
            bounds.union(b.x, b.y, 0f)
        }

        val s = size * 0.95f / max(bounds.deltaX, bounds.deltaY)

        val ox = bounds.centerX - (size / 2f) / s
        val oy = bounds.centerY - (size / 2f) / s

        val bi = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val gfx = bi.graphics as Graphics2D
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        // draw all points
        fun drawPoint(p: Vector2f, color: Int) {
            if (color.a() == 0) return
            gfx.color = java.awt.Color(color)
            val ax = ((p.x - ox) * s).toIntOr()
            val ay = ((p.y - oy) * s).toIntOr()
            gfx.drawOval(ax, ay, 1, 1)
        }
        for ((a, b, _, color) in lines) {
            drawPoint(a, color)
            drawPoint(b, color)
        }
        // draw all triangles
        for ((a, b, color) in lines) {
            if (color.a() == 0) continue
            val ax = ((a.x - ox) * s).toIntOr()
            val ay = ((a.y - oy) * s).toIntOr()
            val bx = ((b.x - ox) * s).toIntOr()
            val by = ((b.y - oy) * s).toIntOr()
            gfx.color = java.awt.Color(color)
            gfx.drawLine(ax, ay, bx, by)
        }
        OS.desktop.getChild(name).outputStream().use {
            ImageIO.write(bi, "png", it)
        }
    }
}