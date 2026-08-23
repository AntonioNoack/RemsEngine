package me.anno.ui

import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom64
import me.anno.maths.geometry.SkylinePacking
import me.anno.utils.structures.arrays.IntArrayList
import org.lwjgl.opengl.GL11C.GL_RGB
import org.lwjgl.opengl.GL11C.GL_RGB8
import org.lwjgl.opengl.GL11C.GL_RGBA
import org.lwjgl.opengl.GL11C.GL_RGBA8
import org.lwjgl.opengl.GL11C.glCopyTexSubImage2D
import kotlin.math.min

class Canvas {

    var x0 = 0
    var x1 = 0
    var y0 = 0
    var y1 = 0

    val boundsStack = IntArrayList()

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

    fun clip2(x0: Int, y0: Int, x1: Int, y1: Int, child: Panel) {
        // from the bottom to the top
        val w = x1 - x0
        val h = y1 - y0
        GFX.check()
        if (w < 1 || h < 1) return
        // val height = RenderState.currentBuffer?.h ?: height
        // val realY = height - (y + h)
        push(x0, y0, x1, y1)
        child.draw(this)
        pop()
    }

    inline fun custom(render: () -> Unit) {
        finish()
        render()
        start()
    }

    fun start() {}

    fun finish() {}

    fun drawTexture(
        x: Int, y: Int, w: Int, h: Int,
        texture: ITexture2D, tint: Int = -1,
    ) {
        if (x >= x1 || y >= y1 || x + w <= x0 || y + h <= y0) return // invisible
        val bounds = getBounds(texture)
        if (bounds != null) {
            // todo ensure we have a slot
            // todo push values onto buffer
        } else custom {
            // check if we overlap the bounds
            val isSafe = x >= x0 && y >= y0 && x + w <= x1 && y + h <= y1
            if (isSafe) {
                DrawTextures.drawTexture(x, y, w, h, texture, tint)
            } else {
                useFrame(x0, y0, x1 - x0, y1 - y0) {
                    DrawTextures.drawTexture(x, y, w, h, texture, tint)
                }
            }
        }
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
            GL_RGB8, GL_RGBA8 -> true
            else -> false
        }
    }
}