package me.anno.objects

import me.anno.config.DefaultConfig
import me.anno.fonts.AWTFont
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.FontMesh
import me.anno.fonts.mesh.FontMesh.Companion.DEFAULT_LINE_HEIGHT
import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.cache.Cache
import me.anno.studio.Studio.selectedProperty
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import me.anno.utils.BiMap
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
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
    // done multiple line alignment
    // todo working tabs



    // how we apply sampling probably depends on our AA solution...

    // parameters
    var font = "Verdana"

    var isBold = false
    var isItalic = false

    var textAlignment = AxisAlignment.MIN
    var blockAlignmentX = AxisAlignment.MIN
    var blockAlignmentY = AxisAlignment.MIN

    enum class TextMode {
        RAW,
        HTML,
        MARKDOWN
    }

    var textMode = TextMode.RAW

    val relativeLineSpacing = AnimatedProperty.float(1f)

    // caching
    var lastText = text
    // todo allow style by HTML/.md? :D
    var lineSegmentsWithStyle = text.split('\n')
    var keys = lineSegmentsWithStyle.map { FontMeshKey(font, isBold, isItalic, it) }

    var minX = 0.0
    var maxX = 0.0

    var hasMeasured = false

    // todo tabsegment??
    class TextSegment(){

    }

    fun splitSegments(){

    }

    data class FontMeshKey(
        val fontName: String, val isBold: Boolean, val isItalic: Boolean,
        val text: String){
        fun equals(fontName: String, isBold: Boolean, isItalic: Boolean, text: String) =
            fontName == this.fontName && isBold == this.isBold && isItalic == this.isItalic && text == this.text
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f){

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
                lineSegmentsWithStyle = text.split('\n')
                keys = lineSegmentsWithStyle.map { FontMeshKey(font, isBold, isItalic, it) }
            } else {
                // !none = at least one?, is not equal, so needs update
                if(!keys.withIndex().none { (index, fontMeshKey) ->
                        !fontMeshKey.equals(font, isBold, isItalic, lineSegmentsWithStyle[index])
                    }){
                    wasChanged = true
                    hasMeasured = false
                    keys = lineSegmentsWithStyle.map { FontMeshKey(font, isBold, isItalic, it) }
                }
            }

            val lineOffset = - DEFAULT_LINE_HEIGHT * relativeLineSpacing[time]
            val alignment = textAlignment

            val awtFont = FontManager.getFont(font, 20f, isBold, isItalic)

            // min and max x are cached for long texts with thousands of lines (not really relevant)
            if((wasChanged || minX >= maxX || !hasMeasured)){
                minX = Double.POSITIVE_INFINITY
                maxX = Double.NEGATIVE_INFINITY
                hasMeasured = true
                keys.forEach { fontMeshKey ->
                    if(fontMeshKey.text.isNotBlank()){
                        val fontMesh = Cache.getEntry(fontMeshKey, fontMeshTimeout, true){
                            val buffer = FontMesh((awtFont as AWTFont).font, fontMeshKey.text)
                            buffer
                        } as? FontMesh
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

            // todo actual text height vs baseline? for height

            val totalHeight = lineOffset * lineSegmentsWithStyle.size

            stack.translate(
                when(blockAlignmentX){
                    AxisAlignment.MIN -> + (maxX - minX).toFloat()
                    AxisAlignment.CENTER -> 0f
                    AxisAlignment.MAX -> - (maxX - minX).toFloat()
                }, when(blockAlignmentY){
                    AxisAlignment.MIN -> 0f
                    AxisAlignment.CENTER -> - totalHeight * 0.5f
                    AxisAlignment.MAX -> - totalHeight
                } + lineOffset/2, 0f
            )

            for(fontMeshKey in keys){

                if(fontMeshKey.text.isNotBlank()){

                    val fontMesh = Cache.getEntry(fontMeshKey, fontMeshTimeout, true){
                        val buffer = FontMesh((awtFont as AWTFont).font, fontMeshKey.text)
                        buffer
                    } as? FontMesh ?: continue

                    val offset = when(alignment){
                        AxisAlignment.MIN -> 2 * (minX - fontMesh.minX).toFloat()
                        AxisAlignment.CENTER -> 0f
                        AxisAlignment.MAX -> 2 * (maxX - fontMesh.maxX).toFloat()
                    }

                    if(offset != 0f) stack.translate(+offset, 0f, 0f)

                    GFX.draw3D(stack, fontMesh.buffer, GFX.whiteTexture, color, isBillboard[time], true, null)

                    if(offset != 0f) stack.translate(-offset, 0f, 0f)

                }

                stack.translate(0f, lineOffset, 0f)

            }

            // todo calculate (signed) distance fields for different kinds of shadows from the mesh
            // bad solution for blurred shadows
            /*color.w *= 0.001f
            val random = Random()
            for(i in 0 until 1000){
                stack.pushMatrix()
                val radius = 0.01f
                stack.translate(radius * random.nextGaussian().toFloat(), radius * random.nextGaussian().toFloat(), 0f)
                GFX.draw3D(stack, buffer.buffer, GFX.whiteTexture, color, isBillboard[time], true)
                stack.popMatrix()
            }*/

        }

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("text", text)
        writer.writeString("font", font)
        writer.writeBool("isItalic", isItalic, true)
        writer.writeBool("isBold", isBold, true)
        writer.writeObject(this, "relativeLineSpacing", relativeLineSpacing)
        writer.writeInt("textAlignment", textAlignment.id)
        writer.writeInt("blockAlignmentX", blockAlignmentX.id)
        writer.writeInt("blockAlignmentY", blockAlignmentY.id)
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "textAlignment"   -> textAlignment   = AxisAlignment.values().firstOrNull { it.id == value } ?: textAlignment
            "blockAlignmentX" -> blockAlignmentX = AxisAlignment.values().firstOrNull { it.id == value } ?: blockAlignmentX
            "blockAlignmentY" -> blockAlignmentY = AxisAlignment.values().firstOrNull { it.id == value } ?: blockAlignmentY
            else -> super.readInt(name, value)
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
            .setChangeListener { text = it }
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
                font = it }
            .setIsSelectedListener { show(null) }

        list += BooleanInput("Italic", isItalic, style)
            .setChangeListener { isItalic = it }
            .setIsSelectedListener { show(null) }

        list += BooleanInput("Bold", isBold, style)
            .setChangeListener { isBold = it }
            .setIsSelectedListener { show(null) }

        val alignmentValues = alignmentNamesX.entries.sortedBy { it.value.ordinal }.map { it.key }

        list += EnumInput("Text Alignment", true,
            alignmentNamesX.reverse[textAlignment]!!,
            alignmentValues, style)
            .setIsSelectedListener { show(null) }
            .setChangeListener { textAlignment = alignmentNamesX[it]!! }

        list += EnumInput("Block Alignment X", true,
            alignmentNamesX.reverse[blockAlignmentX]!!,
            alignmentValues, style)
            .setIsSelectedListener { show(null) }
            .setChangeListener { blockAlignmentX = alignmentNamesX[it]!! }

        list += EnumInput("Block Alignment Y", true,
            alignmentNamesX.reverse[blockAlignmentY]!!,
            alignmentValues, style)
            .setIsSelectedListener { show(null) }
            .setChangeListener { blockAlignmentY = alignmentNamesX[it]!! }

        list += VI("Line Spacing", "How much lines are apart from each other", relativeLineSpacing, style)

    }

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