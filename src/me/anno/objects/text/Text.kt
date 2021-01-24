package me.anno.objects.text

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
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField.sdfResolution
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFXx3D.draw3DText
import me.anno.gpu.GFXx3D.draw3DTextWithOffset
import me.anno.gpu.GFXx3D.drawOutlinedText
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.objects.modes.TextMode
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.Selection.selectTransform
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.Font
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.menuSeparator
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import me.anno.utils.Casting.castToFloat
import me.anno.utils.Vectors.plus
import me.anno.utils.Vectors.times
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.awt.font.TextLayout
import kotlin.math.max
import kotlin.math.min

// todo background "color" in the shape of a plane? for selections and such

// todo fix color attractors

open class Text(text: String = "", parent: Transform? = null) : GFXTransform(parent) {

    val backgroundColor = AnimatedProperty.color()

    var text = text.replace("\r", "")
        set(value) {
            field = value.replace("\r", "")
        }

    var renderingMode = TextRenderMode.MESH
    var roundSDFCorners = false

    var textAlignment = AxisAlignment.CENTER
    var blockAlignmentX = AxisAlignment.CENTER
    var blockAlignmentY = AxisAlignment.CENTER

    val outlineColor0 = AnimatedProperty.color()
    val outlineColor1 = AnimatedProperty.color(Vector4f(0f))
    val outlineColor2 = AnimatedProperty.color(Vector4f(0f))
    val outlineWidths = AnimatedProperty.vec4(Vector4f(0f, 1f, 1f, 1f))
    val outlineSmoothness = AnimatedProperty<Vector4f>(Type.VEC4_PLUS)

    val startCursor = AnimatedProperty.int(-1)
    val endCursor = AnimatedProperty.int(-1)

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
    var keys: List<TextSegmentKey>? = null

    var font = Font("Verdana", DEFAULT_FONT_HEIGHT, false, false)
    val charSpacing get() = font.size * relativeCharSpacing
    var forceVariableBuffer = false

    fun createKeys() =
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

    var lastVisualState: Any? = null
    fun getVisualState() = Triple(Triple(text, font, lineBreakWidth), renderingMode, roundSDFCorners)

    val shallLoadAsync get() = !forceVariableBuffer
    fun getTextMesh(key: TextSegmentKey): TextRepBase? {
        return TextCache.getEntry(key, textMeshTimeout, shallLoadAsync) {
            TextMeshGroup(key.font, key.text, key.charSpacing, forceVariableBuffer)
        } as? TextRepBase
    }

    fun getTextTexture(key: TextSegmentKey): TextSDFGroup? {
        val entry = TextCache.getEntry(key to 1, textMeshTimeout, shallLoadAsync) {
            TextSDFGroup(key.font, key.text, charSpacing, forceVariableBuffer)
        } ?: return null
        if (entry !is TextSDFGroup) throw RuntimeException("Got different class for $key to 1: ${entry.javaClass.simpleName}")
        return entry
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        val text = text
        val isBold = font.isBold
        val isItalic = font.isItalic

        val charSpacing = charSpacing

        if (text.isBlank()) {
            super.onDraw(stack, time, color)
            return
        }

        val visualState = getVisualState()
        if (lastVisualState != visualState || keys == null) {
            lastText = text
            lineSegmentsWithStyle = splitSegments(text)
            keys = createKeys()
            lastVisualState = visualState
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

        // min and max x are cached for long texts with thousands of lines (not really relevant)
        // actual text height vs baseline? for height

        val totalHeight = lineOffset * height

        val dx = when (blockAlignmentX) {
            AxisAlignment.MIN -> -width
            AxisAlignment.CENTER -> -width / 2
            AxisAlignment.MAX -> 0f
        }

        val dy = when (blockAlignmentY) {
            AxisAlignment.MIN -> 0f + lineOffset * 0.57f // text touches top
            AxisAlignment.CENTER -> -totalHeight * 0.5f + lineOffset * 0.75f // center line, height of horizontal in e
            AxisAlignment.MAX -> -totalHeight + lineOffset // exactly baseline
        }

        val renderingMode = renderingMode
        val drawMeshes = renderingMode == TextRenderMode.MESH
        val drawTextures = !drawMeshes

        // limit the characters
        // cursorLimit1 .. cursorLimit2
        val textLength = text.codePoints().count().toInt()
        val startCursor = max(0, startCursor[time])
        var endCursor = min(textLength, endCursor[time])
        if (endCursor < 0) endCursor = textLength

        if (startCursor > endCursor) {
            // invisible
            super.onDraw(stack, time, color)
            return
        }

        val parentColor = parent?.getLocalColor() ?: Vector4f(1f)

        firstTimeDrawing = true

        var charIndex = 0

        val lbw = lineBreakWidth
        if (lbw >= 0f && !isFinalRendering && selectedTransform === this) {
            // draw the line
            // why 0.81? correct x-scale? (off by ca ~ x0.9)
            val x0 = dx + width * 0.5f
            val minX = x0 - 0.81f * lbw
            val maxX = x0 + 0.81f * lbw
            val y0 = dy - lineOffset * 0.75f
            val y1 = y0 + totalHeight
            Grid.drawLine(stack, color, Vector3f(minX, y0, 0f), Vector3f(minX, y1, 0f))
            Grid.drawLine(stack, color, Vector3f(maxX, y0, 0f), Vector3f(maxX, y1, 0f))
        }

        for ((index, part) in lineSegmentsWithStyle.parts.withIndex()) {

            val startIndex = charIndex
            val partLength = part.codepointLength
            val endIndex = charIndex + partLength

            val localMin = max(0, startCursor - startIndex)
            val localMax = min(partLength, endCursor - startIndex)

            if (localMin < localMax) {

                val key = keys[index]

                val offsetX = when (textAlignment) {
                    AxisAlignment.MIN -> 0f
                    AxisAlignment.CENTER -> (width - part.lineWidth * scaleX) / 2f
                    AxisAlignment.MAX -> (width - part.lineWidth * scaleX)
                }

                val lineDeltaX = dx + part.xPos * scaleX + offsetX
                val lineDeltaY = dy + part.yPos * scaleY * lineOffset

                if (drawMeshes) {
                    drawMesh(
                        key, time, stack, color,
                        lineDeltaX, lineDeltaY,
                        localMin, localMax
                    )
                }

                if (drawTextures) {
                    drawTexture(
                        key, time, stack, color,
                        lineDeltaX, lineDeltaY,
                        localMin, localMax,
                        exampleLayout, parentColor
                    )
                }

            }

            charIndex = endIndex

        }

    }

    var firstTimeDrawing = false

    private fun drawTexture(
        key: TextSegmentKey, time: Double, stack: Matrix4fArrayList,
        color: Vector4f, lineDeltaX: Float, lineDeltaY: Float,
        startIndex: Int, endIndex: Int,
        exampleLayout: TextLayout, parentColor: Vector4f
    ) {

        val sdf2 = getTextTexture(key)
        if (sdf2 == null) {
            if (isFinalRendering) throw MissingFrameException("Text-Texture (291) $font: '$text'")
            needsUpdate = true
            return
        }

        val sdfResolution = sdfResolution

        sdf2.charByChar = renderingMode != TextRenderMode.SDF_JOINED
        sdf2.roundCorners = roundSDFCorners
        sdf2.draw(startIndex, endIndex) { _, sdf, xOffset ->

            val texture = sdf?.texture
            if (texture != null && texture.isCreated) {

                stack.pushMatrix()

                val baseScale =
                    DEFAULT_LINE_HEIGHT / sdfResolution / (exampleLayout.ascent + exampleLayout.descent)

                val scale = Vector2f(0.5f * texture.w * baseScale, 0.5f * texture.h * baseScale)

                /**
                 * character- and alignment offset
                 * */
                stack.translate(lineDeltaX + xOffset, lineDeltaY, 0f)
                stack.scale(scale.x, scale.y, 1f)

                val sdfOffset = sdf.offset
                val offset = Vector2f(
                    (lineDeltaX + xOffset) * scale.x,
                    lineDeltaY * scale.y
                ) + sdfOffset

                /**
                 * offset, because the textures are always centered; don't start from the bottom left
                 * (text mesh does)
                 * */
                stack.translate(sdfOffset.x, sdfOffset.y, 0f)

                if (firstTimeDrawing) {

                    val outline = outlineWidths[time] * sdfResolution
                    outline.y = max(0f, outline.y) + outline.x
                    outline.z = max(0f, outline.z) + outline.y
                    outline.w = max(0f, outline.w) + outline.z

                    val smoothness = outlineSmoothness[time] * sdfResolution

                    drawOutlinedText(
                        this, time,
                        stack, offset, scale,
                        texture, color, 5,
                        arrayOf(
                            color,
                            outlineColor0[time] * parentColor,
                            outlineColor1[time] * parentColor,
                            outlineColor2[time] * parentColor,
                            Vector4f(0f)
                        ),
                        floatArrayOf(-1e3f, outline.x, outline.y, outline.z, outline.w),
                        floatArrayOf(0f, smoothness.x, smoothness.y, smoothness.z, smoothness.w)
                    )

                    firstTimeDrawing = false

                } else {

                    drawOutlinedText(stack, offset, scale, texture)

                }

                stack.popMatrix()

            } else if (sdf?.isValid != true) {

                if (isFinalRendering) throw MissingFrameException("Text-Texture (367) $font: '$text'")
                needsUpdate = true

            }

        }
    }

    private fun drawMesh(
        key: TextSegmentKey, time: Double, stack: Matrix4fArrayList,
        color: Vector4f, lineDeltaX: Float, lineDeltaY: Float,
        startIndex: Int, endIndex: Int
    ) {

        val textMesh = getTextMesh(key)
        if (textMesh == null) {
            if (isFinalRendering) throw MissingFrameException("Text-Mesh (383) $font: '$text'")
            needsUpdate = true
            return
        }

        textMesh.draw(startIndex, endIndex) { buffer, _, xOffset ->
            buffer!!
            val offset = Vector3f(lineDeltaX + xOffset, lineDeltaY, 0f)
            if (firstTimeDrawing) {
                draw3DText(this, time, offset, stack, buffer, color)
                firstTimeDrawing = false
            } else {
                draw3DTextWithOffset(buffer, offset)
            }
        }

    }

    override fun transformLocally(pos: Vector3f, time: Double): Vector3f {
        return pos
    }

    fun invalidate() {
        lastVisualState = null
        needsUpdate = true
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveWithoutSuper(writer)
    }

    fun saveWithoutSuper(writer: BaseWriter){
        writer.writeString("text", text)
        writer.writeString("font", font.name)
        writer.writeBoolean("isItalic", font.isItalic, true)
        writer.writeBoolean("isBold", font.isBold, true)
        writer.writeObject(this, "relativeLineSpacing", relativeLineSpacing)
        writer.writeInt("textAlignment", textAlignment.id, true)
        writer.writeInt("blockAlignmentX", blockAlignmentX.id, true)
        writer.writeInt("blockAlignmentY", blockAlignmentY.id, true)
        writer.writeFloat("relativeTabSize", relativeTabSize, true)
        writer.writeFloat("lineBreakWidth", lineBreakWidth)
        writer.writeFloat("relativeCharSpacing", relativeCharSpacing)
        writer.writeInt("renderingMode", renderingMode.id)
        writer.writeObject(this, "outlineColor0", outlineColor0)
        writer.writeObject(this, "outlineColor1", outlineColor1)
        writer.writeObject(this, "outlineColor2", outlineColor2)
        writer.writeObject(this, "outlineWidths", outlineWidths)
        writer.writeObject(this, "outlineSmoothness", outlineSmoothness)
        writer.writeBoolean("roundSDFCorners", roundSDFCorners)
        writer.writeObject(this, "startCursor", startCursor)
        writer.writeObject(this, "endCursor", endCursor)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "textAlignment" -> textAlignment = AxisAlignment.find(value) ?: textAlignment
            "blockAlignmentX" -> blockAlignmentX = AxisAlignment.find(value) ?: blockAlignmentX
            "blockAlignmentY" -> blockAlignmentY = AxisAlignment.find(value) ?: blockAlignmentY
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
            "text" -> text = value
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
            "relativeLineSpacing" -> relativeLineSpacing.copyFrom(value)
            "outlineColor0" -> outlineColor0.copyFrom(value)
            "outlineColor1" -> outlineColor1.copyFrom(value)
            "outlineColor2" -> outlineColor2.copyFrom(value)
            "outlineWidths" -> outlineWidths.copyFrom(value)
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
    ) {

        list += TextInputML("Text", style, text)
            .setChangeListener {
                RemsStudio.incrementalChange("text") {
                    getSelfWithShadows().forEach { c -> c.text = it }
                }
            }
            .setIsSelectedListener { show(null) }
        val fontList = ArrayList<String>()
        fontList += font.name
        fontList += menuSeparator

        fun sortFavourites() {
            fontList.sort()
            val lastUsedSet = lastUsedFonts.toHashSet()
            fontList.sortByDescending { if (it == menuSeparator) 1 else if (it in lastUsedSet) 2 else 0 }
        }

        FontManager.requestFontList { systemFonts ->
            synchronized(fontList) {
                fontList += systemFonts.filter { it != font.name }
                sortFavourites()
            }
        }

        // todo general favourites for all enum types?
        // todo at least a generalized form to make it simpler?

        val fontGroup = getGroup("Font", "", "font")

        fontGroup += EnumInput(
            "Font Name",
            "The style of the text",
            "obj.font.name",
            font.name,
            fontList.map { NameDesc(it) },
            style
        )
            .setChangeListener { it, _, _ ->
                RemsStudio.largeChange("Change Font to '$it'") {
                    getSelfWithShadows().forEach { c -> c.font = c.font.withName(it) }
                }
                invalidate()
                putLastUsedFont(it)
                sortFavourites()
            }
            .setIsSelectedListener { show(null) }
        fontGroup += BooleanInput("Italic", font.isItalic, style)
            .setChangeListener {
                RemsStudio.largeChange("Italic: $it") {
                    getSelfWithShadows().forEach { c -> c.font = c.font.withItalic(it) }
                }
                invalidate()
            }
            .setIsSelectedListener { show(null) }
        fontGroup += BooleanInput("Bold", font.isBold, style)
            .setChangeListener {
                RemsStudio.largeChange("Bold: $it") {
                    getSelfWithShadows().forEach { c -> c.font = c.font.withBold(it) }
                }
                invalidate()
            }
            .setIsSelectedListener { show(null) }

        val alignGroup = getGroup("Alignment", "", "alignment")
        fun align(title: String, value: AxisAlignment, xAxis: Boolean, set: (self: Text, AxisAlignment) -> Unit) {
            operator fun AxisAlignment.get(x: Boolean) = if (x) xName else yName
            alignGroup += EnumInput(
                title, true,
                value[xAxis],
                AxisAlignment.values().map { NameDesc(it[xAxis]) }, style
            )
                .setIsSelectedListener { show(null) }
                .setChangeListener { name, _, _ ->
                    val alignment = AxisAlignment.values().first { it[xAxis] == name }
                    RemsStudio.largeChange("Set $title to $name") {
                        getSelfWithShadows().forEach { set(it, alignment) }
                    }
                    invalidate()
                }
        }

        align("Text Alignment", textAlignment, true) { self, it -> self.textAlignment = it }
        align("Block Alignment X", blockAlignmentX, true) { self, it -> self.blockAlignmentX = it }
        align("Block Alignment Y", blockAlignmentY, false) { self, it -> self.blockAlignmentY = it }

        val spaceGroup = getGroup("Spacing", "", "spacing")
        // make this element separable from the parent???
        spaceGroup += vi(
            "Character Spacing",
            "Space between individual characters",
            "text.characterSpacing",
            null, relativeCharSpacing, style
        ) {
            RemsStudio.incrementalChange("char space") { relativeCharSpacing = it }
            invalidate()
        }
        spaceGroup += vi(
            "Line Spacing",
            "How much lines are apart from each other",
            "text.lineSpacing",
            relativeLineSpacing, style
        )
        spaceGroup += vi(
            "Tab Size", "Relative tab size, in widths of o's", "text.tabSpacing",
            tabSpaceType, relativeTabSize, style
        ) {
            RemsStudio.incrementalChange("tab size") { relativeTabSize = it }
            invalidate()
        }
        spaceGroup += vi(
            "Line Break Width",
            "How broad the text shall be, at maximum; < 0 = no limit", "text.widthLimit",
            lineBreakType, lineBreakWidth, style
        ) {
            RemsStudio.incrementalChange("line break width") { lineBreakWidth = it }
            invalidate()
        }

        val ops = getGroup("Operations", "", "operations")
        ops += TextButton("Create Shadow", false, style)
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
                RemsStudio.largeChange("Add Text Shadow") { addChild(shadow) }
                selectTransform(shadow)
            }

        val rpgEffects = getGroup("RPG Effects", "", "rpg-effects")
        rpgEffects += vi("Start Cursor", "The first character index to be drawn", startCursor, style)
        rpgEffects += vi("End Cursor", "The last character index to be drawn; -1 = unlimited", endCursor, style)

        val outline = getGroup("Outline", "", "outline")
        outline.setTooltip("Needs Rendering Mode = SDF or Merged SDF")
        outline += vi(
            "Rendering Mode",
            "Mesh: Sharp, Signed Distance Fields: with outline", "text.renderingMode",
            null, renderingMode, style
        ) { renderingMode = it }
        outline += vi("Color 1", "First Outline Color", "outline.color1", outlineColor0, style)
        outline += vi("Color 2", "Second Outline Color", "outline.color2", outlineColor1, style)
        outline += vi("Color 3", "Third Outline Color", "outline.color3", outlineColor2, style)
        outline += vi("Widths", "[Main, 1st, 2nd, 3rd]", "outline.widths", outlineWidths, style)
        outline += vi(
            "Smoothness",
            "How smooth the edge is, [Main, 1st, 2nd, 3rd]",
            "outline.smoothness",
            outlineSmoothness,
            style
        )
        outline += vi("Rounded Corners", "Makes corners curvy", "outline.roundCorners", null, roundSDFCorners, style) {
            roundSDFCorners = it
            invalidate()
        }

    }

    fun getSelfWithShadows() = getShadows() + this
    fun getShadows() = children.filter { it.name.contains("shadow", true) && it is Text } as List<Text>
    override fun passesOnColor() = false // otherwise white shadows of black text wont work

    override fun getClassName(): String = "Text"
    override fun getDefaultDisplayName() = Dict["Text", "obj.text"]
    override fun getSymbol() = DefaultConfig["ui.symbol.text", "\uD83D\uDCC4"]

    companion object {

        val tabSpaceType = Type(4f, 1, 1f, true, true, { max(it as Float, 0f) }, ::castToFloat)
        val lineBreakType = Type(-1f, 1, 1f, true, true, { it as Float }, ::castToFloat)

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

}