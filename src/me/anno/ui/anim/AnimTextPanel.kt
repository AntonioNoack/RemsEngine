package me.anno.ui.anim

import me.anno.Time
import me.anno.ecs.annotations.Docs
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.GlyphLayout
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths.PIf
import me.anno.maths.MinMax.max
import me.anno.ui.Style
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.Color.a
import me.anno.utils.Color.toRGB
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import me.anno.utils.types.Strings.splitLines
import kotlin.math.round

@Docs("Text panel with char-wise animation")
open class AnimTextPanel(text: String, style: Style) : TextPanel(text, style) {

    @Docs("AnimTextPanel, which can be created with a lambda")
    class SimpleAnimTextPanel(
        text: String, style: Style,
        val animation: SimpleAnimation,
    ) : AnimTextPanel(text, style) {

        // saving allocations by declaring it a functional interface;
        // for lambdas, floats and integers would need to be wrapped into Objects
        fun interface SimpleAnimation {
            fun animate(panel: SimpleAnimTextPanel, time: Float, index: Int, cx: Float, cy: Float): Int
        }

        override fun animate(time: Float, index: Int, cx: Float, cy: Float): Int {
            return animation.animate(this, time, index, cx, cy)
        }
    }

    open fun animate(time: Float, index: Int, cx: Float, cy: Float): Int {
        return textColor
    }

    var resetTransform = true
    var lineSpacing = 0.5f
    var disableSubpixels = true
    var periodMillis = 24L * 3600L * 1000L // 1 day

    override var text: String
        get() = super.text
        set(value) {
            if (super.text != value) {
                lines = value.splitLines()
                    .map { line -> line.cpList() }
            }
            super.text = value
        }

    override var font: Font
        get() = super.font
        set(value) {
            if (value != super.font) {
                super.font = value
                lines = text.splitLines()
                    .map { line -> line.cpList() }
            }
        }

    // cached text and lines for fewer allocations
    private var lines = text.splitLines()
        .map { line -> line.cpList() }

    fun String.cpList() = Pair(
        this,
        codepoints().map { char ->
            TextCacheKey(char.joinChars(), font)
        })

    var glyphLayout: GlyphLayout? = null
    fun getTextGroup(text: String): GlyphLayout {
        val group1 = glyphLayout
        if (group1 != null && group1.text == text && group1.font == font)
            return group1
        val group2 = GlyphLayout(font, text, 0f, Int.MAX_VALUE)
        glyphLayout = group2
        return group2
    }

    override fun drawText(color: Int) {
        if (text != this.text) {
            drawText2(0, 0, text.cpList())
        } else {
            val lines = lines
            val lineOffset = (font.size * (1f + lineSpacing)).roundToIntOr()
            for (index in lines.indices) {
                val s = lines[index]
                drawText2(0, index * lineOffset, s)
            }
        }
    }

    fun drawText2(dx: Int, dy: Int, text: Pair<String, List<TextCacheKey>>): Int {

        val x = this.x + dx + padding.left
        val y = this.y + dy + padding.top

        val alignX = alignmentX
        val alignY = alignmentY
        val equalSpaced = useMonospaceCharacters

        val time = (Time.gameTime % max(1.0, periodMillis * 1e-3)).toFloat()
        val shader =
            (if (disableSubpixels) ShaderLib.textShader else ShaderLib.subpixelCorrectTextGraphicsShader[0]).value
        shader.use()

        var h = font.lineHeightI
        val totalWidth: Int

        GFX.loadTexturesSync.push(true)

        val transform = GFXx2D.transform
        val backup = JomlPools.mat4f.create()
        backup.set(transform)

        if (!disableSubpixels) shader.v4f("backgroundColor", backgroundColor)

        if (equalSpaced) {

            val text1 = text.second
            val charWidth = DrawTexts.getTextSizeX(font, "x", widthLimit, heightLimit)
            val textWidth = charWidth * text1.size

            val dxi = DrawTexts.getOffset(textWidth, alignX)
            val dyi = DrawTexts.getOffset(font.sampleHeight, alignY)

            var fx = x + dxi
            val y2 = y + dyi

            for (index in text1.indices) {
                val key = text1[index]
                val size = FontManager.getSize(key).waitFor() ?: 0
                h = GFXx2D.getSizeY(size)
                if (!key.text.isBlank2()) {
                    val texture = FontManager.getTexture(key).waitFor()
                    if (texture != null && texture.wasCreated) {
                        texture.bindTrulyNearest(0)
                        val x2 = fx + (charWidth - texture.width) / 2
                        if (resetTransform) transform.set(backup)
                        val color2 = animate(time, index, x2 + texture.width / 2f, y2 + texture.height / 2f)
                        if (color2.a() > 0) {
                            shader.m4x4("transform", transform)
                            posSize(shader, x2, y2, texture.width, texture.height, true)
                            if (disableSubpixels) shader.v4f("backgroundColor", color2 and 0xffffff)
                            shader.v4f("textColor", color2)
                            flat01.draw(shader)
                        }
                    }
                }
                fx += charWidth
            }

            totalWidth = fx - (x + dxi)
        } else {

            val group = getTextGroup(text.first)

            val textWidth = group.width

            val dxi = DrawTexts.getOffset(textWidth.roundToIntOr(), alignX)
            val dyi = DrawTexts.getOffset(font.sampleHeight, alignY)

            val y2 = (y + dyi).toFloat()

            val text1 = text.second
            for (index in text1.indices) {
                val txt = text1[index]
                val size = FontManager.getSize(txt).waitFor() ?: 0
                val dx0 = group.getX0(index)
                val dx1 = group.getX1(index)
                val fx = x + dxi + dx0
                val w = dx1 - dx0
                h = GFXx2D.getSizeY(size)
                if (!txt.text.isBlank2()) {
                    val texture = FontManager.getTexture(txt).waitFor()
                    if (texture != null && texture.wasCreated) {
                        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP)
                        val x2 = fx + (w - texture.width) / 2
                        if (resetTransform) transform.set(backup)
                        val color2 = animate(time, index, x2 + texture.width / 2f, y2 + texture.height / 2f)
                        if (color2.a() > 0) {
                            shader.m4x4("transform", transform)
                            posSize(shader, x2, y2 + texture.height, texture.width.toFloat(), -texture.height.toFloat())
                            if (disableSubpixels) shader.v4f("backgroundColor", color2 and 0xffffff)
                            shader.v4f("textColor", color2)
                            flat01.draw(shader)
                        }
                    }
                }
            }

            totalWidth = textWidth.roundToIntOr()
        }

        transform.set(backup)
        JomlPools.mat4f.sub(1)

        GFX.loadTexturesSync.pop()

        return GFXx2D.getSize(totalWidth, h)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeBoolean("resetTransform", resetTransform)
        writer.writeFloat("lineSpacing", lineSpacing)
        writer.writeBoolean("disableSubpixels", disableSubpixels)
        writer.writeLong("periodMillis", periodMillis)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "resetTransform" -> resetTransform = value == true
            "disableSubpixels" -> disableSubpixels = value == true
            "lineSpacing" -> lineSpacing = value as? Float ?: return
            "periodMillis" -> periodMillis = value as? Long ?: return
            else -> super.setProperty(name, value)
        }
    }

    companion object {

        fun limitFps(time: Float, fps: Float) =
            round(time * fps) / fps

        fun px(cx: Float) = +((cx - (GFX.viewportX + GFX.viewportWidth / 2)) / (GFX.viewportWidth)) * 2f
        fun py(cy: Float) = -((cy - (GFX.viewportY + GFX.viewportHeight / 2)) / (GFX.viewportHeight)) * 2f

        /**
         * rotate the current letter around the pixel coordinates (cx,cy) by angleRadians;
         * use cx, cy from the parameters in AnimTextPanel.animate()
         * */
        fun rotate(angleRadians: Float, cx: Float, cy: Float) {
            val transform = GFXx2D.transform
            val px = px(cx)
            val py = py(cy)
            val a = GFX.viewportWidth.toFloat() / GFX.viewportHeight
            transform.translate(+px, +py, 0f)
            transform.scale(1f, a, 1f)
            transform.rotateZ(angleRadians)
            transform.scale(1f, 1f / a, 1f)
            transform.translate(-px, -py, 0f)
        }

        /**
         * scale the current letter by the factor (sx, sy)
         * */
        fun scale(sx: Float, sy: Float = sx) {
            GFXx2D.transform.scale(sx, sy, 1f)
        }

        /**
         * translate the current letter by (dx, dy) pixels
         * */
        fun translate(dx: Float, dy: Float) {
            GFXx2D.transform.translate(dx * 2f / GFX.viewportWidth, -dy * 2f / GFX.viewportHeight, 0f)
        }

        /**
         * create a hsluv color from an angle, saturation and luma value
         * */
        fun hsluv(angleRadians: Float, saturation: Float = 1f, luma: Float = 0.7f): Int {
            val vec = JomlPools.vec3f.borrow()
            return HSLuv.toRGB(vec.set(angleRadians / (2f * PIf), saturation, luma)).toRGB()
        }

        /**
         * not fully implemented, can/will change in the future
         * */
        fun perspective(cx: Float, cy: Float, rx: Float, ry: Float) {
            // todo apply this effect gradually. fov vs scale...
            // apply a perspective effect
            val transform = GFXx2D.transform
            val a = GFX.viewportWidth.toFloat() / GFX.viewportHeight
            val m = JomlPools.mat4f.borrow()
            val px = px(cx)
            val py = py(cy)
            m.identity()
            m.perspective(PIf / 2f, 1f, 0f, 2f)
            transform.translate(+px, +py, 0f)
            transform.scale(1f, a, 1f)
            transform.mul(m)
            transform.translate(0f, 0f, -1f)
            transform.rotateX(rx)
            transform.rotateY(ry)
            transform.scale(1f, 1f / a, 1f)
            transform.translate(-px, -py, 0f)
        }
    }
}