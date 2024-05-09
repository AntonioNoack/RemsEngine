package me.anno.graph.visual.render

import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GLSLExprNode
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.io.base.BaseWriter
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.editor.graph.GraphPanel
import me.anno.ui.input.ColorInput
import me.anno.utils.Color.toARGB
import org.joml.Vector4f

@Suppress("unused")
class ColorNode : ComputeNode("Color", emptyList(), listOf("Vector4f", "Color", "Int", "ARGB")), GLSLExprNode {

    val value = Vector4f(1f)

    init {
        setOutput(0, value)
        setOutput(1, value.toARGB())
    }

    // nothing to do
    override fun compute() {
    }

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        g.appendVec4(value)
    }

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        list += ColorInput("Value", "", value, true, style)
            .setChangeListener { r, gr, b, a, _ ->
                value.set(r, gr, b, a)
                setOutput(1, value.toARGB())
                g.onChange(false)
            }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeVector4f("value", value)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "value" -> this.value.set(value as? Vector4f ?: return)
            else -> super.setProperty(name, value)
        }
    }
}