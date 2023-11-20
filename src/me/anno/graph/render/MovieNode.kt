package me.anno.graph.render

import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.ui.GraphPanel
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.FileInput
import me.anno.ui.Style
import org.joml.Vector2f
import org.joml.Vector4f

class MovieNode : CalculationNode(
    "Movie",
    // todo different color repeat modes
    listOf("Vector2f", "UV", "Boolean", "Linear", "Float", "ConstTime(s)"),
    listOf("Vector4f", "Color")
) {

    init {
        setInput(0, Vector2f())
        setInput(1, true)
        setInput(2, 0f)
    }

    var file: FileReference = InvalidRef

    override fun calculate(): Vector4f {
        throw IllegalArgumentException("Operations is not supported")
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
        writer.writeFile("textures/fileExplorer", file)
    }

    override fun readFile(name: String, value: FileReference) {
        if (name == "textures/fileExplorer") file = value
        else super.readFile(name, value)
    }

}