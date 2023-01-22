package me.anno.graph.render

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.ui.GraphEditor
import me.anno.image.ImageCPUCache
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.FileInput
import me.anno.ui.style.Style
import me.anno.utils.Color.black4
import me.anno.utils.Color.toVecRGBA
import org.joml.Vector2f
import org.joml.Vector4f

class TextureNode : CalculationNode(
    "Texture",
    // todo different color repeat modes
    listOf("Vector2f", "UV", "Boolean", "Linear"),
    listOf("Vector4f", "Color")
) {

    companion object {
        val black = black4
        val violet = Vector4f(1f, 0f, 1f, 1f)
    }

    init {
        setInput(0, Vector2f())
        setInput(1, true)
    }

    var file: FileReference = InvalidRef

    override fun calculate(graph: FlowGraph): Vector4f {
        val file = file
        val uv = getInput(graph, 0) as Vector2f
        val image = ImageCPUCache[file, false]
        return if (image != null) {
            val linear = getInput(graph, 1) == true
            // todo support linear sampling
            val x = Maths.clamp((Maths.fract(uv.x) * image.width).toInt(), 0, image.width - 1)
            val y = Maths.clamp((Maths.fract(uv.y) * image.height).toInt(), 0, image.height - 1)
            val c = image.getRGB(x, y)
            c.toVecRGBA()
        } else {
            if ((uv.x + uv.y) % 1f > 0.5f) black else violet
        }
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
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