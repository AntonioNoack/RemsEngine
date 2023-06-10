package me.anno.graph.render

import me.anno.graph.types.flow.ValueNode
import me.anno.graph.ui.GraphPanel
import me.anno.io.base.BaseWriter
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.ColorInput
import me.anno.ui.style.Style
import me.anno.utils.Color.toARGB
import org.joml.Vector4f

class ColorNode : ValueNode("Color", emptyList(), listOf("Vector4f", "Color", "Int", "ARGB")) {

    val value = Vector4f(1f)

    init {
        setOutput(value, 0)
        setOutput(value.toARGB(), 1)
    }

    // nothing to do
    override fun compute() {
    }

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        list += ColorInput(style, "Value", "", value, true)
            .setChangeListener { r, gr, b, a ->
                value.set(r, gr, b, a)
                setOutput(value.toARGB(), 1)
                g.onChange(false)
            }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeVector4f("value", value)
    }

    override fun readVector4f(name: String, value: Vector4f) {
        if (name == "value") this.value.set(value)
        else super.readVector4f(name, value)
    }

}