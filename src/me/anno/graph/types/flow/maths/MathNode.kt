package me.anno.graph.types.flow.maths

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.EnumNode
import me.anno.graph.render.MaterialGraph.kotlinToGLSL
import me.anno.graph.types.flow.ValueNode
import me.anno.graph.ui.GraphEditor
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.hasFlag
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.strings.StringHelper.upperSnakeCaseToTitle

abstract class MathNode<V : Enum<V>>(
    val data: MathNodeData<V>,
) : ValueNode("", data.inputs, data.outputs), EnumNode, GLSLExprNode {

    class MathNodeData<V : Enum<V>>(
        val values: Array<V>,
        inputTypes: List<String>,
        outputType: String,
        getId: (V) -> Int,
        val getGLSL: (V) -> String,
    ) {
        val inputs = (0 until inputTypes.size * 2)
            .map { if (it.hasFlag(1)) ('A' + it.shr(1)).toString() else inputTypes[it shr 1] }
        val defaultType = values[0]
        val outputs = listOf(outputType, "Result")
        val byId = values.associateBy { getId(it) }
        val shaderFuncPrefix by lazy {
            (0 until inputs.size / 2).joinToString(",") {
                kotlinToGLSL(inputs[it * 2]) + " " + ('a' + it)
            }
        }
        val names = Array(values.size) {
            inputs[0] + " " + values[it].name.upperSnakeCaseToTitle()
        }
        val typeToIndex = values.withIndex().associate { it.value to it.index }
    }

    var type: V = data.defaultType
        set(value) {
            field = value
            val idx = data.typeToIndex[value]!!
            name = data.names[idx]
            description = data.getGLSL(data.values[idx])
        }

    init {
        // init name and description
        type = type
    }

    override fun getShaderFuncName(outputIndex: Int): String = "${data.outputs.first()}$type"
    override fun defineShaderFunc(outputIndex: Int): String =
        "(${data.shaderFuncPrefix}){return ${data.getGLSL(type)};}"

    override fun listNodes(): List<MathNode<V>> {
        val clazz = javaClass
        return data.values.map {
            clazz.newInstance().apply { type = it }
        }
    }

    override fun createUI(g: GraphEditor, list: PanelList, style: Style) {
        super.createUI(g, list, style)
        list += EnumInput(
            "Type", true, type.name.upperSnakeCaseToTitle(),
            data.values.map { NameDesc(it.name.upperSnakeCaseToTitle(), data.getGLSL(it), "") }, style
        ).setChangeListener { _, index, _ ->
            type = data.values[index]
            g.onChange(false)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "type") type = data.byId[value] ?: type
        else super.readInt(name, value)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as MathNode<V>
        clone.type = type
    }

}