package me.anno.ui.anim

import me.anno.Engine
import me.anno.ecs.annotations.Docs
import me.anno.fonts.FontManager
import me.anno.fonts.TextGroup
import me.anno.fonts.keys.TextCacheKey
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
import me.anno.maths.Maths.max
import me.anno.ui.base.Font
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.toRGB
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.streams.toList

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
    var autoRedraw = true
    var lineSpacing = 0.5f
    var disableSubpixels = true
    var periodMillis = 24L * 3600L * 1000L // 1 day

    override var text: String
        get() = super.text
        set(value) {
            if (super.text != value) {
                lines = value.split('\n')
                    .map { line -> line.cpList() }
            }
            super.text = value
        }

    override var font: Font
        get() = super.font
        set(value) {
            if (value != super.font) {
                super.font = value
                lines = text.split('\n')
                    .map { line -> line.cpList() }
            }
        }

    // cached text and lines for fewer allocations
    private var lines = text.split('\n')
        .map { line -> line.cpList() }

    fun String.cpList() = Pair(this,
        codePoints().toList().map {
            TextCacheKey(String(Character.toChars(it)), font)
        })

    var textGroup: TextGroup? = null
    fun getTextGroup(text: String): TextGroup {
        val font2 = FontManager.getFont(font)
        val group1 = textGroup
        if (group1 != null && group1.text == text && group1.font == font2)
            return group1
        val group2 = TextGroup(font2, text, 0.0)
        textGroup = group2
        return group2
    }

    override fun onUpdate() {
        super.onUpdate()
        if (autoRedraw) invalidateDrawing()
    }

    override fun drawText(dx: Int, dy: Int, color: Int): Int {
        return drawText(dx, dy, text, color)
    }

    override fun drawText(dx: Int, dy: Int, text: String, color: Int): Int {
        return if (text != this.text) {
            drawText2(dx, dy, text.cpList())
        } else {
            val lines = lines
            var sizeX = 0
            val lineOffset = (font.size * (1f + lineSpacing)).roundToInt()
            for (index in lines.indices) {
                val s = lines[index]
                val size = drawText2(dx, dy + index * lineOffset, s)
                sizeX = max(GFXx2D.getSizeX(size), sizeX)
            }
            GFXx2D.getSize(sizeX, (lines.size - 1) * lineOffset + font.sizeInt)
        }
    }

    fun drawText2(dx: Int, dy: Int, text: Pair<String, List<TextCacheKey>>): Int {

        val x = this.x + dx + padding.left
        val y = this.y + dy + padding.top

        val alignX = alignmentX
        val alignY = alignmentY
        val equalSpaced = useMonospaceCharacters

        val time = (Engine.gameTime % max(1, periodMillis * MILLIS_TO_NANOS)) / 1e9f
        val shader = (if (disableSubpixels) ShaderLib.textShader else ShaderLib.subpixelCorrectTextShader[0]).value
        shader.use()

        var h = font.sizeInt
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
                val size = FontManager.getSize(key)
                h = GFXx2D.getSizeY(size)
                if (!key.text.isBlank2()) {
                    val texture = FontManager.getTexture(key)
                    if (texture != null && (texture !is Texture2D || texture.isCreated)) {
                        texture.bindTrulyNearest(0)
                        val x2 = fx + (charWidth - texture.w) / 2
                        if (resetTransform) transform.set(backup)
                        val color2 = animate(time, index, x2 + texture.w / 2f, y2 + texture.h / 2f)
                        if (color2.a() > 0) {
                            shader.m4x4("transform", transform)
                            GFXx2D.posSize(shader, x2, y2, texture.w, texture.h)
                            if (disableSubpixels) shader.v4f("backgroundColor", color2 and 0xffffff)
                            shader.v4f("textColor", color2)
                            GFX.flat01.draw(shader)
                        }
                    }
                }
                fx += charWidth
            }

            transform.set(backup)
            JomlPools.mat4f.sub(1)

            GFX.loadTexturesSync.pop()

            totalWidth = fx - (x + dxi)

        } else {

            val group = getTextGroup(text.first)

            val textWidth = group.offsets.last().toFloat()

            val dxi = DrawTexts.getOffset(textWidth.roundToInt(), alignX)
            val dyi = DrawTexts.getOffset(font.sampleHeight, alignY)

            val y2 = (y + dyi).toFloat()

            val text1 = text.second
            for (index in text1.indices) {
                val txt = text1[index]
                val size = FontManager.getSize(txt)
                val o0 = group.offsets[index].toFloat()
                val o1 = group.offsets[index + 1].toFloat()
                val fx = x + dxi + o0
                val w = o1 - o0
                h = GFXx2D.getSizeY(size)
                if (!txt.text.isBlank2()) {
                    // todo in TextGroup, ti is broken ... why??
                    val texture = FontManager.getTexture(txt)
                    if (texture != null && (texture !is Texture2D || texture.isCreated)) {
                        texture.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
                        val x2 = fx + (w - texture.w) / 2
                        if (resetTransform) transform.set(backup)
                        val color2 = animate(time, index, x2 + texture.w / 2f, y2 + texture.h / 2f)
                        if (color2.a() > 0) {
                            shader.m4x4("transform", transform)
                            GFXx2D.posSize(shader, x2, y2, texture.w.toFloat(), texture.h.toFloat())
                            if (disableSubpixels) shader.v4f("backgroundColor", color2 and 0xffffff)
                            shader.v4f("textColor", color2)
                            GFX.flat01.draw(shader)
                        }
                    }
                }
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

    override val className: String get() = "AnimTextPanel"

    companion object {

        fun limitFps(time: Float, fps: Float) =
            round(time * fps) / fps

        fun px(cx: Float) = +((cx - (GFX.viewportX + GFX.viewportWidth / 2)) / (GFX.viewportWidth)) * 2f
        fun py(cy: Float) = -((cy - (GFX.viewportY + GFX.viewportHeight / 2)) / (GFX.viewportHeight)) * 2f

        /**
         * rotate the current letter around the pixel coordinates (cx,cy) by angleRadians;
         * use cx,cy from the parameters in AnimTextPanel.animate()
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
         * scale the current letter by the factor (sx,sy)
         * */
        fun scale(sx: Float, sy: Float = sx) {
            GFXx2D.transform.scale(sx, sy, 1f)
        }

        /**
         * translate the current letter by (dx,dy) pixels
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