package me.anno.objects.particles

import me.anno.cache.CacheData
import me.anno.cache.instances.TextCache
import me.anno.cache.keys.TextSegmentKey
import me.anno.fonts.PartResult
import me.anno.fonts.mesh.TextMesh
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.text.TextWriter
import me.anno.objects.text.Text
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.types.Strings.joinChars
import org.joml.Vector3f
import kotlin.streams.toList

class TextParticles : ParticleSystem() {

    val text = object : Text() {
        override val approxSize get() = this@TextParticles.approxSize
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

        val text2 = text.text[time]

        val char = text2.codePoints().toList().getOrNull(index) ?: return null

        val str = listOf(char).joinChars()
        val clone = text.clone() as Text
        clone.text.set(str)

        val particle = super.createParticle(index, time)!!
        particle.type = clone

        // get position and add to original particle
        var px = 0f
        var py = 0f

        text.apply {

            val visualState = getVisualState(text2)
            val data = TextCache.getEntry(visualState, 1000L, false) {
                val segments = splitSegments(text2)
                val keys = createKeys(segments)
                CacheData(segments to keys)
            } as CacheData<*>
            val dataValue = data.value as Pair<*, *>

            val lineSegmentsWithStyle = dataValue.first as PartResult
            val keys = dataValue.second as List<TextSegmentKey>

            val exampleLayout = lineSegmentsWithStyle.exampleLayout
            val scaleX = TextMesh.DEFAULT_LINE_HEIGHT / (exampleLayout.ascent + exampleLayout.descent)
            val scaleY = 1f / (exampleLayout.ascent + exampleLayout.descent)
            val width = lineSegmentsWithStyle.width * scaleX
            val height = lineSegmentsWithStyle.height * scaleY

            val lineOffset = -TextMesh.DEFAULT_LINE_HEIGHT * relativeLineSpacing[time]

            // min and max x are cached for long texts with thousands of lines (not really relevant)
            // actual text height vs baseline? for height

            val totalHeight = lineOffset * height

            val dx = getDrawDX(width, time)
            val dy = getDrawDY(lineOffset, totalHeight, time)

            var charIndex = 0

            forceVariableBuffer = true

            val textAlignment01 = textAlignment[time] * .5f + .5f
            partSearch@ for ((partIndex, part) in lineSegmentsWithStyle.parts.withIndex()) {

                val startIndex = charIndex
                val partLength = part.codepointLength
                val endIndex = charIndex + partLength

                if (index in startIndex until endIndex) {

                    val offsetX = (width - part.lineWidth * scaleX) * textAlignment01

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
            "text",
            "relativeLineSpacing", "outlineColor0",
            "outlineColor1", "outlineColor2",
            "outlineWidths", "outlineSmoothness",
            "startCursor", "endCursor",
            "textAlignment", "blockAlignmentX", "blockAlignmentY" ->
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

    override fun readString(name: String, value: String?) {
        when (name) {
            "text", "font" -> text.readString(name, value)
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

    override val defaultDisplayName: String = "Text Particles"
    override val className get() = "TextParticles"

}