package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.fonts.AWTFont
import me.anno.fonts.FontManager
import me.anno.fonts.PartResult
import me.anno.fonts.mesh.FontMesh
import me.anno.fonts.mesh.FontMesh.Companion.DEFAULT_LINE_HEIGHT
import me.anno.fonts.mesh.FontMeshBase
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.studio.Studio.selectedProperty
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.color.ColorSpace.Companion.HSLuv
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import me.anno.utils.BiMap
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import java.awt.Font
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

// todo background "color" in the shape of a plane? for selections and such
class Text(text: String = "", parent: Transform? = null): GFXTransform(parent){

    val backgroundColor = AnimatedProperty.color()

    var text = text.replace("\r", "")
        set(value) {
            field = value.replace("\r", "")
        }

    // todo automatic line break after length x

    // todo blurry, colorful text shadow
    // todo by calculated distance fields:
    // todo just project the points outwards xD
    // todo and create the shadow on a (linked?) child element

    // done multiple line alignment
    // todo working tabs



    // how we apply sampling probably depends on our AA solution...

    // parameters
    var font = "Verdana"
    var lastFont = font

    var isBold = false
    var isItalic = false

    var textAlignment = AxisAlignment.MIN
    var blockAlignmentX = AxisAlignment.MAX
    var blockAlignmentY = AxisAlignment.MIN

    enum class TextMode {
        RAW,
        HTML,
        MARKDOWN
    }

    var textMode = TextMode.RAW

    var relativeLineSpacing = AnimatedProperty.float(1f)

    var relativeTabSize = 4f

    // caching
    var lastText = text

    // todo allow style by HTML/.md? :D
    var lineSegmentsWithStyle = splitSegments(text)
    var keys = createKeys()

    var minX = 0.0
    var maxX = 0.0

    var hasMeasured = false

    fun createKeys() = lineSegmentsWithStyle.parts.map { FontMeshKey(it.font, isBold, isItalic, it.text) }

    fun splitSegments(text: String): PartResult {
        val fontSize0 = 20f
        val awtFont = FontManager.getFont(font, fontSize0, isBold, isItalic) as AWTFont
        return awtFont.splitParts(text, fontSize0, relativeTabSize)
    }

    data class FontMeshKey(
        val font: Font, val isBold: Boolean, val isItalic: Boolean,
        val text: String){
        fun equals(isBold: Boolean, isItalic: Boolean, text: String) =
            isBold == this.isBold && isItalic == this.isItalic && text == this.text
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f){

        stack.pushMatrix()

        val text = text
        val isBold = isBold
        val isItalic = isItalic
        val font = font

        if(text.isNotBlank()){

            var wasChanged = false
            if(text != lastText){
                wasChanged = true
                hasMeasured = false
                lastText = text
                lastFont = font
                lineSegmentsWithStyle = splitSegments(text)
                keys = createKeys()
            } else {
                // !none = at least one?, is not equal, so needs update
                if(font != lastFont || !keys.withIndex().none { (index, fontMeshKey) ->
                        !fontMeshKey.equals(isBold, isItalic, lineSegmentsWithStyle.parts[index].text)
                    }){
                    wasChanged = true
                    hasMeasured = false
                    lastFont = font
                    keys = createKeys()
                }
            }

            val exampleLayout = lineSegmentsWithStyle.exampleLayout
            val scaleX = DEFAULT_LINE_HEIGHT / (exampleLayout.ascent + exampleLayout.descent)
            val scaleY = 1f / (exampleLayout.ascent + exampleLayout.descent)
            val width = lineSegmentsWithStyle.width * scaleX
            val height = lineSegmentsWithStyle.height * scaleY // todo why is this incorrect

            val lineOffset = - DEFAULT_LINE_HEIGHT * relativeLineSpacing[time]
            val textAlignment = textAlignment

            fun getFontMesh(fontMeshKey: FontMeshKey): FontMeshBase? {
                return Cache.getEntry(fontMeshKey, fontMeshTimeout, true){
                    FontMesh(fontMeshKey.font, fontMeshKey.text)
                } as? FontMeshBase
            }

            // min and max x are cached for long texts with thousands of lines (not really relevant)
            if((wasChanged || minX >= maxX || !hasMeasured)){
                minX = Double.POSITIVE_INFINITY
                maxX = Double.NEGATIVE_INFINITY
                hasMeasured = true
                keys.forEach { fontMeshKey ->
                    if(fontMeshKey.text.isNotBlank()){
                        val fontMesh = getFontMesh(fontMeshKey)
                        if(fontMesh == null){
                            hasMeasured = false
                            if(GFX.isFinalRendering) throw MissingFrameException(null)
                        } else {
                            if(fontMesh.minX.isFinite() && fontMesh.maxX.isFinite()){
                                minX = min(minX, fontMesh.minX)
                                maxX = max(maxX, fontMesh.maxX)
                            }// else empty?
                        }
                    }
                }
            }

            // todo set correct min and max...
            minX = 0.0
            maxX = width.toDouble() / 2 // why???

            // todo actual text height vs baseline? for height

            val totalHeight = lineOffset * height

            stack.translate(
                when(blockAlignmentX){
                    AxisAlignment.MIN -> -width
                    AxisAlignment.CENTER -> - width/2
                    AxisAlignment.MAX -> 0f
                }, when(blockAlignmentY){
                    AxisAlignment.MIN -> 0f
                    AxisAlignment.CENTER -> - totalHeight * 0.5f
                    AxisAlignment.MAX -> - totalHeight
                } + lineOffset/2, 0f
            )

            for((index, value) in lineSegmentsWithStyle.parts.withIndex()){

                val fontMeshKey = keys[index]

                val fontMesh = getFontMesh(fontMeshKey) ?: continue

                val offsetX = when(textAlignment){
                    AxisAlignment.MIN -> 0f
                    AxisAlignment.CENTER -> (width - value.lineWidth * scaleX) / 2f
                    AxisAlignment.MAX -> (width - value.lineWidth * scaleX)
                }// - 2 * fontMesh.minX.toFloat()

                stack.pushMatrix()

                stack.translate(value.xPos * scaleX + offsetX, value.yPos * scaleY * lineOffset, 0f)

                fontMesh.draw(stack){ buffer ->
                    GFX.draw3D(stack, buffer, GFX.whiteTexture, color, isBillboard[time], true, null)
                }

                stack.popMatrix()

            }

            // todo calculate (signed) distance fields for different kinds of shadows from the mesh

        }

        stack.popMatrix()

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
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "textAlignment"   -> textAlignment   = AxisAlignment.values().firstOrNull { it.id == value } ?: textAlignment
            "blockAlignmentX" -> blockAlignmentX = AxisAlignment.values().firstOrNull { it.id == value } ?: blockAlignmentX
            "blockAlignmentY" -> blockAlignmentY = AxisAlignment.values().firstOrNull { it.id == value } ?: blockAlignmentY
            else -> super.readInt(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when(name){
            "relativeTabSize" -> relativeTabSize = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when(name){
            "text" -> text = value
            "font" -> font = value
            else -> super.readString(name, value)
        }
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "isBold" -> isBold = value
            "isItalic" -> isItalic = value
            else -> super.readBool(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "relativeLineSpacing" -> relativeLineSpacing.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += TextInputML("Text", style, text)
            .setChangeListener { getSelfWithShadows().forEach { c -> c.text = it } }
            .setIsSelectedListener { selectedProperty = null }
        val fontList = ArrayList<String>()
        fontList += font
        fontList += GFX.menuSeparator

        fun sortFavourites(){
            fontList.sort()
            val lastUsedSet = lastUsedFonts.toHashSet()
            fontList.sortByDescending { if(it == GFX.menuSeparator) 1 else if(it in lastUsedSet) 2 else 0 }
        }

        FontManager.requestFontList { systemFonts ->
            synchronized(fontList){
                fontList += systemFonts.filter { it != font }
                sortFavourites()
            }
        }

        // todo general favourites for all enum types?
        // todo at least a generalized form to make it simpler?
        list += EnumInput("Font", true, font, fontList, style)
            .setChangeListener {
                putLastUsedFont(it)
                sortFavourites()
                getSelfWithShadows().forEach { c -> c.font = it } }
            .setIsSelectedListener { show(null) }

        list += BooleanInput("Italic", isItalic, style)
            .setChangeListener { getSelfWithShadows().forEach { c -> c.isItalic = it } }
            .setIsSelectedListener { show(null) }

        list += BooleanInput("Bold", isBold, style)
            .setChangeListener { getSelfWithShadows().forEach { c -> c.isBold = it } }
            .setIsSelectedListener { show(null) }

        val alignmentValues = alignmentNamesX.entries.sortedBy { it.value.ordinal }.map { it.key }

        list += EnumInput("Text Alignment", true,
            alignmentNamesX.reverse[textAlignment]!!,
            alignmentValues, style)
            .setIsSelectedListener { show(null) }
            .setChangeListener { name ->
                val alignment = alignmentNamesX[name]!!
                getSelfWithShadows().forEach { it.textAlignment = alignment }
            }

        list += EnumInput("Block Alignment X", true,
            alignmentNamesX.reverse[blockAlignmentX]!!,
            alignmentValues, style)
            .setIsSelectedListener { show(null) }
            .setChangeListener { name ->
                val alignment = alignmentNamesX[name]!!
                getSelfWithShadows().forEach { it.blockAlignmentX = alignment }
            }

        list += EnumInput("Block Alignment Y", true,
            alignmentNamesX.reverse[blockAlignmentY]!!,
            alignmentValues, style)
            .setIsSelectedListener { show(null) }
            .setChangeListener { name ->
                val alignment = alignmentNamesX[name]!!
                getSelfWithShadows().forEach { it.blockAlignmentY = alignment }
            }

        // make this element separable from the parent???
        list += VI("Line Spacing", "How much lines are apart from each other", relativeLineSpacing, style)
        list += VI("Tab Size", "Relative tab size, in widths of o's", AnimatedProperty.Type.FLOAT_PLUS, relativeTabSize, style){
            relativeTabSize = it
            lastText = "" // to invalidate
        }

        list += ButtonPanel("Create Shadow", style)
            .setSimpleClickListener {
                // such a mess is the result of copying colors from the editor ;)
                val signalColor = Vector4f(HSLuv.toRGB(Vector3f(0.000f,0.934f,0.591f)), 1f)
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
            }

    }

    fun getSelfWithShadows() = getShadows() + this
    fun getShadows() = children.filter { it.name.contains("shadow", true) && it is Text } as List<Text>
    override fun passesOnColor() = false // otherwise white shadows of black text wont work

    override fun getClassName(): String = "Text"


    companion object {

        val alignmentNamesX = BiMap<String, AxisAlignment>(3)
        init {
            alignmentNamesX["Left"] = AxisAlignment.MIN
            alignmentNamesX["Center"] = AxisAlignment.CENTER
            alignmentNamesX["Right"] = AxisAlignment.MAX
        }

        // save the last used fonts? yes :)
        // todo per project? idk
        val fontMeshTimeout = 5000L
        val lastUsedFonts = arrayOfNulls<String>(max(0, DefaultConfig["lastUsed.fonts.count", 5]))
        fun putLastUsedFont(font: String){
            if(lastUsedFonts.isNotEmpty()){
                for(i in lastUsedFonts.indices){
                    if(lastUsedFonts[i] == font) return
                }
                for(i in 0 until lastUsedFonts.lastIndex){
                    lastUsedFonts[i] = lastUsedFonts[i+1]
                }
                lastUsedFonts[lastUsedFonts.lastIndex] = font
            }
        }
    }

}