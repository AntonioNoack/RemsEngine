package me.anno.graph.visual.render

import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.FlowGraphNodeUtils.getBoolInput
import me.anno.graph.visual.FlowGraphNodeUtils.getIntInput
import me.anno.ui.editor.graph.GraphPanel
import me.anno.image.ImageCache
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.FileInput
import me.anno.ui.Style
import me.anno.utils.Color.black4
import me.anno.utils.Color.toVecRGBA
import org.joml.Vector2f
import org.joml.Vector4f

class TextureNode : CalculationNode(
    "Texture",
    listOf(
        "Vector2f", "UV",
        "Boolean", "Linear",
        "Int", "Clamp/Repeat/MRepeat"
    ),
    listOf("Vector4f", "Color")
) {

    companion object {
        val black = black4
        val violet = Vector4f(1f, 0f, 1f, 1f)
    }

    init {
        setInput(0, Vector2f())
        setInput(1, true)
        setInput(2, 0)
    }

    var file: FileReference = InvalidRef

    override fun calculate(): Vector4f {
        val file = file
        val uv = getInput(0) as Vector2f
        val image = ImageCache[file, false]
        return if (image != null) {
            val linear = if (getBoolInput(1)) Filtering.LINEAR else Filtering.NEAREST
            val clamping = when (getIntInput(2)) {
                1 -> Clamping.REPEAT
                2 -> Clamping.MIRRORED_REPEAT
                else -> Clamping.CLAMP
            }
            image.sampleRGB(uv.x, uv.y, linear, clamping).toVecRGBA()
        } else {
            if ((uv.x + uv.y) % 1f > 0.5f) black else violet
        }
    }

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        list += FileInput("", style, file, emptyList())
            .addChangeListener {
                file = it
                g.onChange(false)
            }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "file" -> file = value as? FileReference ?: InvalidRef
            else -> super.setProperty(name, value)
        }
    }
}