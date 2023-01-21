package me.anno.graph.types.flow.local

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import me.anno.io.base.BaseWriter

class GetLocalVariableNode(type: String = "?") :
    CalculationNode("GetLocal", inputs, listOf(type, "Value")) {

    constructor(key: String, type: String) : this(type) {
        setInput(0, key)
    }

    var type: String = type
        set(value) {
            field = value
            outputs!![0].type = value
            name = if (value == "?") "GetLocal"
            else "GetLocal $value"
        }

    init {
        if (type != "?") name = "GetLocal $type"
    }

    fun getKey(graph: FlowGraph) = getInput(graph, 0) as String
    fun getValue(graph: FlowGraph) = graph.localVariables[getKey(graph)]

    override fun calculate(graph: FlowGraph): Any? {
        val key = getInput(graph, 0)
        return graph.localVariables[key]
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
    }

    override fun readString(name: String, value: String?) {
        if (name == "type") type = value ?: return
        else super.readString(name, value)
    }

    companion object {
        val inputs = listOf("String", "Name")
        val outputs = listOf("?", "Value")
    }

}