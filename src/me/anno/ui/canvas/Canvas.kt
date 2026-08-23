package me.anno.ui.canvas

import me.anno.fonts.Font
import me.anno.fonts.FontImpl.Companion.heightLimitToMaxNumLines
import me.anno.fonts.FontManager
import me.anno.gpu.Clipping
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
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
import me.anno.gpu.drawing.DrawTexts.getOffset
import me.anno.gpu.drawing.DrawTexts.sizeLayoutHelper
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.gpu.drawing.GFXx2D.transform
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom64
import me.anno.maths.geometry.ShelfPacking
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.convertARGB2ABGR
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector2f
import org.lwjgl.opengl.GL11C.GL_RGB
import org.lwjgl.opengl.GL11C.GL_RGB8
import org.lwjgl.opengl.GL11C.GL_RGBA
import org.lwjgl.opengl.GL11C.GL_RGBA8
import org.lwjgl.opengl.GL11C.glCopyTexSubImage2D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

// todo somehow, many panels are no longer rendered, although they definitely should be!

class Canvas {

    companion object {

        private const val maxTexSize = 256
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

        private val shapeBuffer = StaticBuffer("canvas", attr, 4096, BufferUsage.DYNAMIC)
        private val textureCache = HashMap<ITexture2D, Bounds?>()
        private val storage by lazy {
            val size = min(GFX.maxTextureSize, 4096)
            Texture2D("canvasCache", size, size, 1)
        }

        private val texturePacking by lazy {
            ShelfPacking(storage.width, storage.height, 4)
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

    // todo an instanced buffer, where we store all draw operations
    // todo a super shader, which can render text and shapes
    // todo a cache, which stores all small textures in one big texture
    // todo big textures are drawn directly

    inline fun clip2(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) {
        if (x1 <= x0 || y1 <= y0) return
        push(x0, y0, x1, y1)
        render()
        pop()
    }

    fun drawClipped(x0: Int, y0: Int, x1: Int, y1: Int, panel: Panel) {
        if (x1 <= x0 || y1 <= y0) return
        push(x0, y0, x1, y1)
        panel.draw(this)
        pop()
    }

    inline fun custom(crossinline render: () -> Unit) {
        finish()
        Clipping.clip(x0, y0, dx, dy, render)
    }

    fun finish() {
        val nio = shapeBuffer.getOrCreateNioBuffer()
        if (nio.position() == 0) return

        shapeBuffer.cpuSideChanged()
        shapeBuffer.ensureBuffer()

        val fb = GFXState.framebuffer.last()
        val texture = storage.createdOrNull() ?: TextureLib.whiteTexture
        texture.bind(0)

        useFrame(0, 0, fb.width, fb.height, fb) {
            val shader = CanvasShader
            shader.use()
            if (fb is Framebuffer) shader.v2i("dstOffset", fb.offsetX, fb.offsetY)
            else shader.v2i("dstOffset", 0, 0)
            shader.v2f("invRenderSize", 1f / fb.width, 1f / fb.height)
            shader.v2f("invAtlasSize", 1f / storage.width, 1f / storage.height)
            shader.m4x4("transform", transform)
            flat01.drawInstanced(shader, shapeBuffer)
        }

        nio.position(0)
        nio.limit(nio.capacity())
    }

    fun pushBounds(nio: ByteBuffer, x: Int, y: Int, width: Int, height: Int) {
        nio.putShort(x.toShort())
        nio.putShort(y.toShort())
        nio.putShort((x + width).toShort())
        nio.putShort((y + height).toShort())
    }

    fun pushScissor(nio: ByteBuffer) {
        nio.putShort(x0.toShort())
        nio.putShort(y0.toShort())
        nio.putShort(x1.toShort())
        nio.putShort(y1.toShort())
    }

    fun pushTexBounds(nio: ByteBuffer, bounds: Bounds) {
        // texBounds
        nio.putShort(bounds.x0.toShort())
        nio.putShort(bounds.y0.toShort())
        nio.putShort(bounds.x1.toShort())
        nio.putShort(bounds.y1.toShort())
    }

    private fun skipTexBounds(nio: ByteBuffer) {
        nio.putLong(0)
    }

    fun pushColor(nio: ByteBuffer, color: Int) {
        if (isLittleEndian) {
            // can be optimized, because we know it is Little Endian
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

    fun isFinished() = shapeBuffer.getOrCreateNioBuffer().position() == 0

    fun getNioBuffer(): ByteBuffer {
        val nio = shapeBuffer.getOrCreateNioBuffer()
        if (nio.position() == nio.capacity()) finish() // create some space for us
        check(nio.position() < nio.capacity())
        return nio
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, tint: Int = -1,
    ) = drawTexture(x, y, w, h, texture, false, tint)

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean, tint: Int = -1,
    ) {
        val minX = min(x, x + w)
        val maxX = max(x, x + w)
        val minY = min(y, y + h)
        val maxY = max(y, y + h)
        if (minX >= x1 || minY >= y1 || maxX <= x0 || maxY <= y0) return // invisible

        val bounds = getBounds(texture)
        // println("Bounds for texture $texture at $x,$y += $w,$h: $bounds")
        if (bounds != null) {

            val nio = getNioBuffer()
            val mode = if (ignoreAlpha) CanvasDrawMode.TEXTURE_NO_ALPHA else CanvasDrawMode.TEXTURE
            pushBounds(nio, x, y, w, h)
            pushScissor(nio)
            pushTexBounds(nio, bounds)
            pushColor(nio, tint)
            pushColor(nio, 0)
            pushMode(nio, mode)

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

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int) {

        val minX = min(x, x + w)
        val maxX = max(x, x + w)
        val minY = min(y, y + h)
        val maxY = max(y, y + h)
        if (minX >= x1 || minY >= y1 || maxX <= x0 || maxY <= y0) return // invisible

        val nio = getNioBuffer()
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

    fun fill(color: Int) {
        drawRect(x0, y0, dx, dy, color)
    }

    fun getBounds(texture: ITexture2D): Bounds? {
        if (!texture.isCreated()) return null
        val w = texture.width
        val h = texture.height
        if (w <= 0 || h <= 0 || w > maxTexSize || h > maxTexSize) return null
        return textureCache.getOrPut(texture) { insert(texture, w, h) }
    }

    private fun clearCache() {
        texturePacking.clear()
        textureCache.clear()
    }

    private fun insert(texture: ITexture2D, w: Int, h: Int): Bounds {
        var pos = texturePacking.allocate(w, h)
        if (pos < 0) {
            // if cache is full, reset
            finish()
            clearCache()
            pos = texturePacking.allocate(w, h)
            println("Inserted $texture at ${unpackHighFrom64(pos)},${unpackLowFrom64(pos)}")
            check(pos >= 0)
        }

        val x = unpackHighFrom64(pos)
        val y = unpackLowFrom64(pos)
        return insertAt(texture, x, y, w, h)
    }

    private fun insertAt(source: ITexture2D, x: Int, y: Int, w: Int, h: Int): Bounds {
        if (!storage.isCreated()) storage.create(TargetType.UInt8x4)

        // use glCopyTexSubImage2D, if the formats are compatible
        if (source is Texture2D && hasCompatibleFormat(source.internalFormat)) {
            useFrame(source) {
                // copies framebuffer to texture; first coords are texture, second are framebuffer
                storage.bind(0)
                glCopyTexSubImage2D(storage.target, 0, x, y, 0, 0, w, h)
            }
        } else {
            useFrame(storage) {
                GFXState.blendMode.use(null) {
                    GFXState.depthMode.use(DepthMode.ALWAYS) {
                        DrawTextures.drawTexture(x, y, w, h, source)
                    }
                }
            }
        }
        return Bounds(x, y, x + w, y + h)
    }

    private fun hasCompatibleFormat(format: Int): Boolean {
        return when (format) {
            GL_RGB, GL_RGBA,
            GL_RGB8, GL_RGBA8,
                -> true
            else -> false
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
    ): Int {

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

}
