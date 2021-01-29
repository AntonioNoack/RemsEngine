package me.anno.objects.particles

import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import me.anno.objects.text.Text
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.types.Strings.joinChars
import org.joml.Vector3f
import kotlin.streams.toList

class TextParticles : ParticleSystem() {

    val text = object : Text() {
        override fun getApproxSize(): Int = this@TextParticles.getApproxSize()
    }

    override fun needsChildren() = false

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        text.createInspectorWithoutSuper(list, style, getGroup)
    }

    override fun createParticle(index: Int, time: Double): Particle? {

        val char = text.text.codePoints().toList().getOrNull(index) ?: return null

        val str = listOf(char).joinChars()
        val clone = text.clone() as Text
        clone.text = str

        val particle = super.createParticle(index, time)!!
        particle.type = clone

        // get position and add to original particle
        var px = 0f
        var py = 0f

        text.apply {

            val isBold = font.isBold
            val isItalic = font.isItalic

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
            val scaleX = TextMesh.DEFAULT_LINE_HEIGHT / (exampleLayout.ascent + exampleLayout.descent)
            val scaleY = 1f / (exampleLayout.ascent + exampleLayout.descent)
            val width = lineSegmentsWithStyle.width * scaleX
            val height = lineSegmentsWithStyle.height * scaleY

            val lineOffset = -TextMesh.DEFAULT_LINE_HEIGHT * relativeLineSpacing[time]
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

            var charIndex = 0

            forceVariableBuffer = true

            partSearch@ for ((partIndex, part) in lineSegmentsWithStyle.parts.withIndex()) {

                val startIndex = charIndex
                val partLength = part.codepointLength
                val endIndex = charIndex + partLength

                if (index in startIndex until endIndex) {
                    val offsetX = when (textAlignment) {
                        AxisAlignment.MIN -> 0f
                        AxisAlignment.CENTER -> (width - part.lineWidth * scaleX) / 2f
                        AxisAlignment.MAX -> (width - part.lineWidth * scaleX)
                    }

                    val lineDeltaX = dx + part.xPos * scaleX + offsetX
                    val lineDeltaY = dy + part.yPos * scaleY * lineOffset

                    val key = keys[partIndex]
                    val textMesh = getTextMesh(key)!! as TextMeshGroup

                    val di = index - startIndex
                    val xOffset = (textMesh.offsets[di] + textMesh.offsets[di + 1]).toFloat() * 0.5f
                    val offset = Vector3f(lineDeltaX + xOffset, lineDeltaY, 0f)
                    px = offset.x / 100f
                    py = offset.y

                    break@partSearch

                }

                charIndex = endIndex + 1 // '\n'

            }
        }

        particle.states.first().position.add(px, py, 0f)
        return particle

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        text.saveWithoutSuper(writer)
    }

    override fun getSystemState(): Any? {
        return super.getSystemState() to TextWriter.toText(text, false)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "relativeLineSpacing", "outlineColor0",
            "outlineColor1", "outlineColor2",
            "outlineWidths", "outlineSmoothness",
            "startCursor", "endCursor" ->
                text.readObject(name, value)
            else -> super.readObject(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "textAlignment", "blockAlignmentX", "blockAlignmentY",
            "renderingMode" ->
                text.readInt(name, value)
            else -> super.readInt(name, value)
        }
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "isItalic", "isBold", "roundSDFCorners" ->
                text.readBoolean(name, value)
            else -> super.readBoolean(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "text", "font" ->
                text.readString(name, value)
            else -> super.readString(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "lineBreakWidth", "relativeTabSize" ->
                text.readFloat(name, value)
            else -> super.readFloat(name, value)
        }
    }

    override fun getDefaultDisplayName(): String = "Text Particles"
    override fun getClassName() = "TextParticles"

}