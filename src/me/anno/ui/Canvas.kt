package me.anno.ui

import me.anno.fonts.Font
import me.anno.gpu.Clipping
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.DefaultFonts.monospaceFont
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom64
import me.anno.maths.geometry.SkylinePacking
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.FrameTimings
import me.anno.utils.structures.arrays.IntArrayList
import org.lwjgl.opengl.GL11C.GL_RGB
import org.lwjgl.opengl.GL11C.GL_RGB8
import org.lwjgl.opengl.GL11C.GL_RGBA
import org.lwjgl.opengl.GL11C.GL_RGBA8
import org.lwjgl.opengl.GL11C.glCopyTexSubImage2D
import kotlin.math.min

class Canvas {

    companion object {
        private const val maxTexSize = 64
        private val attr = bind(
            Attribute("bounds", AttributeType.UINT16, 4), // 8 bytes, where to draw
            Attribute("scissor", AttributeType.UINT16, 4), // 8 bytes, where to clip
            Attribute("texBounds", AttributeType.UINT16, 4), // 8 bytes,
            Attribute("tint", AttributeType.UINT8_NORM, 4), // 4 bytes,
            Attribute("mode", AttributeType.UINT32, 1), // 4 bytes -> total 32 bytes (nice!)
            // todo we need a corner-radius and corner-flags(?)...
        )

        class TexBounds(val x0: Int, val x1: Int, val y0: Int, val y1: Int)

        private val cache = HashMap<ITexture2D, TexBounds?>()
        private val storage by lazy {
            val size = min(GFX.maxTextureSize, 4096)
            Texture2D("canvasCache", size, size, 1)
        }

        private val packing by lazy {
            SkylinePacking(storage.width, storage.height, 4)
        }
    }

    enum class DrawMode {
        RECTANGLE,
        TEXTURE,
        TEXTURE_NO_ALPHA,
        CIRCLE,
        SQUIRCLE,
        ROUNDED_RECT,
        TEXT,
        LINE
    }

    var x0 = 0
    var x1 = 0
    var y0 = 0
    var y1 = 0

    val dx get() = x1 - x0
    val dy get() = y1 - y0

    val boundsStack = IntArrayList()
    val drawData = IntArrayList()

    fun push(x0: Int, y0: Int, x1: Int, y1: Int) {
        boundsStack.ensureExtra(4)
        boundsStack.addUnsafe(x0)
        boundsStack.addUnsafe(y0)
        boundsStack.addUnsafe(x1)
        boundsStack.addUnsafe(y1)
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

    val buffer = StaticBuffer("canvas", attr, 1024, BufferUsage.DYNAMIC)

    // todo an instanced buffer, where we store all draw operations
    // todo a super shader, which can render text and shapes
    // todo a cache, which stores all small textures in one big texture
    // todo big textures are drawn directly

    fun clip2(x0: Int, y0: Int, x1: Int, y1: Int, render: () -> Unit) {
        // from the bottom to the top
        val w = x1 - x0
        val h = y1 - y0
        GFX.check()
        if (w < 1 || h < 1) return
        // val height = RenderState.currentBuffer?.h ?: height
        // val realY = height - (y + h)
        push(x0, y0, x1, y1)
        render()
        pop()
    }

    fun drawClipped(x0: Int, y0: Int, x1: Int, y1: Int, panel: Panel) {
        // from the bottom to the top
        val w = x1 - x0
        val h = y1 - y0
        GFX.check()
        if (w < 1 || h < 1) return
        // val height = RenderState.currentBuffer?.h ?: height
        // val realY = height - (y + h)
        push(x0, y0, x1, y1)
        panel.draw(this)
        pop()
    }

    inline fun custom(crossinline render: () -> Unit) {
        finish()
        Clipping.clip(x0, y0, dx, dy, render)
    }

    fun finish() {
        if (drawData.isEmpty()) return
        TODO()
    }

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, tint: Int = -1,
    ) = drawTexture(x, y, w, h, texture, false, tint)

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, ignoreAlpha: Boolean, tint: Int = -1,
    ) {
        if (x >= x1 || y >= y1 || x + w <= x0 || y + h <= y0) return // invisible
        val bounds = getBounds(texture)
        if (bounds != null) {
            // todo ensure we have a slot
            // todo push values onto buffer
            TODO()
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
        drawTexture(x, y, w, h, TextureLib.whiteTexture, color)
    }

    fun fill(color: Int) {
        drawTexture(x0, y0, dx, dy, TextureLib.whiteTexture, color)
    }

    private fun getBounds(texture: ITexture2D): TexBounds? {
        if (!texture.isCreated()) return null
        val w = texture.width
        val h = texture.height
        if (w <= 0 || h <= 0 || w > maxTexSize || h > maxTexSize) return null
        return cache.getOrPut(texture) { insert(texture, w, h) }
    }

    private fun insert(texture: ITexture2D, w: Int, h: Int): TexBounds {
        var pos = packing.allocate(w, h)
        if (pos < 0) {
            // if cache is full, reset
            finish()
            packing.clear()
            pos = packing.allocate(w, h)
            check(pos >= 0)
        }

        val x = unpackHighFrom64(pos)
        val y = unpackLowFrom64(pos)
        return insertAt(texture, x, y, w, h)
    }

    private fun insertAt(source: ITexture2D, x: Int, y: Int, w: Int, h: Int): TexBounds {
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
        return TexBounds(x, y, x + w, y + h)
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
    ): Int = DrawTexts.drawText(
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
    ): Int = DrawTexts.drawText(
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
    ): Int = DrawTexts.drawText(
        x, y, padding, font, text,
        color, backgroundColor,
        -1, -1, alignX, alignY
    )

    fun drawText(
        x: Int, y: Int,
        padding: Int, text: CharSequence,
    ): Int = DrawTexts.drawText(
        x, y, padding, monospaceFont, text,
        FrameTimings.textColor,
        FrameTimings.backgroundColor,
    )

    fun drawText(
        x: Int, y: Int,
        font: Font, text: CharSequence,
        color: Int, backgroundColor: Int,
        widthLimit: Int, heightLimit: Int,
        alignX: AxisAlignment = AxisAlignment.MIN,
        alignY: AxisAlignment = AxisAlignment.MIN,
    ): Int {
        var res = 0
        custom {
            // todo support this properly
            res = DrawTexts.drawText(
                x, y, 0, font, text,
                color, backgroundColor,
                widthLimit, heightLimit,
                alignX, alignY
            )
        }
        return res
    }

    fun drawCircle(
        x: Float, y: Float,
        radiusX: Float, radiusY: Float, innerRadius: Float,
        startDegrees: Float, endDegrees: Float,
        color: Int,
    ) {
        custom {
            // todo support this properly
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
            // todo support this properly
            DrawCurves.drawLine(x0, y0, x1, y1, thickness, color, background, flatEnds, smoothness)
        }
    }

}