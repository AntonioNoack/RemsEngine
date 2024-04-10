package me.anno.image.thumbs

import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.graph.hdb.HDBKey
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.utils.Color
import me.anno.utils.InternalAPI
import me.anno.utils.Sleep
import me.anno.utils.Warning
import me.anno.utils.structures.Callback
import me.anno.utils.structures.Iterators.subList

@InternalAPI
object TextThumbnails {

    @JvmStatic
    @InternalAPI
    fun register() {
        Thumbs.registerSignature("ttf", TextThumbnails::generateFontPreview)
        Thumbs.registerSignature("woff1", TextThumbnails::generateFontPreview)
        Thumbs.registerSignature("woff2", TextThumbnails::generateFontPreview)
        Thumbs.registerExtension("txt", TextThumbnails::generateTextImage)
        Thumbs.registerExtension("html", TextThumbnails::generateTextImage)
        Thumbs.registerExtension("md", TextThumbnails::generateTextImage)
    }

    @JvmStatic
    private fun generateFontPreview(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        Warning.unused(dstFile)
        // generate font preview
        val text = "The quick\nbrown fox\njumps over\nthe lazy dog"
        val lineCount = 4
        val key = Font(srcFile.absolutePath, size * 0.7f / lineCount, isBold = false, isItalic = false)
        val texture = FontManager.getTexture(
            key, text, size * 2, size * 2, 0, false
        )
        if (texture != null) {
            Sleep.waitUntil(true) { texture.wasCreated }
            callback.ok(texture)
        } else callback.err(null)
    }

    @JvmStatic
    @InternalAPI
    fun generateTextImage(
        srcFile: FileReference,
        dstFile: HDBKey,
        size: Int,
        callback: Callback<ITexture2D>
    ) {
        Warning.unused(dstFile)
        // todo html preview???
        // todo markdown preview (?)
        // generate text preview
        // scale text with size?
        val maxLineCount = Maths.clamp(size / 24, 3, 40)
        val maxLineLength = maxLineCount * 5 / 2
        srcFile.readLines(maxLineLength) { itr, exc ->
            exc?.printStackTrace()
            if (itr != null) {
                var lines = itr
                    .subList(0, maxLineCount)
                    .toMutableList()
                if (itr.hasNext()/*lines.size > maxLineCount*/) {
                    lines = lines.subList(0, maxLineCount)
                    lines[lines.lastIndex] = "..."
                }
                itr.close()
                // remove empty lines at the end
                while (lines.isNotEmpty() && lines.last().isEmpty()) {
                    lines.removeAt(lines.lastIndex)
                }
                if (lines.isNotEmpty()) {
                    val length = lines.maxOf { it.length }
                    if (length > 0) {
                        val sx = DrawTexts.monospaceFont.sampleWidth
                        val sy = DrawTexts.monospaceFont.sizeInt
                        val w = (length + 1) * sx
                        val h = (lines.size + 1) * sy
                        GFX.addGPUTask("textThumbs", w, h) {
                            val transform = GFXx2D.transform
                            transform.identity().scale(1f, -1f, 1f)
                            val tex = Texture2D("textThumbs", w, h, 1)
                            tex.create(TargetType.UInt8x3)
                            GFXState.useFrame(tex, 0) {
                                val tc = Color.black
                                val bg = -1
                                it.clearColor(bg)
                                val x = sx.shr(1)
                                for (yi in lines.indices) {
                                    val line = lines[yi].trimEnd()
                                    if (line.isNotEmpty()) {
                                        val y = yi * sy + sy.shr(1)
                                        DrawTexts.drawSimpleTextCharByChar(
                                            x, y, 1, line, tc, bg
                                        )
                                    }
                                }
                            }
                            transform.identity()
                            callback.ok(tex)
                        }
                    }
                }
            }
        }
    }
}