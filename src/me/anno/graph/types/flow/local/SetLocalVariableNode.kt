package me.anno.graph.types.flow.local

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.io.base.BaseWriter

class SetLocalVariableNode(type: String = "?") :
    ActionNode(
        "SetLocal",
        listOf("String", "Name", type, "New Value"),
        listOf(type, "Current Value")
    ) {

    var type: String = type
        set(value) {
            field = value
            inputs!![2].type = value
            outputs!![1].type = value
            name = if (value == "?") "SetLocal"
            else "SetLocal $value"
        }

    init {
        if (type != "?") name = "SetLocal $type"
    }

    constructor(key: String, value: Any?) : this() {
        setInputs(listOf(null, key, value))
    }

    fun getKey(graph: FlowGraph) = getInput(graph, 1) as String
    fun getValue(graph: FlowGraph) = getInput(graph, 2)

    override fun executeAction(graph: FlowGraph) {
        val key = getInput(graph, 1) as String
        val value = getInput(graph, 2)
        graph.localVariables[key] = value
        setOutput(value, 1)
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
        val inputs = listOf("String", "Name", "?", "New Value")
        val outputs = listOf("?", "Current Value")
    }


}