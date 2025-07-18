package me.anno.image

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.image.bmp.BMPWriter
import me.anno.image.hdr.HDRWriter
import me.anno.image.raw.IFloatImage
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpImageFile
import me.anno.maths.Maths.clamp
import me.anno.utils.Color.mixARGB22d
import me.anno.utils.Color.toARGB
import me.anno.utils.InternalAPI
import me.anno.utils.Logging.hash32
import me.anno.utils.async.Callback
import me.anno.utils.callbacks.I2U
import me.anno.utils.hpc.WorkSplitter
import org.joml.Vector4f
import java.io.OutputStream
import kotlin.math.floor

abstract class Image(
    open var width: Int,
    open var height: Int,
    var numChannels: Int,
    var hasAlphaChannel: Boolean,
    var offset: Int,
    var stride: Int,
) : ICacheData {

    constructor(width: Int, height: Int, numChannels: Int, hasAlphaChannel: Boolean) :
            this(width, height, numChannels, hasAlphaChannel, 0, width)

    override fun toString(): String {
        return "${this::class.simpleName}@${hash32(this)}[$width x $height x $numChannels${if (hasAlphaChannel) ", alpha" else ""}]"
    }

    open fun getIndex(x: Int, y: Int): Int {
        val xi = clamp(x, 0, width - 1)
        val yi = clamp(y, 0, height - 1)
        return offset + xi + yi * stride
    }

    open fun asIntImage(): IntImage {
        return cloneToIntImage()
    }

    open fun cloneToIntImage(): IntImage {
        val width = width
        val height = height
        val data = IntArray(width * height)
        var i = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                data[i++] = getRGB(x, y)
            }
        }
        return IntImage(width, height, data, hasAlphaChannel)
    }

    /**
     * get the argb-color at that pixel index;
     * please use getIndex() or getRGB(x,y) instead of calculating the index yourself
     * */
    abstract fun getRGB(index: Int): Int

    /**
     * return r/g/b/a value at shift(r=16,g=8,b=0,a=24) as a float [0,255]
     * */
    fun getValueAt(x: Float, y: Float, shift: Int): Float {
        val xf = floor(x)
        val yf = floor(y)
        val xi = xf.toInt()
        val yi = yf.toInt()
        val fx = x - xf
        val gx = 1f - fx
        val fy = y - yf
        val gy = 1f - fy
        val c00 = getSafeRGB(xi, yi).shr(shift).and(255)
        val c01 = getSafeRGB(xi, yi + 1).shr(shift).and(255)
        val c10 = getSafeRGB(xi + 1, yi).shr(shift).and(255)
        val c11 = getSafeRGB(xi + 1, yi + 1).shr(shift).and(255)
        val r0 = c00 * gy + fy * c01
        val r1 = c10 * gy + fy * c11
        return r0 * gx + fx * r1
    }

    /**
     * returns argb value at these coordinates;
     * might crash if x or y are out of bounds
     * */
    fun getRGB(x: Int, y: Int): Int {
        return getRGB(getIndex(x, y))
    }

    fun setRGB(x: Int, y: Int, value: Int): Boolean {
        val validCoords = x in 0 until width && y in 0 until height
        if (validCoords) setRGB(getIndex(x, y), value)
        return validCoords
    }

    abstract fun setRGB(index: Int, value: Int)

    fun setRGB(x: Int, y: Int, value: Vector4f): Boolean {
        val validCoords = x in 0 until width && y in 0 until height
        if (validCoords) setRGB(getIndex(x, y), value)
        return validCoords
    }

    open fun setRGB(index: Int, value: Vector4f) {
        setRGB(index, value.toARGB())
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

    /**
     * returns argb value at these coordinates;
     * clamped access, if x or y are out of bounds
     * */
    fun getSafeRGB(x: Int, y: Int): Int {
        val xi = clamp(x, 0, width - 1)
        val yi = clamp(y, 0, height - 1)
        return getRGB(xi, yi)
    }

    fun createTexture(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        if (GFX.isGFXThread()) {
            createTextureImpl(texture, checkRedundancy, callback)
        } else {
            addGPUTask("Image.createTexture", width, height) {
                createTextureImpl(texture, checkRedundancy, callback)
            }
        }
    }

    open fun createTextureImpl(texture: Texture2D, checkRedundancy: Boolean, callback: Callback<ITexture2D>) {
        texture.create(asIntImage(), sync = true, checkRedundancy = true, callback)
    }

    open fun resized(dstWidth: Int, dstHeight: Int, allowUpscaling: Boolean): Image {
        return ImageResizing.resized(this, dstWidth, dstHeight, allowUpscaling)
    }

    fun resampled(dstWidth: Int, dstHeight: Int): Image {
        return ResampledImage(this, dstWidth, dstHeight)
    }

    open fun write(dst: FileReference, quality: Float = 0.9f) {
        val format = dst.lcExtension
        dst.outputStream().use { out -> write(out, format, quality) }
    }

    fun write(dst: OutputStream, format: String, quality: Float = 0.9f) {
        writeImageImpl.write(this, dst, format, quality)
    }

    override fun destroy() {}

    /**
     * for debugging and easier seeing pixels: nearest filtering, no interpolation
     * */
    open fun scaleUp(sx: Int, sy: Int = sx): Image {
        return ScaledImage(this, sx, sy)
    }

    open fun split(numTilesX: Int, numTilesY: Int): List<Image> {
        return List(numTilesX * numTilesY) { tileId ->
            val ix = tileId % numTilesX
            val iy = tileId / numTilesX
            val x0 = WorkSplitter.partition(ix, width, numTilesX)
            val x1 = WorkSplitter.partition(ix + 1, width, numTilesX)
            val y0 = WorkSplitter.partition(iy, height, numTilesY)
            val y1 = WorkSplitter.partition(iy + 1, height, numTilesY)
            cropped(x0, y0, x1 - x0, y1 - y0)
        }
    }

    open fun cropped(x0: Int, y0: Int, w0: Int, h0: Int): Image {
        return CroppedImage(this, x0, y0, w0, h0)
    }

    /**
     * execution order isn't guaranteed!
     * */
    fun forEachPixel(callback: I2U) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                callback.call(x, y)
            }
        }
    }

    fun flipY() {
        offset = getIndex(0, height - 1)
        stride = -stride
    }

    var ref: FileReference = InvalidRef
        get() {
            if (field == InvalidRef) {
                field = InnerTmpImageFile(this)
            }
            return field
        }

    fun sizeGuess(): Long {
        return width.toLong() * height.toLong() * numChannels.toLong()
    }

    companion object {

        @InternalAPI
        var writeImageImpl = ImageStreamWriter { img, out, format, _ ->
            if ("hdr".equals(format, true)) {
                writeHDR(img, out)
            } else {
                out.write(BMPWriter.createBMP(img))
            }
        }

        fun writeHDR(image: Image, output: OutputStream) {
            if (image is IFloatImage && image.numChannels >= 3) {
                val floatImage = image.toFloatImage(false)
                HDRWriter.writeHDR(
                    floatImage.width, floatImage.height, floatImage.stride,
                    floatImage.data, output
                )
            } else {
                output.write(BMPWriter.createBMP(image))
            }
        }
    }
}