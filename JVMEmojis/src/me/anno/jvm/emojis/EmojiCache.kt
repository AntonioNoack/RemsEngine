package me.anno.jvm.emojis

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.fonts.IEmojiCache
import me.anno.fonts.signeddistfields.Contours
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.renderer.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib
import me.anno.image.Image
import me.anno.image.svg.DrawSVGs
import me.anno.image.svg.SVGBuffer
import me.anno.image.svg.SVGMesh
import me.anno.image.svg.SVGMeshCache
import me.anno.maths.Packing
import me.anno.maths.Packing.unpackHighFrom64
import me.anno.maths.Packing.unpackLowFrom64
import me.anno.utils.Color
import me.anno.utils.OS.res
import me.anno.utils.types.Floats.toIntOr
import org.joml.Matrix4fArrayList
import speiger.primitivecollections.IntHashSet
import speiger.primitivecollections.ObjectToLongHashMap
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.math.ceil
import kotlin.math.max

// todo integrate this into text texture generation

object EmojiCache : IEmojiCache {

    private data class EmojiKey(val path: List<Int>, val size: Int)

    private val rasterTimeoutMillis = 10_000L
    private val meshTimeoutMillis = 10_000L

    private val meshCache = CacheSection<List<Int>, SVGMesh>("EmojiMeshCache")
    private val rasterCache = CacheSection<EmojiKey, Image>("EmojiRasterCache")
    private val contourCache = CacheSection<EmojiKey, Contours>("EmojiContourCache")

    private val knownEmojis = ObjectToLongHashMap<List<Int>>(0)
    private val knownEmojiChars = IntHashSet()
    private val svgEmojiBytes: ByteArray

    init {
        val zipBytes = res.getChild("twemoji.zip").inputStreamSync()
        val zis = ZipInputStream(zipBytes)
        var size = 0
        var tmpRawBytes = ByteArray(1 shl 22)
        while (true) {
            val entry = zis.nextEntry ?: break
            val name = entry.name
            if (!name.endsWith(".svg")) continue

            val path = name.dropLast(4)
                .split('-')
                .map { it.toInt(16) }

            val startIndex = size
            while (true) {
                val remaining = tmpRawBytes.size - size
                if (remaining < 1024) {
                    // ensure we have enough space
                    tmpRawBytes = tmpRawBytes.copyOf(tmpRawBytes.size * 2)
                } else {
                    val n = zis.read(tmpRawBytes, size, remaining)
                    if (n < 0) break
                    size += n
                }
            }

            // todo support [30-39]-20e3 somehow...
            if (path.size == 1) knownEmojiChars.add(path[0])
            knownEmojis[path] = Packing.pack64(startIndex, size)
        }
        svgEmojiBytes = tmpRawBytes
    }

    override fun contains(codepoint: Int): Boolean {
        return knownEmojiChars.contains(codepoint)
    }

    override fun contains(codepoints: List<Int>): Boolean {
        return knownEmojis.containsKey(codepoints)
    }

    override fun getEmojiImage(codepoints: List<Int>, fontSize: Int): AsyncCacheData<Image> {
        return rasterCache.getEntry(EmojiKey(codepoints, fontSize), rasterTimeoutMillis) { key, result ->
            getEmojiMesh(key.path).waitFor { svgMesh ->
                val buffer0 = svgMesh?.buffer?.value
                if (svgMesh != null && buffer0 != null) {
                    val buffer = SVGBuffer(svgMesh.bounds, buffer0)
                    addGPUTask("Render SVG", key.size, key.size) {
                        result.value = renderSVGMesh(buffer, key.size)
                    }
                } else result.value = null
            }
        }
    }

    private fun renderSVGMesh(mesh: SVGBuffer, size: Int): Image {
        val bounds = mesh.bounds
        val maxSize = max(bounds.maxX, bounds.maxY)
        val w = ceil(size * bounds.maxX / maxSize).toIntOr()
        val h = ceil(size * bounds.maxY / maxSize).toIntOr()

        val fb = FBStack["Emojis", w, h, 4, false, 4, DepthBufferType.NONE] as Framebuffer

        GFX.check()

        GFXState.renderPurely {
            GFXState.useFrame(w, h, false, fb, colorRenderer) {
                fb.clearColor(0)
                GFXState.blendMode.use(BlendMode.DEFAULT) {
                    val transform = Matrix4fArrayList()
                    transform.scale(bounds.maxY / bounds.maxX, -1f, 1f)
                    DrawSVGs.draw3DSVG(
                        transform, mesh,
                        TextureLib.whiteTexture, Color.white4,
                        Filtering.NEAREST, TextureLib.whiteTexture.clamping,
                        null
                    )
                }
            }
        }

        GFX.check()

        return fb.createImage(flipY = false, withAlpha = true)!!
    }

    fun getEmojiMesh(codepoints: List<Int>): AsyncCacheData<SVGMesh> {
        return meshCache.getEntry(codepoints, meshTimeoutMillis) { key, result ->
            val dataBounds = knownEmojis[key]
            val i0 = unpackHighFrom64(dataBounds)
            val i1 = unpackLowFrom64(dataBounds)
            val stream = ByteArrayInputStream(svgEmojiBytes, i0, i1 - i0)
            result.value = SVGMeshCache.loadSVGMeshSync(stream)
        }
    }

    override fun getEmojiContour(codepoints: List<Int>, fontSize: Int): AsyncCacheData<Contours> {
        if (!contains(codepoints)) return AsyncCacheData.empty() // fast-path
        return contourCache.getEntry(EmojiKey(codepoints, fontSize), rasterTimeoutMillis) { key, result ->
            getEmojiMesh(key.path).waitFor { svgMesh ->
                val fontSizeF = fontSize.toFloat()
                result.value = if (svgMesh != null) {
                    val contours = svgMesh.getTransformedContours(
                        fontSizeF * 0.05f, -0.82f * fontSizeF,
                        fontSizeF
                    )
                    Contours(contours, false)
                } else null
            }
        }
    }
}