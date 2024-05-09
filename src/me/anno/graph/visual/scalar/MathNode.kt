package me.anno.graph.visual.scalar

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.render.MaterialGraph.kotlinToGLSL
import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import me.anno.ui.editor.graph.GraphEditor
import me.anno.ui.editor.graph.GraphPanel
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.utils.types.Booleans.hasFlag
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.EnumInput
import me.anno.utils.types.Strings.upperSnakeCaseToTitle

abstract class MathNode<V : Enum<V>>(
    val data: MathNodeData<V>,
) : ComputeNode("", data.inputs, data.outputs), EnumNode, GLSLFuncNode {

    class MathNodeData<V : Enum<V>>(
        val values: List<V>,
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

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        val typeName = type.name.upperSnakeCaseToTitle()
        if (g is GraphEditor) {
            list += EnumInput(
                "Type", true, typeName,
                data.values.map { NameDesc(it.name.upperSnakeCaseToTitle(), data.getGLSL(it), "") }, style
            ).setChangeListener { _, index, _ ->
                type = data.values[index]
                g.onChange(false)
            }
        } else list.add(TextPanel("Type: $typeName", style))
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", type)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "type" -> {
                if (value !is Int) return
                type = data.byId[value] ?: type
            }
            else -> super.setProperty(name, value)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        @Suppress("UNCHECKED_CAST")
        dst as MathNode<V>
        dst.type = type
    }
}