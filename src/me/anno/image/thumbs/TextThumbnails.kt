package me.anno.image.thumbs

import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.gpu.GFXState
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.graph.hdb.HDBKey
import me.anno.io.files.FileReference
import me.anno.io.files.ReadLineIterator
import me.anno.maths.Maths
import me.anno.utils.Color
import me.anno.utils.InternalAPI
import me.anno.utils.Sleep
import me.anno.utils.Warning
import me.anno.utils.async.Callback
import me.anno.utils.structures.Iterators.subList

@InternalAPI
object TextThumbnails {

    @JvmStatic
    @InternalAPI
    fun register() {
        Thumbs.registerSignatures("ttf", TextThumbnails::generateFontPreview)
        Thumbs.registerSignatures("woff1", TextThumbnails::generateFontPreview)
        Thumbs.registerSignatures("woff2", TextThumbnails::generateFontPreview)
        Thumbs.registerFileExtensions("txt", TextThumbnails::generateTextImage)
        Thumbs.registerFileExtensions("html", TextThumbnails::generateTextImage)
        Thumbs.registerFileExtensions("md", TextThumbnails::generateTextImage)
    }

    @JvmStatic
    private fun generateFontPreview(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        Warning.unused(dstFile)
        // generate font preview
        val text = "The quick\nbrown fox\njumps over\nthe lazy dog"
        val lineCount = 4
        val key = Font(srcFile.absolutePath, size * 0.7f / lineCount, isBold = false, isItalic = false)
        Sleep.waitUntilDefined(canBeKilled = true, {
            FontManager.getTexture(key, text, size * 2, size * 2, 0, false)
        }) { texture -> callback.ok(texture) }
    }

    @JvmStatic
    @InternalAPI
    fun generateTextImage(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
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
            val lines = getLines(itr, maxLineCount)
            val length = lines.maxOfOrNull { it.length } ?: 0
            if (length > 0) {
                val sx = DrawTexts.monospaceFont.sampleWidth
                val sy = DrawTexts.monospaceFont.sizeInt
                val w = (length + 1) * sx
                val h = (lines.size + 1) * sy
                addGPUTask("textThumbs", w, h) {
                    generateImage(lines, w, h, sx, sy, callback)
                }
            }
        }
    }

    private fun generateImage(lines: List<String>, w: Int, h: Int, sx: Int, sy: Int, callback: Callback<ITexture2D>) {
        val tex = Texture2D("textThumbs", w, h, 1)
        tex.create(TargetType.UInt8x3)
        GFXState.useFrame(tex) {
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
        callback.ok(tex)
    }

    private fun getLines(itr: ReadLineIterator?, maxLineCount: Int): List<String> {
        if (itr == null) return emptyList()
        val lines = itr.subList(0, maxLineCount)
        if (itr.hasNext()) {
            lines[lines.lastIndex] = "..."
        }
        itr.close()
        // remove empty lines at the end
        while (lines.isNotEmpty() && lines.last().isEmpty()) {
            lines.removeAt(lines.lastIndex)
        }
        return lines
    }
}