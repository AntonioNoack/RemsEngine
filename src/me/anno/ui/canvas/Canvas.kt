package me.anno.ui.canvas

import me.anno.fonts.Font
import me.anno.fonts.FontImpl.Companion.heightLimitToMaxNumLines
import me.anno.fonts.FontManager
import me.anno.gpu.Clipping
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawRounded
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.getOffset
import me.anno.gpu.drawing.DrawTexts.sizeLayoutHelper
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.transform
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.canvas.CanvasAtlasCache.atlas
import me.anno.ui.canvas.CanvasAtlasCache.getBounds
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.convertARGB2ABGR
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.GrowingList
import me.anno.video.formats.gpu.GPUFrame
import org.joml.Vector2f
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Converts many small draw calls into bigger ones for better performance.
 * This assumes a strict order text > texture > background.
 * This must only be used if that order is valid. Otherwise, call .custom {} and call the draw methods directly.
 *
 * Structure:
 * - an instanced buffer, where we store all draw operations
 * - a super shader, which can render text, textures and rectangles (maybe circles and lines in the future)
 * - a cache, which stores all small textures in one big texture
 * - big textures are drawn directly
 * */
class Canvas {

    companion object {

        const val RECT_ORDER = 0
        const val TEXTURE_ORDER = 1
        const val TEXT_ORDER = 2

        private val isLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN

        private val attr = bind(
            Attribute("instBounds", AttributeType.UINT16, 4), // 8 bytes, where to draw
            Attribute("instScissor", AttributeType.UINT16, 4), // 8 bytes, where to clip
            Attribute("instTexBounds", AttributeType.UINT16, 4), // 8 bytes,
            Attribute("instFgColor", AttributeType.UINT8_NORM, 4), // 4 bytes,
            Attribute("instBgColor", AttributeType.UINT8_NORM, 4), // 4 bytes,
            Attribute("instMode", AttributeType.UINT32, 1), // 4 bytes -> total 32 bytes (nice!)
            // todo we need a corner-radius and corner-flags(?)...
        )

        // todo setting vertexCount to 1 fixes all issues, so the issue definitely is dependencies...
        //  with higher values we also get duplicated renders... how??
        //  and some too-big-faces <- I can only explain those by corrupted data or if the wrong framebuffer is used

        // todo define a VAO for each buffer, so we can bind them quicker
        private fun createBuffer() = StaticBuffer("canvas", attr, 128, BufferUsage.STREAM)
        private val shapeBuffers = GrowingList { createBuffer() }
        private val vaos = GrowingList { index ->
            FixedAttributeBinding(flat01, shapeBuffers[index], CanvasShader)
        }

        private val flat01 = SimpleBuffer(
            "flat01", listOf(
                Vector2f(0f, 0f),
                Vector2f(0f, 1f),
                Vector2f(1f, 1f),
                Vector2f(1f, 0f)
            ), intArrayOf(0, 1, 2, 0, 2, 3), "positions"
        )
    }

    var x0 = 0
    var x1 = 0
    var y0 = 0
    var y1 = 0

    val dx get() = x1 - x0
    val dy get() = y1 - y0

    val boundsStack = IntArrayList()
    val depth: Int get() = boundsStack.size shr 2

    lateinit var framebuffer: IFramebuffer

    fun define(framebuffer: IFramebuffer) {
        this.framebuffer = framebuffer
    }

    fun push(x0: Int, y0: Int, x1: Int, y1: Int) {
        boundsStack.ensureExtra(4)
        boundsStack.addUnsafe(this.x0)
        boundsStack.addUnsafe(this.y0)
        boundsStack.addUnsafe(this.x1)
        boundsStack.addUnsafe(this.y1)
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
    }

    fun pop() {
        check(boundsStack.size >= 4)
        y1 = boundsStack.removeLast()
        x1 = boundsStack.removeLast()
        y0 = boundsStack.removeLast()
        x0 = boundsStack.removeLast()
    }

    inline fun clip2(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) {
        if (x1 <= x0 || y1 <= y0) return
        push(x0, y0, x1, y1)
        render()
        pop()
    }

    inline fun clip2Dual(
        x0: Int, y0: Int, x1: Int, y1: Int,
        x2: Int, y2: Int, x3: Int, y3: Int,
        crossinline render: () -> Unit,
    ) = clip2(
        max(x0, x2), max(y0, y2),
        min(x1, x3), min(y1, y3),
        render
    )

    fun drawClipped(x0: Int, y0: Int, x1: Int, y1: Int, panel: Panel) {
        if (x1 <= x0 || y1 <= y0) return
        push(x0, y0, x1, y1)
        panel.draw(this)
        pop()
    }

    /**
     * sometimes custom is the background:
     *   add a parameter for which depths to render, and we only render until that depth
     * */
    inline fun custom(crossinline render: () -> Unit) {
        custom(10000, render)
    }

    inline fun custom(maxExtraDepth: Int, crossinline render: () -> Unit) {
        isInsideCustom = true
        finish(maxExtraDepth)
        Clipping.clip(x0, y0, dx, dy, render)
        isInsideCustom = false
    }

    fun finish(maxExtraDepth: Int = 10000) {
        val depth = min(depth + maxExtraDepth, shapeBuffers.size)
        if (isFinished(depth)) return

        val fb = framebuffer

        var dx = 0
        var dy = 0
        if (fb is Framebuffer) {
            dx = fb.offsetX
            dy = fb.offsetY
        }

        useFrame(dx, dy, fb.width, fb.height, fb) {
            val shader = CanvasShader
            shader.use()
            shader.v2i("dstOffset", dx, dy)
            shader.v2f("invRenderSize", 1f / fb.width, 1f / fb.height)
            shader.v1f("invAtlasSize", 1f / atlas.size)
            shader.m4x4("transform", transform)

            val texture = atlas.texture.createdOrNull() ?: TextureLib.whiteTexture
            texture.bind(0)

            for (i in 0 until depth) {
                val shapeBuffer = shapeBuffers[i]
                val nio = shapeBuffer.getOrCreateNioBuffer()
                if (nio.position() > 0) {
                    shapeBuffer.ensureBuffer()
                    val vao = vaos[i]
                    vao.bind()
                    vao.drawInstanced(flat01, shapeBuffer, flat01.drawMode)
                    shapeBuffer.clear()
                }
            }

            FixedAttributeBinding.unbind()
        }
    }

    var ctr = 0

    fun isFinished(): Boolean = isFinished(shapeBuffers.size)

    fun isFinished(depth: Int): Boolean {
        for (i in 0 until min(depth, shapeBuffers.size)) {
            val buffer = shapeBuffers[i]
            val nio = buffer.nioBuffer
            if (nio != null && nio.position() > 0) return false
        }
        return true
    }

    private fun pushBounds(nio: ByteBuffer, x: Int, y: Int, width: Int, height: Int) {
        nio.putShort(x.toShort())
        nio.putShort(y.toShort())
        nio.putShort((x + width).toShort())
        nio.putShort((y + height).toShort())
    }

    private fun pushScissor(nio: ByteBuffer) {
        check(x1 > x0) { "Must bind scissor bounds for canvas!" }
        check(y1 > y0)
        check(!isInsideCustom) { "Cannot use Canvas inside custom {}" }
        nio.putShort(x0.toShort())
        nio.putShort(y0.toShort())
        nio.putShort(x1.toShort())
        nio.putShort(y1.toShort())
    }

    private fun pushTexBounds(nio: ByteBuffer, bounds: Bounds) {
        // texBounds
        nio.putShort(bounds.x0.toShort())
        nio.putShort(bounds.y0.toShort())
        nio.putShort(bounds.x1.toShort())
        nio.putShort(bounds.y1.toShort())
    }

    private fun skipTexBounds(nio: ByteBuffer) {
        nio.putLong(0)
    }

    private fun pushColor(nio: ByteBuffer, color: Int) {
        if (isLittleEndian) {
            // optimized insert
            nio.putInt(convertARGB2ABGR(color))
        } else {
            nio.put(color.r().toByte())
            nio.put(color.g().toByte())
            nio.put(color.b().toByte())
            nio.put(color.a().toByte())
        }
    }

    fun pushMode(nio: ByteBuffer, mode: CanvasDrawMode) {
        nio.putInt(mode.ordinal)
        check(nio.position() % attr.stride == 0) // can be removed
    }

    fun getNioBuffer(extraDepth: Int): ByteBuffer {
        val nio = shapeBuffers[depth + extraDepth].getOrCreateNioBuffer()
        if (nio.position() == nio.capacity()) finish() // create some space for us
        check(nio.position() < nio.capacity())
        return nio
    }

    private fun pushTexture(
        x: Int, y: Int, w: Int, h: Int,
        bounds: Bounds, ignoreAlpha: Boolean, tint: Int,
        extraDepth: Int,
    ) {
        val nio = getNioBuffer(extraDepth)
        val mode = if (ignoreAlpha) CanvasDrawMode.TEXTURE_NO_ALPHA else CanvasDrawMode.TEXTURE
        pushBounds(nio, x, y, w, h)
        pushScissor(nio)
        pushTexBounds(nio, bounds)
        pushColor(nio, tint)
        pushColor(nio, 0)
        pushMode(nio, mode)
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, tint: Int = -1,
    ) = drawTexture(x, y, w, h, texture, false, tint)

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean, tint: Int = -1,
        extraDepth: Int = TEXTURE_ORDER,
    ) {
        if (!overlaps2(x, y, w, h) || tint.a() == 0) return // invisible

        val bounds = getBounds(this, texture)
        if (bounds != null) {
            pushTexture(x, y, w, h, bounds, ignoreAlpha, tint, extraDepth)
        } else custom {
            // check if we overlap the bounds
            val isSafe = x >= x0 && y >= y0 && x + w <= x1 && y + h <= y1
            if (isSafe) {
                DrawTextures.drawTexture(x, y, w, h, texture, ignoreAlpha, tint)
            } else {
                useFrame(x0, y0, x1 - x0, y1 - y0) {
                    DrawTextures.drawTexture(x, y, w, h, texture, ignoreAlpha, tint)
                }
            }
        }
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int, frame: GPUFrame,
        extraDepth: Int = TEXTURE_ORDER,
    ) {
        if (!overlaps2(x, y, w, h)) return // invisible

        val bounds = getBounds(this, frame)
        if (bounds != null) {
            pushTexture(x, y, w, h, bounds, ignoreAlpha = false, tint = -1, extraDepth)
        } else custom {
            // check if we overlap the bounds
            val isSafe = x >= x0 && y >= y0 && x + w <= x1 && y + h <= y1
            if (isSafe) {
                DrawTextures.drawTexture(x, y, w, h, frame)
            } else {
                useFrame(x0, y0, x1 - x0, y1 - y0) {
                    DrawTextures.drawTexture(x, y, w, h, frame)
                }
            }
        }
    }

    fun overlaps2(x: Int, y: Int, w: Int, h: Int): Boolean {
        return overlaps(x, y, x + w, y + h)
    }

    fun overlaps(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        val minX = min(x0, x1)
        val maxX = max(x0, x1)
        val minY = min(y0, y1)
        val maxY = max(y0, y1)
        return minX < x1 && minY < y1 &&
                maxX > x0 && maxY > y0
    }

    fun drawRect(
        x: Int, y: Int, w: Int, h: Int, color: Int,
        extraDepth: Int = RECT_ORDER,
    ) {
        if (!overlaps2(x, y, w, h) || color.a() == 0) return // invisible

        val nio = getNioBuffer(extraDepth)
        pushBounds(nio, x, y, w, h)
        pushScissor(nio)
        skipTexBounds(nio)
        pushColor(nio, color)
        pushColor(nio, 0)
        pushMode(nio, CanvasDrawMode.RECTANGLE)
    }

    fun drawRoundedRect(
        x: Int, y: Int, w: Int, h: Int,
        topRightRadius: Float, bottomRightRadius: Float,
        topLeftRadius: Float, bottomLeftRadius: Float,
        outlineThickness: Float,
        centerColor: Int, outlineColor: Int, backgroundColor: Int,
        smoothness: Float,
    ) {
        custom {
            // todo support this
            DrawRounded.drawRoundedRect(
                x, y, w, h, topRightRadius, bottomRightRadius, topLeftRadius, bottomLeftRadius,
                outlineThickness, centerColor, outlineColor, backgroundColor, smoothness
            )
        }
    }

    fun drawText(
        x: Int, y: Int,
        font: Font, text: CharSequence,
        color: Int, backgroundColor: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int = drawText(
        x, y, 0, font, text,
        color, backgroundColor,
        -1, -1, alignX, alignY
    )

    fun drawText(
        x: Int, y: Int,
        text: CharSequence,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int = drawText(x, y, 0, text, alignX, alignY)

    fun drawText(
        x: Int, y: Int, padding: Int,
        text: CharSequence,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int = drawText(
        x, y, padding, monospaceFont, text,
        FrameTimings.textColor, FrameTimings.backgroundColor,
        -1, -1, alignX, alignY
    )

    fun drawText(
        x: Int, y: Int, padding: Int,
        font: Font, text: CharSequence,
        color: Int, backgroundColor: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int = drawText(
        x, y, padding, font, text,
        color, backgroundColor,
        -1, -1, alignX, alignY
    )

    fun drawText(
        x: Int, y: Int,
        padding: Int, text: CharSequence,
    ): Int = drawText(
        x, y, padding, monospaceFont, text,
        FrameTimings.textColor,
        FrameTimings.backgroundColor,
    )

    fun drawText(
        x: Int, y: Int,
        font: Font, text: CharSequence,
        textColor: Int, backgroundColor: Int,
        widthLimit: Int, heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int = drawText(
        x, y, 0, font, text, textColor, backgroundColor,
        widthLimit, heightLimit, alignX, alignY
    )

    fun drawText(
        x: Int, y: Int, padding: Int,
        font: Font, text: CharSequence,
        textColor: Int, backgroundColor: Int,
        widthLimit: Int, heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
        extraDepth: Int = TEXT_ORDER,
    ): Int {

        // compute-shader fallback for true blending
        if (DrawTexts.enableTrueBlending && DrawTexts.canUseComputeShader()) {
            // todo why is this not used for 'tris, inst, ...'??
            finish()
            return DrawTexts.drawText(
                x, y, padding, font, text,
                textColor, backgroundColor, widthLimit, heightLimit, alignX, alignY
            )
        }

        this.extraDepth = extraDepth

        val sizeHelper = sizeLayoutHelper
        val fontImpl = FontManager.getFontImpl()
        val relativeWidthLimit = widthLimit / font.size
        val maxNumLines =
            if (heightLimit < 0) Int.MAX_VALUE
            else heightLimitToMaxNumLines(heightLimit, font.size, font.relativeLineSpacing)

        fontImpl.fillGlyphLayout(font, text, sizeHelper, relativeWidthLimit, maxNumLines)

        val totalWidth = sizeHelper.width
        val totalHeight = sizeHelper.height
        sizeHelper.clear()

        val xi = x + getOffset(totalWidth, alignX)
        val yi = y + getOffset(totalHeight, alignY)

        val drawHelper = CanvasTextDrawHelper
        drawHelper.font = font
        drawHelper.canvas = this
        drawHelper.lineHeight = font.lineSpacingI
        drawHelper.x = xi
        drawHelper.y = yi
        drawHelper.color = textColor
        drawHelper.bgColor = backgroundColor

        if (backgroundColor.a() != 0) {
            drawRect(xi - padding, yi - padding, totalWidth + 2 * padding, totalHeight + 2 * padding, backgroundColor)
        }

        GFX.loadTexturesSync.push(true)

        fontImpl.fillGlyphLayout(font, text, CanvasTextDrawHelper, relativeWidthLimit, maxNumLines)
        drawHelper.clear()

        GFX.loadTexturesSync.pop()

        return getSize(totalWidth, totalHeight)
    }

    fun drawCircle(
        x: Float, y: Float,
        radiusX: Float, radiusY: Float, innerRadius: Float,
        startDegrees: Float, endDegrees: Float,
        color: Int,
    ) {
        custom {
            // todo support this properly
            //  but we somehow need to encode startDegrees, endDegrees, innerRadius
            GFXx2D.drawCircle(x, y, radiusX, radiusY, innerRadius, startDegrees, endDegrees, color)
        }
    }

    fun drawLine(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        thickness: Float,
        color: Int, background: Int,
        flatEnds: Boolean,
        smoothness: Float = 1f,
    ) {
        custom {
            // todo support this properly:
            //  we need to encode thickness somehow
            //  the vertex shader needs to rotate the line
            DrawCurves.drawLine(x0, y0, x1, y1, thickness, color, background, flatEnds, smoothness)
        }
    }

    var isInsideCustom = false
    private var extraDepth = 0

    fun pushText(bounds: Bounds, x: Int, y: Int, w: Int, h: Int) {
        if (!overlaps2(x, y, w, h)) return

        val nio = getNioBuffer(extraDepth)
        pushBounds(nio, x, y, w, h)
        pushScissor(nio)
        pushTexBounds(nio, bounds)
        pushColor(nio, CanvasTextDrawHelper.color)
        pushColor(nio, CanvasTextDrawHelper.bgColor)
        pushMode(nio, CanvasDrawMode.TEXT)
    }
}
