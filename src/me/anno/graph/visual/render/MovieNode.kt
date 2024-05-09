package me.anno.graph.visual.render

import me.anno.ecs.annotations.Type
import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.editor.graph.GraphPanel
import me.anno.ui.input.FileInput
import org.joml.Vector2f
import org.joml.Vector4f

class MovieNode : CalculationNode(
    "Movie",
    // todo different color repeat modes
    listOf("Vector2f", "UV", "Boolean", "Linear", "Float", "ConstTime(s)"),
    listOf("Vector4f", "Color")
), GLSLExprNode {

    init {
        setInput(0, Vector2f())
        setInput(1, true)
        setInput(2, 0f)
    }

    @Type("Texture/Reference")
    var file: FileReference = InvalidRef

    override fun calculate(): Vector4f {
        throw IllegalArgumentException("Operations is not supported")
    }

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        val texName = g.movies.getOrPut(this) {
            val linear = g.constEval(n.inputs[1]) == true
            Pair("movI${g.movies.size}", linear)
        }.first
        g.builder.append("texture(").append(texName).append(',')
        g.expr(n.inputs[0]) // uv
        g.builder.append(')')
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