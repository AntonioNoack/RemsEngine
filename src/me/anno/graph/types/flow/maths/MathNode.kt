package me.anno.graph.types.flow.maths

import me.anno.graph.EnumNode
import me.anno.graph.types.flow.ValueNode
import me.anno.graph.ui.GraphEditor
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.strings.StringHelper.upperSnakeCaseToTitle

abstract class MathNode<V : Enum<V>>(
    val values: Array<V>,
) : ValueNode("", inputs, outputs), EnumNode, GLSLExprNode {

    constructor(clazz: Class<Enum<V>>) : this(vCache.getOrPut(clazz) { clazz.enumConstants } as Array<V>) {
        this.type = type
    }

    constructor(type: V) : this(type.javaClass) {
        this.type = type
    }

    var type: V = values[0]
        set(value) {
            field = value
            name = "Float " + value.name.upperSnakeCaseToTitle()
        }

    abstract fun getGLSL(type: V): String

    override fun getShaderFuncName(outputIndex: Int): String = "f1$type"
    override fun defineShaderFunc(outputIndex: Int): String = "(float a){return ${getGLSL(type)};}"

    override fun listNodes() = values.map {
        val cl = ISaveable.create(className) as MathNode<V>
        cl.type = type
        cl
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        list += EnumInput(
            "Type", true, type.name.upperSnakeCaseToTitle(),
            values.map { NameDesc(it.name.upperSnakeCaseToTitle(), getGLSL(it), "") }, style
        ).setChangeListener { _, index, _ ->
            type = values[index]
            g.onChange(false)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = values.getOrNull(values.binarySearch(value)) ?: type
        else super.readInt(name, value)
    }

    companion object {
        val vCache = HashMap<Class<*>, Any>(64)
        val inputs = listOf("Float", "A")
        val outputs = listOf("Float", "Result")
    }

}