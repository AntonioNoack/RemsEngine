package me.anno.graph.visual.scalar

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import me.anno.io.base.BaseWriter
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.graph.GraphEditor
import me.anno.ui.editor.graph.GraphPanel
import me.anno.ui.input.EnumInput
import me.anno.utils.types.Strings.upperSnakeCaseToTitle

abstract class MathNode<V : Enum<V>>(var data: MathNodeData<V>) :
    ComputeNode("", data.inputs, data.outputs), EnumNode, GLSLFuncNode {

    var type: V = data.defaultType
        set(value) {
            field = value
            updateNameDesc()
        }

    init {
        // init name and description
        updateNameDesc()
    }

    fun updateNameDesc() {
        val idx = data.typeToIndex[type]!!
        name = data.names[idx]
        description = data.getGLSL(data.enumValues[idx])
    }

    override fun getShaderFuncName(outputIndex: Int): String = "${data.outputs.first()}$type"
    override fun defineShaderFunc(outputIndex: Int): String =
        "(${data.shaderFuncPrefix}){return ${data.getGLSL(type)};}"

    override fun listNodes(): List<MathNode<V>> {
        return data.enumValues.map { type ->
            @Suppress("UNCHECKED_CAST")
            val clone = this.clone() as MathNode<V>
            clone.type = type
            clone
        }
    }

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        val typeName = type.name.upperSnakeCaseToTitle()
        if (g is GraphEditor) {
            list += EnumInput(
                "Type", true, typeName,
                data.enumValues.map { NameDesc(it.name.upperSnakeCaseToTitle(), data.getGLSL(it), "") }, style
            ).setChangeListener { _, index, _ ->
                type = data.enumValues[index]
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
        dst.data = data
        dst.type = type
        for (i in dst.inputs.indices) {
            dst.inputs[i].type = inputs[i].type
        }
        for (i in dst.outputs.indices) {
            dst.outputs[i].type = outputs[i].type
        }
    }
}