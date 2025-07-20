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
        private set

    init {
        // init name and description
        onTypeChange()
    }

    fun onTypeChange() {
        val idx = data.typeToIndex[enumType]
        name = data.names[idx]
        description = data.getGLSL(data.enumValues[idx])
        for (i in inputs.indices) {
            inputs[i].name = data.inputs[i * 2 + 1]
            inputs[i].type = data.inputs[i * 2]
        }
        for (i in outputs.indices) {
            outputs[i].name = data.outputs[i * 2 + 1]
            outputs[i].type = data.outputs[i * 2]
        }
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
                NameDesc("Type"), true, NameDesc(typeName),
                data.enumValues.map { NameDesc(it.name.upperSnakeCaseToTitle(), data.getGLSL(it), "") }, style
            ).setChangeListener { _, index, _ ->
                enumType = data.enumValues[index]
                g.onChange(false)
            }
        } else list.add(TextPanel("Type: $typeName", style))
    }

    fun setEnumType(type: V): MathNode<V> {
        enumType = type
        onTypeChange()
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
                onTypeChange()
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
        dst.onTypeChange()
    }
}