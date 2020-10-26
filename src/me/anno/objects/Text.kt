package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.fonts.AWTFont
import me.anno.fonts.FontManager
import me.anno.fonts.PartResult
import me.anno.fonts.mesh.FontMesh.Companion.DEFAULT_LINE_HEIGHT
import me.anno.fonts.mesh.FontMesh2
import me.anno.fonts.mesh.FontMeshBase
import me.anno.gpu.GFX
import me.anno.gpu.GFXx3D.draw3D
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.FilteringMode
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.objects.cache.Cache
import me.anno.objects.modes.TextMode
import me.anno.studio.RemsStudio
import me.anno.studio.RemsStudio.onLargeChange
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.RemsStudio.selectedProperty
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import java.awt.Font
import java.io.File
import kotlin.math.max

// todo when a new part is loaded, notify the ui

// todo animated text, like in RPGs, where text appears; or like typing

// todo background "color" in the shape of a plane? for selections and such
open class Text(text: String = "", parent: Transform? = null) : GFXTransform(parent) {

    val backgroundColor = AnimatedProperty.color()

    var text = text.replace("\r", "")
        set(value) {
            field = value.replace("\r", "")
        }

    override fun getSymbol() = DefaultConfig["ui.symbol.text", "\uD83D\uDCC4"]

    // todo blurry, colorful text shadow
    // todo by calculated distance fields:
    // todo just project the points outwards xD
    // todo and create the shadow on a (linked?) child element

    // done multiple line alignment
    // done working tabs


    // how we apply sampling probably depends on our AA solution...

    // parameters
    var font = "Verdana"

    var isBold = false
    var isItalic = false

    var textAlignment = AxisAlignment.CENTER
    var blockAlignmentX = AxisAlignment.CENTER
    var blockAlignmentY = AxisAlignment.CENTER

    // automatic line break after length x
    var lineBreakWidth = -1f

    var textMode = TextMode.RAW

    var relativeLineSpacing = AnimatedProperty.float(1f)

    var relativeCharSpacing = 0f
    var relativeTabSize = 4f

    // caching
    var lastText = ""

    // todo allow style by HTML/.md? :D
    var lineSegmentsWithStyle: PartResult? = null
    var keys: List<FontMeshKey>? = null

    val fontSize0 = 20f
    val charSpacing get() = fontSize0 * relativeCharSpacing

    fun createKeys() =
        lineSegmentsWithStyle?.parts?.map { FontMeshKey(it.font, isBold, isItalic, it.text, charSpacing) }

    var needsUpdate = false
    override fun claimLocalResources(lTime0: Double, lTime1: Double) {
        if (needsUpdate) {
            RemsStudio.updateSceneViews()
            needsUpdate = false
        }
    }

    open fun splitSegments(text: String): PartResult? {
        if (text.isEmpty()) return null
        val awtFont = FontManager.getFont(font, fontSize0, isBold, isItalic) as AWTFont
        val absoluteLineBreakWidth = lineBreakWidth * fontSize0 * 2f / DEFAULT_LINE_HEIGHT
        return awtFont.splitParts(text, fontSize0, relativeTabSize, relativeCharSpacing, absoluteLineBreakWidth)
    }

    data class FontMeshKey(
        val font: Font, val isBold: Boolean, val isItalic: Boolean,
        val text: String, val charSpacing: Float
    ) {
        fun equals(isBold: Boolean, isItalic: Boolean, text: String, charSpacing: Float) =
            isBold == this.isBold && isItalic == this.isItalic && text == this.text && charSpacing == this.charSpacing
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        val text = text
        val isBold = isBold
        val isItalic = isItalic

        val shallLoadAsync = true

        val charSpacing = charSpacing

        if (text.isNotBlank()) {

            stack.pushMatrix()

            if (text != lastText || keys == null) {
                lastText = text
                lineSegmentsWithStyle = splitSegments(text)
                keys = createKeys()
            } else {
                // !none = at least one?, is not equal, so needs update
                if (!keys!!.withIndex().none { (index, fontMeshKey) ->
                        !fontMeshKey.equals(isBold, isItalic, lineSegmentsWithStyle!!.parts[index].text, charSpacing)
                    }) {
                    keys = createKeys()
                }
            }

            val lineSegmentsWithStyle = lineSegmentsWithStyle!!
            val keys = keys!!

            val exampleLayout = lineSegmentsWithStyle.exampleLayout
            val scaleX = DEFAULT_LINE_HEIGHT / (exampleLayout.ascent + exampleLayout.descent)
            val scaleY = 1f / (exampleLayout.ascent + exampleLayout.descent)
            val width = lineSegmentsWithStyle.width * scaleX
            val height = lineSegmentsWithStyle.height * scaleY

            val lineOffset = -DEFAULT_LINE_HEIGHT * relativeLineSpacing[time]
            val textAlignment = textAlignment

            fun getFontMesh(fontMeshKey: FontMeshKey): FontMeshBase? {
                return Cache.getEntry(fontMeshKey, fontMeshTimeout, shallLoadAsync) {
                    FontMesh2(fontMeshKey.font, fontMeshKey.text, charSpacing)
                } as? FontMeshBase
            }

            // min and max x are cached for long texts with thousands of lines (not really relevant)
            // actual text height vs baseline? for height

            val totalHeight = lineOffset * height

            stack.translate(
                when (blockAlignmentX) {
                    AxisAlignment.MIN -> -width
                    AxisAlignment.CENTER -> -width / 2
                    AxisAlignment.MAX -> 0f
                }, when (blockAlignmentY) {
                    AxisAlignment.MIN -> 0f + lineOffset * 0.57f // text touches top
                    AxisAlignment.CENTER -> -totalHeight * 0.5f + lineOffset * 0.75f // center line, height of horizontal in e
                    AxisAlignment.MAX -> -totalHeight + lineOffset // exactly baseline
                }, 0f
            )

            for ((index, value) in lineSegmentsWithStyle.parts.withIndex()) {

                val fontMeshKey = keys[index]

                val fontMesh = getFontMesh(fontMeshKey)
                if (fontMesh == null) {
                    if (GFX.isFinalRendering) throw MissingFrameException(File("Text"))
                    needsUpdate = true
                    continue
                }

                val offsetX = when (textAlignment) {
                    AxisAlignment.MIN -> 0f
                    AxisAlignment.CENTER -> (width - value.lineWidth * scaleX) / 2f
                    AxisAlignment.MAX -> (width - value.lineWidth * scaleX)
                }// - 2 * fontMesh.minX.toFloat()

                stack.pushMatrix()

                stack.translate(value.xPos * scaleX + offsetX, value.yPos * scaleY * lineOffset, 0f)

                fontMesh.draw(stack) { buffer ->
                    draw3D(stack, buffer, whiteTexture, color, FilteringMode.NEAREST, ClampMode.CLAMP, null)
                }

                stack.popMatrix()

            }

            // todo calculate (signed) distance fields for different kinds of shadows from the mesh

            stack.popMatrix()

        }


    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("text", text)
        writer.writeString("font", font)
        writer.writeBool("isItalic", isItalic, true)
        writer.writeBool("isBold", isBold, true)
        writer.writeObject(this, "relativeLineSpacing", relativeLineSpacing)
        writer.writeInt("textAlignment", textAlignment.id, true)
        writer.writeInt("blockAlignmentX", blockAlignmentX.id, true)
        writer.writeInt("blockAlignmentY", blockAlignmentY.id, true)
        writer.writeFloat("relativeTabSize", relativeTabSize, true)
        writer.writeFloat("lineBreakWidth", lineBreakWidth)
        writer.writeFloat("relativeCharSpacing", relativeCharSpacing)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "textAlignment" -> textAlignment = AxisAlignment.values().firstOrNull { it.id == value } ?: textAlignment
            "blockAlignmentX" -> blockAlignmentX =
                AxisAlignment.values().firstOrNull { it.id == value } ?: blockAlignmentX
            "blockAlignmentY" -> blockAlignmentY =
                AxisAlignment.values().firstOrNull { it.id == value } ?: blockAlignmentY
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

    fun invalidate() {
        lastText = if (text == "") "a" else ""
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "text" -> text = value
            "font" -> font = value
            else -> super.readString(name, value)
        }
        invalidate()
    }

    override fun readBool(name: String, value: Boolean) {
        when (name) {
            "isBold" -> isBold = value
            "isItalic" -> isItalic = value
            else -> super.readBool(name, value)
        }
        invalidate()
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "relativeLineSpacing" -> relativeLineSpacing.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)

        list += TextInputML("Text", style, text)
            .setChangeListener {
                getSelfWithShadows().forEach { c -> c.text = it }
                onSmallChange("text-changed")
            }
            .setIsSelectedListener { selectedProperty = null }
        val fontList = ArrayList<String>()
        fontList += font
        fontList += GFX.menuSeparator

        fun sortFavourites() {
            fontList.sort()
            val lastUsedSet = lastUsedFonts.toHashSet()
            fontList.sortByDescending { if (it == GFX.menuSeparator) 1 else if (it in lastUsedSet) 2 else 0 }
        }

        FontManager.requestFontList { systemFonts ->
            synchronized(fontList) {
                fontList += systemFonts.filter { it != font }
                sortFavourites()
            }
        }

        // todo general favourites for all enum types?
        // todo at least a generalized form to make it simpler?

        val fontGroup = getGroup("Font", "font")

        fontGroup += EnumInput("Font Name", true, font, fontList, style)
            .setChangeListener { it, _, _ ->
                getSelfWithShadows().forEach { c -> c.font = it }
                invalidate()
                putLastUsedFont(it)
                sortFavourites()
                onSmallChange("text-font")
            }
            .setIsSelectedListener { show(null) }
        fontGroup += BooleanInput("Italic", isItalic, style)
            .setChangeListener {
                getSelfWithShadows().forEach { c -> c.isItalic = it }
                invalidate()
                onSmallChange("text-italic")
            }
            .setIsSelectedListener { show(null) }
        fontGroup += BooleanInput("Bold", isBold, style)
            .setChangeListener {
                getSelfWithShadows().forEach { c -> c.isBold = it }
                invalidate()
                onSmallChange("text-bold")
            }
            .setIsSelectedListener { show(null) }

        val alignGroup = getGroup("Alignment", "alignment")
        fun align(title: String, value: AxisAlignment, x: Boolean, set: (self: Text, AxisAlignment) -> Unit) {
            operator fun AxisAlignment.get(x: Boolean) = if (x) xName else yName
            alignGroup += EnumInput(title, true,
                value[x],
                AxisAlignment.values().map { it[x] }, style
            )
                .setIsSelectedListener { show(null) }
                .setChangeListener { name, _, _ ->
                    val alignment = AxisAlignment.values().first { it[x] == name }
                    getSelfWithShadows().forEach { set(it, alignment) }
                    invalidate()
                    onSmallChange("text-alignment")
                }
        }

        align("Text Alignment", textAlignment, true) { self, it -> self.textAlignment = it }
        align("Block Alignment X", blockAlignmentX, true) { self, it -> self.blockAlignmentX = it }
        align("Block Alignment Y", blockAlignmentY, false) { self, it -> self.blockAlignmentY = it }

        val spaceGroup = getGroup("Spacing", "spacing")
        // make this element separable from the parent???
        spaceGroup += VI("Character Spacing", "Space between individual characters", null, relativeCharSpacing, style) {
            relativeCharSpacing = it
            invalidate()
            onSmallChange("char-space")
        }
        spaceGroup += VI("Line Spacing", "How much lines are apart from each other", relativeLineSpacing, style)
        spaceGroup += VI(
            "Tab Size", "Relative tab size, in widths of o's",
            Type.FLOAT_PLUS, relativeTabSize, style
        ) {
            relativeTabSize = it
            invalidate()
            onSmallChange("tab-size")
        }
        spaceGroup += VI(
            "Line Break Width", "How broad the text shall be, at maximum; < 0 = no limit",
            Type.FLOAT, lineBreakWidth, style
        ) {
            lineBreakWidth = it
            invalidate()
            onSmallChange("line-break-width")
        }

        val ops = getGroup("Operations", "operations")
        ops += ButtonPanel("Create Shadow", style)
            .setSimpleClickListener {
                // such a mess is the result of copying colors from the editor ;)
                val signalColor = Vector4f(HSLuv.toRGB(Vector3f(0.000f, 0.934f, 0.591f)), 1f)
                val shadow = clone() as Text
                shadow.name = "Shadow"
                shadow.comment = "Keep \"shadow\" in the name for automatic property inheritance"
                // this avoids user reports, from people, who can't see their shadow
                // making something black should be simple
                shadow.color.set(signalColor)
                shadow.position.set(Vector3f(0.01f, -0.01f, -0.001f))
                shadow.relativeLineSpacing = relativeLineSpacing // evil ;)
                addChild(shadow)
                GFX.select(shadow)
                onLargeChange()
            }

    }

    fun getSelfWithShadows() = getShadows() + this
    fun getShadows() = children.filter { it.name.contains("shadow", true) && it is Text } as List<Text>
    override fun passesOnColor() = false // otherwise white shadows of black text wont work

    override fun getClassName(): String = "Text"


    companion object {

        // save the last used fonts? yes :)
        // todo per project? idk
        val fontMeshTimeout = 5000L
        val lastUsedFonts = arrayOfNulls<String>(max(0, DefaultConfig["lastUsed.fonts.count", 5]))
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

}