package me.anno.ui.base.text

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.config.DefaultStyle.black
import me.anno.fonts.FontManager
import me.anno.fonts.TextGroup
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.smoothStep
import me.anno.maths.noise.FullNoise
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.style.Style
import me.anno.utils.Color.toRGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.*

/**
 * text panel with char-wise animation
 * */
open class AnimTextPanel(text: String, style: Style) : TextPanel(text, style) {

    class SimpleAnimTextPanel(
        text: String, style: Style,
        val animation: (SimpleAnimTextPanel, time: Float, index: Int, cx: Float, cy: Float) -> Int,
    ) : AnimTextPanel(text, style) {
        override fun animate(time: Float, index: Int, cx: Float, cy: Float): Int {
            return animation(this, time, index, cx, cy)
        }
    }

    open fun animate(time: Float, index: Int, cx: Float, cy: Float): Int {
        return textColor
    }

    var resetTransform = true
    var autoRedraw = true
    var lineSpacing = 0.5f
    var disableSubpixels = true
    var periodMillis = 24L * 3600L * 1000L // 1 day

    override fun tickUpdate() {
        super.tickUpdate()
        if (autoRedraw) invalidateDrawing()
    }

    override fun drawText(dx: Int, dy: Int, color: Int): Int {
        return drawText(dx, dy, text, color)
    }

    override fun drawText(dx: Int, dy: Int, text: String, color: Int): Int {
        val x = this.x + dx + padding.left
        val y = this.y + dy + padding.top

        val alignX = alignmentX
        val alignY = alignmentY
        val equalSpaced = useMonospaceCharacters

        if ('\n' in text) {
            var sizeX = 0
            val split = text.split('\n')
            val lineOffset = (font.size * (1f + lineSpacing)).roundToInt()
            for (index in split.indices) {
                val s = split[index]
                val size = drawText(dx, dy + index * lineOffset, s, color)
                sizeX = max(GFXx2D.getSizeX(size), sizeX)
            }
            return GFXx2D.getSize(sizeX, (split.size - 1) * lineOffset + font.sizeInt)
        }

        val time = (Engine.gameTime % max(1, periodMillis * MILLIS_TO_NANOS)) / 1e9f
        val shader = (if (disableSubpixels) ShaderLib.textShader else ShaderLib.subpixelCorrectTextShader).value
        shader.use()

        var h = font.sizeInt
        val totalWidth: Int

        GFX.loadTexturesSync.push(true)

        val transform = GFXx2D.transform
        val backup = JomlPools.mat4f.create()
        backup.set(transform)

        if (!disableSubpixels) shader.v4f("backgroundColor", backgroundColor)

        if (equalSpaced) {

            val charWidth = DrawTexts.getTextSizeX(font, "x", widthLimit, heightLimit)
            val textWidth = charWidth * text.length

            val dxi = DrawTexts.getOffset(textWidth, alignX)
            val dyi = DrawTexts.getOffset(font.sampleHeight, alignY)

            var fx = x + dxi
            val y2 = y + dyi

            var index = 0
            for (codepoint in text.codePoints()) {
                val txt = String(Character.toChars(codepoint))
                val size = FontManager.getSize(font, txt, -1, -1)
                h = GFXx2D.getSizeY(size)
                if (!txt.isBlank2()) {
                    val texture = FontManager.getTexture(font, txt, -1, -1)
                    if (texture != null && (texture !is Texture2D || texture.isCreated)) {
                        texture.bindTrulyNearest(0)
                        val x2 = fx + (charWidth - texture.w) / 2
                        if (resetTransform) transform.set(backup)
                        val color2 = animate(time, index, x2 + texture.w / 2f, y2 + texture.h / 2f)
                        shader.m4x4("transform", transform)
                        GFXx2D.posSize(shader, x2, y2, texture.w, texture.h)
                        if (disableSubpixels) shader.v4f("backgroundColor", color2 and 0xffffff)
                        shader.v4f("textColor", color2)
                        GFX.flat01.draw(shader)
                        GFX.check()
                    }
                }
                fx += charWidth
                index++
            }

            transform.set(backup)
            JomlPools.mat4f.sub(1)

            GFX.loadTexturesSync.pop()

            totalWidth = fx - (x + dxi)

        } else {

            val font2 = FontManager.getFont(font).font
            val group = TextGroup(font2, text, 0.0)

            val textWidth = group.offsets.last().toFloat()

            val dxi = DrawTexts.getOffset(textWidth.roundToInt(), alignX)
            val dyi = DrawTexts.getOffset(font.sampleHeight, alignY)

            val y2 = (y + dyi).toFloat()

            var index = 0
            for (codepoint in text.codePoints()) {
                val txt = String(Character.toChars(codepoint))
                val size = FontManager.getSize(font, txt, -1, -1)
                val o0 = group.offsets[index].toFloat()
                val o1 = group.offsets[index + 1].toFloat()
                val fx = x + dxi + o0
                val w = o1 - o0
                h = GFXx2D.getSizeY(size)
                if (!txt.isBlank2()) {
                    // todo in TextGroup, ti is broken ... why??
                    val texture = FontManager.getTexture(font, txt, -1, -1)
                    if (texture != null && (texture !is Texture2D || texture.isCreated)) {
                        texture.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
                        val x2 = fx + (w - texture.w) / 2
                        if (resetTransform) transform.set(backup)
                        val color2 = animate(time, index, x2 + texture.w / 2f, y2 + texture.h / 2f)
                        shader.m4x4("transform", transform)
                        GFXx2D.posSize(shader, x2, y2, texture.w.toFloat(), texture.h.toFloat())
                        if (disableSubpixels) shader.v4f("backgroundColor", color2 and 0xffffff)
                        shader.v4f("textColor", color2)
                        GFX.flat01.draw(shader)
                        GFX.check()
                    }
                }
                index++
            }

            totalWidth = textWidth.roundToInt()

        }

        transform.set(backup)
        JomlPools.mat4f.sub(1)

        GFX.loadTexturesSync.pop()

        return GFXx2D.getSize(totalWidth, h)

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeBoolean("resetTransform", resetTransform)
        writer.writeBoolean("autoRedraw", autoRedraw)
        writer.writeFloat("lineSpacing", lineSpacing)
        writer.writeBoolean("disableSubpixels", disableSubpixels)
        writer.writeLong("periodMillis", periodMillis)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "resetTransform" -> resetTransform = value
            "autoRedraw" -> autoRedraw = value
            "disableSubpixels" -> disableSubpixels = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        if (name == "lineSpacing") lineSpacing = value
        else super.readFloat(name, value)
    }

    override fun readLong(name: String, value: Long) {
        if (name == "periodMillis") periodMillis = value
        else super.readLong(name, value)
    }

    override val className = "AnimTextPanel"

    companion object {

        fun limitFps(time: Float, fps: Float) =
            round(time * fps) / fps

        /**
         * rotate the current letter around (cx,cy) by angleRadians radians
         * */
        fun rotate(angleRadians: Float, cx: Float, cy: Float) {
            val transform = GFXx2D.transform
            val px = -((cx - (GFX.viewportX + GFX.viewportWidth / 2)) / (GFX.viewportWidth)) * 2f
            val py = +((cy - (GFX.viewportY + GFX.viewportHeight / 2)) / (GFX.viewportHeight)) * 2f
            val a = GFX.viewportWidth.toFloat() / GFX.viewportHeight
            transform.translate(-px, -py, 0f)
            transform.scale(1f, a, 1f)
            transform.rotateZ(angleRadians)
            transform.scale(1f, 1f / a, 1f)
            transform.translate(+px, +py, 0f)
        }

        fun scale(sx: Float, sy: Float = sx) {
            GFXx2D.transform.scale(sx, sy, 1f)
        }

        fun translate(dx: Float, dy: Float) {
            GFXx2D.transform.translate(dx * 2f / GFX.viewportWidth, -dy * 2f / GFX.viewportHeight, 0f)
        }

        fun hsluv(angleRadians: Float, saturation: Float = 1f, luma: Float = 0.7f): Int {
            val vec = JomlPools.vec3f.borrow()
            return HSLuv.toRGB(vec.set(angleRadians / (2f * PIf), saturation, luma)).toRGB()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            // inspired by https://www.youtube.com/watch?v=3QXGM84ZfSw
            GFX.disableRenderDoc = true
            testUI {
                val list = PanelListY(style)
                val green = 0x8fbc8f or black
                val blue = 0x7777ff or black
                val fontSize = 50f
                val font = AnimTextPanel("", style).font
                    .withSize(fontSize)
                    .withBold(true)
                list.add(SimpleAnimTextPanel("Rainbow Text", style) { p, time, index, cx, cy ->
                    p.font = font
                    val s = time * 5f + index / 3f
                    translate(0f, sin(s) * 5f)
                    rotate(sin(s) * 0.1f, cx, cy)
                    hsluv(time * 2f - index / 2f)
                })
                list.add(SimpleAnimTextPanel("Growing Text", style) { p, time, index, _, _ ->
                    p.font = font
                    val growTime = 0.4f
                    val dissolveTime = 1.0f
                    val phase = time - index * 0.03f
                    val s = smoothStep((phase) / growTime)
                    translate(0f, (1f - s) * p.font.size / 2f)
                    scale(1f, s)
                    green.withAlpha(min(1f, 20f * (dissolveTime - phase)))
                }.apply { periodMillis = 1500 }) // total time
                list.add(SimpleAnimTextPanel("Special Department", style) { p, time, index, _, _ ->
                    p.font = font
                    val phase = time * 4f - index * 0.15f
                    val s = clamp(sin(phase) * 2f + 1f, -1f, +1f)
                    scale(1f, s)
                    if (s < 0f) hsluv(0f, 1f - s) else blue
                })
                val burnPalette = intArrayOf(
                    0, 0x66 shl 24, 0xaa shl 24,
                    black, black, black, black, black,
                    0x533637 or black,
                    0xdd4848 or black,
                    0xf6b24c or black,
                    0xfffab3 or black, 0
                )
                list.add(SimpleAnimTextPanel("Burning", style) { p, time, index, cx, cy ->
                    p.font = font
                    val phase = time * 10f - index * 0.75f
                    val index1 = clamp(burnPalette.lastIndex - phase, 0f, burnPalette.size - 0.001f)
                    val scale = max(1f, 2f - phase / 2f)
                    scale(sqrt(scale), scale)
                    rotate(1f - scale, cx, cy)
                    // smooth index
                    val index1i = clamp(index1.toInt(), 0, burnPalette.size - 2)
                    mixARGB(burnPalette[index1i], burnPalette[index1i + 1], clamp(index1 - index1i))
                }.apply { periodMillis = 1200 })
                val noise = FullNoise(1234L)
                val sketchPalette = intArrayOf(
                    0xa6dee9 or black,
                    0xc5c5c8 or black,
                    0xbecbd2 or black,
                    0x7c99a9 or black
                )
                list.add(SimpleAnimTextPanel("Sketchy", style) { p, time, index, cx, cy ->
                    p.font = font
                    val seed = limitFps(time, 3f) * 3f
                    val pos = index * 5f + seed
                    val y = seed * 0.3f
                    val scale = 5f
                    translate(noise.getValue(pos, y) * scale, noise.getValue(pos, y + 0.1f) * scale)
                    rotate(noise.getValue(pos, y + 3f) - 0.5f, cx, cy)
                    sketchPalette[(noise.getValue(pos) * 1e5).toInt() % sketchPalette.size] // choose a random color
                })
                list
            }
        }
    }

}