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

    var enumType: V = data.defaultType
        set(value) {
            field = value
            updateNameDesc()
        }

    init {
        // init name and description
        updateNameDesc()
    }

    fun updateNameDesc() {
        val idx = data.typeToIndex[enumType]!!
        name = data.names[idx]
        description = data.getGLSL(data.enumValues[idx])
    }

    override fun getShaderFuncName(outputIndex: Int): String = "${data.outputs.first()}$enumType"
    override fun defineShaderFunc(outputIndex: Int): String = data.getGLSL(enumType)

    override fun listNodes(): List<MathNode<V>> {
        return data.enumValues.map { type ->
            @Suppress("UNCHECKED_CAST")
            val clone = this.clone() as MathNode<V>
            clone.enumType = type
            clone
        }
    }

    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        val typeName = enumType.name.upperSnakeCaseToTitle()
        if (g is GraphEditor) {
            list += EnumInput(
                "Type", true, typeName,
                data.enumValues.map { NameDesc(it.name.upperSnakeCaseToTitle(), data.getGLSL(it), "") }, style
            ).setChangeListener { _, index, _ ->
                enumType = data.enumValues[index]
                g.onChange(false)
            }
        } else list.add(TextPanel("Type: $typeName", style))
    }

    fun setEnumType(type: V): MathNode<V> {
        enumType = type
        return this
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeEnum("type", enumType)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "type" -> {
                if (value !is Int) return
                enumType = data.byId[value] ?: enumType
            }
            else -> super.setProperty(name, value)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        @Suppress("UNCHECKED_CAST")
        dst as MathNode<V>
        dst.data = data
        dst.enumType = enumType
        for (i in dst.inputs.indices) {
            dst.inputs[i].type = inputs[i].type
        }
        for (i in dst.outputs.indices) {
            dst.outputs[i].type = outputs[i].type
        }
    }
}