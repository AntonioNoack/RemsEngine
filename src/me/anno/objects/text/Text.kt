package me.anno.objects.text

import me.anno.cache.CacheData
import me.anno.cache.instances.TextCache
import me.anno.cache.keys.TextSegmentKey
import me.anno.config.DefaultConfig
import me.anno.fonts.FontManager
import me.anno.fonts.PartResult
import me.anno.fonts.mesh.TextMesh.Companion.DEFAULT_FONT_HEIGHT
import me.anno.fonts.mesh.TextMesh.Companion.DEFAULT_LINE_HEIGHT
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.fonts.mesh.TextRepBase
import me.anno.fonts.signeddistfields.TextSDFGroup
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.objects.lists.Element
import me.anno.objects.lists.SplittableElement
import me.anno.objects.modes.TextMode
import me.anno.objects.modes.TextRenderMode
import me.anno.objects.text.TextInspector.createInspectorWithoutSuperImpl
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths.mix
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4fc
import java.net.URL
import kotlin.math.max
import kotlin.streams.toList

// todo background "color" in the shape of a plane? for selections and such

// todo Kapitälchen

open class Text(parent: Transform? = null) : GFXTransform(parent), SplittableElement {

    constructor(text: String) : this(null) {
        this.text.set(text)
    }

    constructor(text: String, parent: Transform?) : this(parent) {
        this.text.set(text)
    }

    override fun getDocumentationURL(): URL? = URL("https://remsstudio.phychi.com/?s=learn/text")

    val backgroundColor = AnimatedProperty.color()

    var text = AnimatedProperty.string()

    var renderingMode = TextRenderMode.MESH
    var roundSDFCorners = false

    var textAlignment = AnimatedProperty.alignment()
    var blockAlignmentX = AnimatedProperty.alignment()
    var blockAlignmentY = AnimatedProperty.alignment()

    val outlineColor0 = AnimatedProperty.color()
    val outlineColor1 = AnimatedProperty.color(Vector4f(0f))
    val outlineColor2 = AnimatedProperty.color(Vector4f(0f))
    val outlineWidths = AnimatedProperty.vec4(Vector4f(0f, 1f, 1f, 1f))
    val outlineSmoothness = AnimatedProperty(Type.VEC4_PLUS, Vector4f(0f))
    var outlineDepth = AnimatedProperty.float()

    val shadowColor = AnimatedProperty.color(Vector4f(0f))
    val shadowOffset = AnimatedProperty.pos(Vector3f(0f, 0f, -0.1f))
    val shadowSmoothness = AnimatedProperty.floatPlus(0f)

    val startCursor = AnimatedProperty.int(-1)
    val endCursor = AnimatedProperty.int(-1)

    // automatic line break after length x
    var lineBreakWidth = 0f

    // todo allow style by HTML/.md? :D
    var textMode = TextMode.RAW

    var relativeLineSpacing = AnimatedProperty.float(1f)

    var relativeCharSpacing = 0f
    var relativeTabSize = 4f

    var font = Font("Verdana", DEFAULT_FONT_HEIGHT, false, false)
    val charSpacing get() = font.size * relativeCharSpacing
    var forceVariableBuffer = false

    fun createKeys(lineSegmentsWithStyle: PartResult?) =
        lineSegmentsWithStyle?.parts?.map {
            TextSegmentKey(
                it.font,
                font.isBold,
                font.isItalic,
                it.text,
                charSpacing
            )
        }

    var needsUpdate = false
    override fun claimLocalResources(lTime0: Double, lTime1: Double) {
        if (needsUpdate) {
            RemsStudio.updateSceneViews()
            needsUpdate = false
        }
    }

    open fun splitSegments(text: String): PartResult? {
        if (text.isEmpty()) return null
        val awtFont = FontManager.getFont(font)
        val absoluteLineBreakWidth = lineBreakWidth * font.size * 2f / DEFAULT_LINE_HEIGHT
        return awtFont.splitParts(text, font.size, relativeTabSize, relativeCharSpacing, absoluteLineBreakWidth)
    }

    fun getVisualState(text: String): Any = Pair(
        Triple(text, font, lineBreakWidth),
        Triple(renderingMode, roundSDFCorners, charSpacing)
    )

    val shallLoadAsync get() = !forceVariableBuffer
    fun getTextMesh(key: TextSegmentKey): TextRepBase? {
        return TextCache.getEntry(key, textMeshTimeout, shallLoadAsync) { keyInstance ->
            TextMeshGroup(keyInstance.font, keyInstance.text, keyInstance.charSpacing, forceVariableBuffer)
        } as? TextRepBase
    }

    fun getSDFTexture(key: TextSegmentKey): TextSDFGroup? {
        val entry = TextCache.getEntry(key to 1, textMeshTimeout, false) { (keyInstance, _) ->
            TextSDFGroup(keyInstance.font, keyInstance.text, keyInstance.charSpacing)
        } ?: return null
        if (entry !is TextSDFGroup) throw RuntimeException("Got different class for $key to 1: ${entry.javaClass.simpleName}")
        return entry
    }

    fun getDrawDX(width: Float, time: Double): Float {
        val blockAlignmentX01 = blockAlignmentX[time] * .5f + .5f
        return (blockAlignmentX01 - 1f) * width
    }

    fun getDrawDY(lineOffset: Float, totalHeight: Float, time: Double): Float {
        val dy0 = lineOffset * 0.57f // text touches top
        val dy1 = -totalHeight * 0.5f + lineOffset * 0.75f // center line, height of horizontal in e
        val dy2 = -totalHeight + lineOffset // exactly baseline
        val blockAlignmentY11 = blockAlignmentY[time]
        return if (blockAlignmentY11 < 0f) {
            mix(dy0, dy1, blockAlignmentY11 + 1f)
        } else {
            mix(dy1, dy2, blockAlignmentY11)
        }
    }

    fun getSegments(text: String): Pair<PartResult, List<TextSegmentKey>> {
        val data = TextCache.getEntry(getVisualState(text), 1000L, false) {
            val segments = splitSegments(text)
            val keys = createKeys(segments)
            CacheData(segments to keys)
        } as CacheData<*>
        return data.value as Pair<PartResult, List<TextSegmentKey>>
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        TextRenderer.draw(this, stack, time, color) {
            super.draw(stack, time, color)
        }
    }

    fun invalidate() {
        needsUpdate = true
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveWithoutSuper(writer)
    }

    fun saveWithoutSuper(writer: BaseWriter) {

        // basic settings
        writer.writeObject(this, "text", text)
        writer.writeString("font", font.name)
        writer.writeBoolean("isItalic", font.isItalic)
        writer.writeBoolean("isBold", font.isBold)

        // alignment
        writer.writeObject(this, "textAlignment", textAlignment)
        writer.writeObject(this, "blockAlignmentX", blockAlignmentX)
        writer.writeObject(this, "blockAlignmentY", blockAlignmentY)

        // spacing
        writer.writeObject(this, "relativeLineSpacing", relativeLineSpacing)
        writer.writeFloat("relativeTabSize", relativeTabSize, true)
        writer.writeFloat("lineBreakWidth", lineBreakWidth)
        writer.writeFloat("relativeCharSpacing", relativeCharSpacing)

        // outlines
        writer.writeInt("renderingMode", renderingMode.id)
        writer.writeBoolean("roundSDFCorners", roundSDFCorners)
        writer.writeObject(this, "outlineColor0", outlineColor0)
        writer.writeObject(this, "outlineColor1", outlineColor1)
        writer.writeObject(this, "outlineColor2", outlineColor2)
        writer.writeObject(this, "outlineWidths", outlineWidths)
        writer.writeObject(this, "outlineSmoothness", outlineSmoothness)
        writer.writeObject(this, "outlineDepth", outlineDepth)

        // shadows
        writer.writeObject(this, "shadowColor", shadowColor)
        writer.writeObject(this, "shadowOffset", shadowOffset)
        writer.writeObject(this, "shadowSmoothness", shadowSmoothness)

        // rpg cursor animation
        // todo append cursor symbol at the end
        // todo blinking cursor
        writer.writeObject(this, "startCursor", startCursor)
        writer.writeObject(this, "endCursor", endCursor)

    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "textAlignment" -> textAlignment.set(AxisAlignment.find(value)?.id?.toFloat() ?: return)
            "blockAlignmentX" -> blockAlignmentX.set(AxisAlignment.find(value)?.id?.toFloat() ?: return)
            "blockAlignmentY" -> blockAlignmentY.set(AxisAlignment.find(value)?.id?.toFloat() ?: return)
            "renderingMode" -> renderingMode = TextRenderMode.values().firstOrNull { it.id == value } ?: renderingMode
            else -> super.readInt(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "relativeTabSize" -> relativeTabSize = value
            "relativeCharSpacing" -> relativeCharSpacing = value
            "lineBreakWidth" -> lineBreakWidth = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "text" -> text.set(value)
            "font" -> font = font.withName(value)
            else -> super.readString(name, value)
        }
        invalidate()
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "isBold" -> font = font.withBold(value)
            "isItalic" -> font = font.withItalic(value)
            "roundSDFCorners" -> roundSDFCorners = value
            else -> super.readBoolean(name, value)
        }
        invalidate()
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "text" -> text.copyFrom(value)
            "textAlignment" -> textAlignment.copyFrom(value)
            "blockAlignmentX" -> blockAlignmentX.copyFrom(value)
            "blockAlignmentY" -> blockAlignmentY.copyFrom(value)
            "shadowOffset" -> shadowOffset.copyFrom(value)
            "shadowColor" -> shadowColor.copyFrom(value)
            "shadowSmoothness" -> shadowSmoothness.copyFrom(value)
            "relativeLineSpacing" -> relativeLineSpacing.copyFrom(value)
            "outlineColor0" -> outlineColor0.copyFrom(value)
            "outlineColor1" -> outlineColor1.copyFrom(value)
            "outlineColor2" -> outlineColor2.copyFrom(value)
            "outlineWidths" -> outlineWidths.copyFrom(value)
            "outlineDepth" -> outlineDepth.copyFrom(value)
            "outlineSmoothness" -> outlineSmoothness.copyFrom(value)
            "startCursor" -> startCursor.copyFrom(value)
            "endCursor" -> endCursor.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        createInspectorWithoutSuper(list, style, getGroup)
    }

    fun createInspectorWithoutSuper(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) = createInspectorWithoutSuperImpl(list, style, getGroup)

    fun getSelfWithShadows() = getShadows() + this
    fun getShadows() = children.filter { it.name.contains("shadow", true) && it is Text } as List<Text>
    override fun passesOnColor() = false // otherwise white shadows of black text wont work

    override fun getClassName(): String = "Text"
    override fun getDefaultDisplayName() = // text can be null!!!
        (text?.keyframes?.maxBy { it.value.length }?.value ?: text?.defaultValue)?.ifBlank { Dict["Text", "obj.text"] }

    override fun getSymbol() = DefaultConfig["ui.symbol.text", "\uD83D\uDCC4"]

    companion object {

        val tabSpaceType = Type.FLOAT_PLUS.withDefaultValue(4f)
        val lineBreakType = Type.FLOAT_PLUS.withDefaultValue(0f)

        val textMeshTimeout = 5000L
        val lastUsedFonts = arrayOfNulls<String>(max(0, DefaultConfig["lastUsed.fonts.count", 5]))

        /**
         * saves the most recently used fonts
         * */
        fun putLastUsedFont(font: String) {
            if (lastUsedFonts.isNotEmpty()) {
                for (i in lastUsedFonts.indices) {
                    if (lastUsedFonts[i] == font) return
                }
                for (i in 0 until lastUsedFonts.lastIndex) {
                    lastUsedFonts[i] = lastUsedFonts[i + 1]
                }
                lastUsedFonts[lastUsedFonts.lastIndex] = font
            }
        }

    }

    override fun getSplittingModes(): List<String> {
        return listOf("Letters", "Words", "Sentences", "Lines")
    }

    override fun getSplitElement(mode: String, index: Int): Element {
        val text = text[0.0]
        val word = when (mode) {
            "Letters" -> String(Character.toChars(text.codePoints().toList()[index]))
            "Words" -> splitWords(text)[index]
            "Sentences" -> splitSentences(text)[index]
            "Lines" -> text.split('\n')[index]
            else -> "?"
        }
        val child = clone() as Text
        child.text.set(word)
        val (segments, _) = child.getSegments(word)
        val part0 = segments.parts[0]
        val width = part0.lineWidth
        val height = 0f // ???
        return Element(width, height, 0f, child)
    }

    fun splitWords(str: String): List<String> {
        // todo better criterion (?)
        return str.split(' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun splitSentences(str: String): List<String> {
        val result = ArrayList<String>()
        var hasEndSymbols = false
        var lastI = 0
        for (i in str.indices) {
            when (str[i]) {
                '.', '!', '?' -> {
                    hasEndSymbols = true
                }
                '\n' -> {
                    if (i > lastI) result += str.substring(lastI, i)
                    lastI = i + 1
                    hasEndSymbols = false
                }
                ' ', '\t' -> {
                    // ignore
                }
                else -> {// a letter
                    if (hasEndSymbols) {
                        if (i > lastI) result += str.substring(lastI, i).trim()
                        lastI = i
                        hasEndSymbols = false
                    }
                }
            }
        }
        if (str.length > lastI) result += str.substring(lastI)
        return result
    }

    override fun getSplitLength(mode: String): Int {
        val text = text[0.0]
        return when (mode) {
            "Letters" -> text.codePointCount(0, text.length)
            "Words" -> splitWords(text).size
            "Sentences" -> splitSentences(text).size
            "Lines" -> text.count { it == '\n' }
            else -> 0
        }
    }

}