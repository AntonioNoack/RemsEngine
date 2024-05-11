package me.anno.graph.visual.scalar

import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import me.anno.io.base.BaseWriter
import me.anno.utils.structures.maps.LazyMap

// done: define an abstract class TypedNode, that can handle different types
// todo also create an abstract class AnyTypeNode, that can handle any type, and has an enum-like-or-sth select for the type
abstract class TypedNode(
    val dataMap: LazyMap<String, TypedNodeData>,
    val valueTypes: List<String>,
    var data: TypedNodeData = dataMap[valueTypes.first()]
) : ComputeNode(data.name, data.inputs, data.outputType), EnumNode, GLSLFuncNode {

    init {
        updateNameDesc()
    }

    override fun getShaderFuncName(outputIndex: Int) = data.glsl.first
    override fun defineShaderFunc(outputIndex: Int) = data.glsl.second

    override fun listNodes(): List<Node> {
        return valueTypes.map { type ->
            val clone = clone() as TypedNode
            clone.setType(type)
            clone
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("dataType", data.inputs.first())
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "dataType" -> setType(value.toString())
            else -> super.setProperty(name, value)
        }
    }

    fun setType(type: String): TypedNode {
        data = dataMap[type]
        updateNameDesc()
        return this
    }

    init {
        // init name and description
        updateNameDesc()
    }

    fun updateNameDesc() {
        name = data.name
        description = data.glsl.first
    }
}