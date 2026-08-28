package me.anno.ui.canvas

import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths.clamp
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom64
import me.anno.maths.geometry.ShelfPacking
import me.anno.video.formats.gpu.GPUFrame
import org.lwjgl.opengl.GL11C.GL_RGB
import org.lwjgl.opengl.GL11C.GL_RGB8
import org.lwjgl.opengl.GL11C.GL_RGBA
import org.lwjgl.opengl.GL11C.GL_RGBA8
import org.lwjgl.opengl.GL11C.glCopyTexSubImage2D

object CanvasAtlasCache {

    private const val maxTexSize = 256

    val atlas by lazy {
        Atlas(clamp(GFX.maxTextureSize, maxTexSize, 4096))
    }

    class Atlas(val size: Int) {
        val texture = Texture2D("canvasAtlas", size, size, 1)
        val packing = ShelfPacking(size, size, 4)
    }

    private val textureCache = HashMap<ITexture2D, Bounds?>()
    private val frameCache = HashMap<GPUFrame, Bounds?>()

    fun getBounds(canvas: Canvas, texture: ITexture2D): Bounds? {
        if (!texture.isCreated()) return null
        val w = texture.width
        val h = texture.height
        if (w <= 0 || h <= 0 || w > maxTexSize || h > maxTexSize) return null
        return textureCache.getOrPut(texture) { insert(canvas, texture, w, h) }
    }

    fun getBounds(canvas: Canvas, frame: GPUFrame): Bounds? {
        if (!frame.isCreated) return null
        val w = frame.width
        val h = frame.height
        if (w <= 0 || h <= 0 || w > maxTexSize || h > maxTexSize) return null
        return frameCache.getOrPut(frame) { insert(canvas, frame, w, h) }
    }

    private fun clearCache() {
        atlas.packing.clear()
        textureCache.clear()
        frameCache.clear()
    }

    private fun insert(canvas: Canvas, texture: ITexture2D, w: Int, h: Int): Bounds {
        val pos = getInsertPos(canvas, w, h)
        val x = unpackHighFrom64(pos)
        val y = unpackLowFrom64(pos)
        return insertAt(texture, x, y, w, h)
    }

    private fun insert(canvas: Canvas, texture: GPUFrame, w: Int, h: Int): Bounds {
        val pos = getInsertPos(canvas, w, h)
        val x = unpackHighFrom64(pos)
        val y = unpackLowFrom64(pos)
        return insertAt(texture, x, y, w, h)
    }

    private fun getInsertPos(canvas: Canvas, w: Int, h: Int): Long {
        var pos = atlas.packing.allocate(w, h)
        if (pos < 0) {
            // if cache is full, reset
            canvas.finish()
            clearCache()
            pos = atlas.packing.allocate(w, h)
            check(pos >= 0)
        }
        return pos
    }

    private fun ensureGetAtlas(): Texture2D {
        val atlasTexture = atlas.texture
        if (!atlasTexture.isCreated()) atlasTexture.create(TargetType.UInt8x4)
        return atlasTexture
    }

    private fun insertAt(source: ITexture2D, x: Int, y: Int, w: Int, h: Int): Bounds {
        val atlasTexture = ensureGetAtlas()

        // use glCopyTexSubImage2D, if the formats are compatible
        if (source is Texture2D && hasCompatibleFormat(source.internalFormat)) {
            useFrame(source) {
                // copies framebuffer to texture; first coords are texture, second are framebuffer
                atlasTexture.bind(0)
                glCopyTexSubImage2D(atlasTexture.target, 0, x, y, 0, 0, w, h)
            }
        } else {
            useFrame(atlasTexture) {
                GFXState.blendMode.use(null) {
                    GFXState.depthMode.use(DepthMode.ALWAYS) {
                        DrawTextures.drawTexture(x, y, w, h, source)
                    }
                }
            }
        }
        return createBounds(x, y, w, h)
    }

    private fun insertAt(source: GPUFrame, x: Int, y: Int, w: Int, h: Int): Bounds {
        val atlasTexture = ensureGetAtlas()
        useFrame(atlasTexture) {
            GFXState.blendMode.use(null) {
                GFXState.depthMode.use(DepthMode.ALWAYS) {
                    DrawTextures.drawTexture(x, y, w, h, source)
                }
            }
        }
        return createBounds(x, y, w, h)
    }

    private fun createBounds(x: Int, y: Int, w: Int, h: Int): Bounds {
        return Bounds(x.toShort(), y.toShort(), (x + w).toShort(), (y + h).toShort())
    }

    private fun hasCompatibleFormat(format: Int): Boolean {
        // todo how does RGB work? what is the alpha-value then?
        return when (format) {
            GL_RGB, GL_RGBA,
            GL_RGB8, GL_RGBA8,
                -> true
            else -> false
        }
    }

}