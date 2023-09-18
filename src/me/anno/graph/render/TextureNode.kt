package me.anno.graph.render

import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.ui.GraphPanel
import me.anno.image.ImageCPUCache
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
        val image = ImageCPUCache[file, false]
        return if (image != null) {
            val linear = if (getInput(1) == true) GPUFiltering.LINEAR else GPUFiltering.NEAREST
            val clamping = when (getInput(2) as Int) {
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
            .setChangeListener {
                file = it
                g.onChange(false)
            }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
    }

    override fun readFile(name: String, value: FileReference) {
        if (name == "file") file = value
        else super.readFile(name, value)
    }

}