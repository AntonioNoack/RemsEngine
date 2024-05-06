package me.anno.image

import me.anno.cache.ICacheData
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpImageFile
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.min
import me.anno.maths.Maths.roundDiv
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.mixARGB22d
import me.anno.utils.InternalAPI
import me.anno.utils.Logging.hash32
import me.anno.utils.structures.Callback
import me.anno.utils.structures.lists.Lists.createArrayList
import org.apache.logging.log4j.LogManager
import java.io.OutputStream
import kotlin.math.floor
import kotlin.math.nextDown
import kotlin.math.roundToInt


abstract class Image(
    open var width: Int,
    open var height: Int,
    var numChannels: Int,
    var hasAlphaChannel: Boolean
) : ICacheData {

    override fun toString(): String {
        return "${javaClass.name}@${hash32(this)}[$width x $height x $numChannels${if (hasAlphaChannel) ", alpha" else ""}]"
    }

    open fun getIndex(x: Int, y: Int): Int {
        val xi = clamp(x, 0, width - 1)
        val yi = clamp(y, 0, height - 1)
        return xi + yi * width
    }

    open fun createIntImage(): IntImage {
        val width = width
        val height = height
        val data = IntArray(width * height)
        val image = IntImage(width, height, data, hasAlphaChannel)
        var i = 0
        val size = width * height
        while (i < size) {
            data[i] = getRGB(i)
            i++
        }
        return image
    }

    abstract fun getRGB(index: Int): Int

    fun getValueAt(x: Float, y: Float, shift: Int): Float {
        val xf = floor(x)
        val yf = floor(y)
        val xi = xf.toInt()
        val yi = yf.toInt()
        val fx = x - xf
        val gx = 1f - fx
        val fy = y - yf
        val gy = 1f - fy
        var c00: Int
        var c01: Int
        var c10: Int
        var c11: Int
        val width = width
        if (xi >= 0 && yi >= 0 && xi < width - 1 && yi < height - 1) {
            // safe
            val index = xi + yi * width
            c00 = getRGB(index)
            c01 = getRGB(index + width)
            c10 = getRGB(index + 1)
            c11 = getRGB(index + 1 + width)
        } else {
            // border
            c00 = getSafeRGB(xi, yi)
            c01 = getSafeRGB(xi, yi + 1)
            c10 = getSafeRGB(xi + 1, yi)
            c11 = getSafeRGB(xi + 1, yi + 1)
        }
        c00 = c00.shr(shift).and(255)
        c01 = c01.shr(shift).and(255)
        c10 = c10.shr(shift).and(255)
        c11 = c11.shr(shift).and(255)
        val r0 = c00 * gy + fy * c01
        val r1 = c10 * gy + fy * c11
        return r0 * gx + fx * r1
    }

    fun getRGB(x: Int, y: Int): Int {
        return getRGB(x + y * width)
    }

    fun sampleRGB(x: Float, y: Float, filtering: Filtering, clamping: Clamping): Int {
        return when (filtering) {
            Filtering.TRULY_NEAREST,
            Filtering.NEAREST -> getRGB(
                clamping.apply(floor(x).toInt(), width),
                clamping.apply(floor(y).toInt(), height)
            )
            Filtering.TRULY_LINEAR,
            Filtering.LINEAR -> {
                val xf = floor(x)
                val xi = xf.toInt()
                val fx = x - xf
                val yf = floor(y)
                val yi = yf.toInt()
                val fy = y - yf
                val width = width
                val height = height
                val x0 = clamping.apply(xi, width)
                val x1 = clamping.apply(xi + 1, width)
                val y0 = clamping.apply(yi, height)
                val y1 = clamping.apply(yi + 1, height)
                mixARGB22d(
                    getRGB(x0, y0), getRGB(x0, y1),
                    getRGB(x1, y0), getRGB(x1, y1),
                    fx, fy
                )
            }
        }
    }

    fun getSafeRGB(x: Int, y: Int): Int {
        return getRGB(getIndex(x, y))
    }

    open fun createTexture(
        texture: Texture2D, sync: Boolean, checkRedundancy: Boolean,
        callback: Callback<ITexture2D>
    ) {
        texture.create(createIntImage(), sync = sync, checkRedundancy = true, callback)
    }

    open fun resized(dstWidth: Int, dstHeight: Int, allowUpscaling: Boolean): Image {

        // todo add optional padding, if the aspect ratio isn't fitting

        var dstWidth1 = dstWidth
        var dstHeight1 = dstHeight

        val srcWidth = width
        val srcHeight = height

        if (!allowUpscaling) {
            if (dstWidth1 > srcWidth) {
                dstHeight1 = roundDiv(dstHeight1 * srcWidth, dstWidth1)
                dstWidth1 = srcWidth
            }

            if (dstHeight1 > srcHeight) {
                dstWidth1 = roundDiv(dstWidth1 * srcHeight, dstHeight1)
                dstHeight1 = srcHeight
            }
        }

        if (dstWidth1 == srcWidth && dstHeight1 == srcHeight) {
            return this
        }

        val tmp = if (dstWidth1 < srcWidth) downscaleX(dstWidth1)
        else upscaleX(dstWidth1)
        return if (dstHeight1 < srcHeight) tmp.downscaleY(dstHeight1)
        else tmp.upscaleY(dstHeight1)
    }

    private fun downscaleX(dstWidth: Int): Image {

        val dstHeight = height
        val srcWidth = width

        if (dstWidth == srcWidth) return this

        val img = IntImage(dstWidth, dstHeight, hasAlphaChannel)

        val xf = FloatArray(dstWidth + 1)

        val xi = IntArray(dstWidth + 1)

        val sx = srcWidth.toFloat() / dstWidth
        val maxWidth = srcWidth.toFloat().nextDown()

        for (i in xf.indices) {
            xf[i] = min(i * sx, maxWidth)
            xi[i] = xf[i].toInt()
        }

        // area is constant
        val dst = img.data
        val invArea = 1f / sx
        var di = 0
        for (yd in 0 until dstHeight) {
            for (xd in 0 until dstWidth) {

                val x0 = xf[xd]
                val x1 = xf[xd + 1]

                val x0i = xi[xd]
                val x1i = xi[xd + 1]

                // accumulate
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f

                val xs0 = if (x0i == x1i) x1 - x0 else 1f - fract(x0)

                for (xs in x0i..x1i) {
                    val wx = if (xs == x0i) xs0 else if (xs == x1i) fract(x1) else 1f
                    val color = getRGB(xs, yd)
                    r += wx * color.shr(16).and(255)
                    g += wx * color.shr(8).and(255)
                    b += wx * color.and(255)
                    a += wx * color.ushr(24)
                }

                dst[di++] = argb(
                    (a * invArea).roundToInt(),
                    (r * invArea).roundToInt(),
                    (g * invArea).roundToInt(),
                    (b * invArea).roundToInt()
                )
            }
        }

        return img
    }

    private fun downscaleY(dstHeight: Int): Image {

        val srcWidth = width
        val srcHeight = height

        if (dstHeight == srcHeight) return this

        val img = IntImage(srcWidth, dstHeight, hasAlphaChannel)

        val yf = FloatArray(dstHeight + 1)
        val yi = IntArray(dstHeight + 1)

        val sx = srcWidth.toFloat() / srcWidth
        val sy = srcHeight.toFloat() / dstHeight
        val maxHeight = srcHeight.toFloat().nextDown()
        for (i in yf.indices) {
            yf[i] = min(i * sy, maxHeight)
            yi[i] = yf[i].toInt()
        }

        // area is constant
        val dst = img.data
        val invArea = 1f / (sx * sy)
        var di = 0
        for (yd in 0 until dstHeight) {
            for (xd in 0 until srcWidth) {

                val y0 = yf[yd]
                val y1 = yf[yd + 1]

                val y0i = yi[yd]
                val y1i = yi[yd + 1]

                // accumulate
                var r = 0f
                var g = 0f
                var b = 0f
                var a = 0f

                val ys0 = if (y0i == y1i) y1 - y0 else 1f - fract(y0)

                val c0 = getRGB(xd, y0i)
                r += ys0 * c0.shr(16).and(255)
                g += ys0 * c0.shr(8).and(255)
                b += ys0 * c0.and(255)
                a += ys0 * c0.ushr(24)

                if (y1i > y0i) {
                    for (ys in y0i + 1 until y1i) {
                        val color = getRGB(xd, ys)
                        r += color.shr(16).and(255)
                        g += color.shr(8).and(255)
                        b += color.and(255)
                        a += color.ushr(24)
                    }
                    val wy = fract(y1)
                    val c1 = getRGB(xd, y1i)
                    r += wy * c1.shr(16).and(255)
                    g += wy * c1.shr(8).and(255)
                    b += wy * c1.and(255)
                    a += wy * c1.ushr(24)
                }

                dst[di++] = argb(
                    (a * invArea).roundToInt(),
                    (r * invArea).roundToInt(),
                    (g * invArea).roundToInt(),
                    (b * invArea).roundToInt()
                )
            }
        }

        return img
    }

    private fun upscaleX(dstWidth: Int): Image {
        val srcWidth = width
        if (dstWidth == srcWidth) return this
        val height = height

        val img = IntImage(dstWidth, height, hasAlphaChannel)

        // area is constant
        val dst = img.data
        var di = 0
        val dFract = srcWidth.toFloat() / dstWidth
        val maxI = srcWidth - 1
        for (yd in 0 until height) {
            var fract = 0f
            var i0 = 0
            var i1 = min(1, maxI)
            for (xd in 0 until dstWidth) {
                dst[di++] = mixARGB(getRGB(i0, yd), getRGB(i1, yd), fract)
                fract += dFract
                while (fract >= 1f) {
                    fract--
                    i0++
                    i1 = min(i1 + 1, maxI)
                }
            }
        }

        return img
    }

    private fun upscaleY(dstHeight: Int): Image {
        val srcHeight = height
        if (dstHeight == srcHeight) return this
        val width = width

        val img = IntImage(width, dstHeight, hasAlphaChannel)

        // area is constant
        val dst = img.data
        val dFract = srcHeight.toFloat() / dstHeight
        val maxI = srcHeight - 1
        for (xd in 0 until width) {
            var di = xd
            var fract = 0f
            var i0 = 0
            var i1 = min(1, maxI)
            for (yd in 0 until dstHeight) {
                dst[di] = mixARGB(getRGB(xd, i0), getRGB(xd, i1), fract)
                di += width
                fract += dFract
                while (fract >= 1f) {
                    fract--
                    i0++
                    i1 = min(i1 + 1, maxI)
                }
            }
        }

        return img
    }

    open fun write(dst: FileReference, quality: Float = 0.9f) {
        val format = dst.lcExtension
        dst.outputStream().use { out -> write(out, format, quality) }
    }

    fun write(dst: OutputStream, format: String, quality: Float = 0.9f) {
        val wim = writeImageImpl
        if (wim != null) {
            wim(this, dst, format, quality)
        } else LOGGER.warn("Missing Image Writer")
    }

    override fun destroy() {}

    /**
     * for debugging and easier seeing pixels
     * */
    open fun scaleUp(sx: Int, sy: Int): Image {
        return object : Image(width * sx, height * sy, numChannels, hasAlphaChannel) {
            override fun getRGB(index: Int): Int {
                val x = (index % this.width) / sx
                val y = (index / this.width) / sy
                return this@Image.getRGB(x, y)
            }
        }
    }

    open fun split(sx: Int, sy: Int): List<Image> {
        return createArrayList(sx * sy) {
            val ix = it % sx
            val iy = it / sx
            val x0 = ix * width / sx
            val x1 = (ix + 1) * width / sx
            val y0 = iy * height / sy
            val y1 = (iy + 1) * height / sy
            cropped(x0, y0, x1 - x0, y1 - y0)
        }
    }

    open fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        if (x0 + w0 > width || y0 + h0 > height)
            throw IllegalArgumentException("Cannot crop $x0+=$w0,$y0+=$h0 from ${width}x${height}")
        val self = this
        return object : Image(w0, h0, numChannels, hasAlphaChannel) {
            override fun getRGB(index: Int): Int {
                val x = index % w0
                val y = index / w0
                return self.getRGB(x0 + x, y0 + y)
            }
        }
    }

    var ref: FileReference = InvalidRef
        get() {
            if (field == InvalidRef) {
                field = InnerTmpImageFile(this)
            }
            return field
        }

    companion object {

        private val LOGGER = LogManager.getLogger(Image::class)

        @InternalAPI
        var writeImageImpl: ((Image, OutputStream, format: String, quality: Float) -> Unit)? = null

        @JvmStatic
        fun argb(a: Byte, r: Byte, g: Byte, b: Byte): Int {
            return argb(
                a.toUInt().toInt(), r.toUInt().toInt(),
                g.toUInt().toInt(), b.toUInt().toInt()
            )
        }

        @JvmStatic
        fun argb(a: Int, r: Int, g: Int, b: Int): Int {
            return (a shl 24) + (r shl 16) + (g shl 8) + b
        }
    }
}