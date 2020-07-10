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
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

class Text(text: String = "", parent: Transform? = null): GFXTransform(parent){

    var text = text.replace("\r", "")
        set(value) {
            field = value.replace("\r", "")
        }

    // todo automatic line break after length x

    // todo blurry, colorful text shadow
    // todo multiple line alignment


    // how we apply sampling probably depends on our AA solution...

    // parameters
    var font = "Verdana"

    var isBold = false
    var isItalic = false

    var textAlignment = AxisAlignment.MIN

    val relativeLineSpacing = AnimatedProperty.float().set(1f)

    // caching
    var lastText = text
    var lines = text.split('\n')
    var keys = lines.map { FontMeshKey(font, isBold, isItalic, it) }

    data class FontMeshKey(
        val fontName: String, val isBold: Boolean, val isItalic: Boolean,
        val text: String){
        fun equals(fontName: String, isBold: Boolean, isItalic: Boolean, text: String) =
            fontName == this.fontName && isBold == this.isBold && isItalic == this.isItalic && text == this.text
    }

    var minX = 0f
    var maxX = 0f

    override fun onDraw(stack: Matrix4fArrayList, time: Float, color: Vector4f){

        val text = text
        val isBold = isBold
        val isItalic = isItalic
        val font = font

        if(text.isNotBlank()){

            var wasChanged = false
            if(text != lastText){
                wasChanged = true
                lastText = text
                lines = text.split('\n')
                keys = lines.map { FontMeshKey(font, isBold, isItalic, it) }
            } else {
                // !none = at least one?, is not equal, so needs update
                if(!keys.withIndex().none { (index, fontMeshKey) ->
                        !fontMeshKey.equals(font, isBold, isItalic, lines[index])
                    }){
                    wasChanged = true
                    keys = lines.map { FontMeshKey(font, isBold, isItalic, it) }
                }
            }

            val lineOffset = - DEFAULT_LINE_HEIGHT * relativeLineSpacing[time]
            val alignment = textAlignment

            // min and max x are cached for long texts with thousands of lines (not really relevant)
            if(alignment != AxisAlignment.CENTER && (wasChanged || minX >= maxX)){
                minX = Float.POSITIVE_INFINITY
                maxX = Float.NEGATIVE_INFINITY
                keys.forEach { fontMeshKey ->
                    // todo async font mesh calculation...
                    if(fontMeshKey.text.isNotEmpty()){
                        val fontMesh = Cache.getEntry(fontMeshKey, fontMeshTimeout){
                            val awtFont = FontManager.getFont(font, 20f, isBold, isItalic)
                            val buffer = FontMesh((awtFont as AWTFont).font, fontMeshKey.text)
                            buffer
                        } as FontMesh
                        minX = min(minX, fontMesh.minX)
                        maxX = max(maxX, fontMesh.maxX)
                    }
                }
            }

            keys.forEach { fontMeshKey ->

                if(fontMeshKey.text.isNotEmpty()){

                    val fontMesh = Cache.getEntry(fontMeshKey, fontMeshTimeout){
                        val awtFont = FontManager.getFont(font, 20f, isBold, isItalic)
                        val buffer = FontMesh((awtFont as AWTFont).font, fontMeshKey.text)
                        buffer
                    } as FontMesh

                    val offset = when(alignment){
                        AxisAlignment.MIN -> 2 * (minX - fontMesh.minX)
                        AxisAlignment.CENTER -> 0f
                        AxisAlignment.MAX -> 2 * (maxX - fontMesh.maxX)
                    }

                    if(offset != 0f) stack.translate(+offset, 0f, 0f)

                    GFX.draw3D(stack, fontMesh.buffer, GFX.whiteTexture, color, isBillboard[time], true, null)

                    if(offset != 0f) stack.translate(-offset, 0f, 0f)

                }

                stack.translate(0f, lineOffset, 0f)

            }

            // todo alignment
            // todo left/right alignment xD

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
        writer.writeInt("textAlignment", when(textAlignment){
            AxisAlignment.MIN -> -1
            AxisAlignment.CENTER -> 0
            AxisAlignment.MAX -> +1
        })
    }

    override fun readInt(name: String, value: Int) {
        when(name){
            "textAlignment" -> {
                textAlignment = when(value){
                    -1 -> AxisAlignment.MIN
                     0 -> AxisAlignment.CENTER
                    +1 -> AxisAlignment.MAX
                    else -> textAlignment
                }
            }
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

        list += EnumInput("Text Alignment", true,
            textAlignmentNames.reverse[textAlignment]!!,
            textAlignmentNames.entries.sortedBy { it.value.ordinal }.map { it.key }, style)
            .setIsSelectedListener { show(null) }
            .setChangeListener { textAlignment = textAlignmentNames[it] ?: textAlignment }

        list += VI("Line Spacing", "How much lines are apart from each other", relativeLineSpacing, style)

    }

    override fun getClassName(): String = "Text"

    companion object {

        val textAlignmentNames = BiMap<String, AxisAlignment>(3)
        init {
            textAlignmentNames["Left"] = AxisAlignment.MIN
            textAlignmentNames["Center"] = AxisAlignment.CENTER
            textAlignmentNames["Right"] = AxisAlignment.MAX
        }

        // todo save the last used fonts? yes :)
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