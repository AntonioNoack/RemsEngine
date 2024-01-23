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
            inputs[2].type = value
            outputs[1].type = value
            name = if (value == "?") "SetLocal"
            else "SetLocal $value"
        }

    init {
        if (type != "?") name = "SetLocal $type"
    }

    constructor(key: String, value: Any?) : this() {
        setInputs(listOf(null, key, value))
    }

    val key get() = getInput(1) as String
    val value get() = getInput(2)

    override fun executeAction() {
        val key = getInput(1) as String
        val value = getInput(2)
        val graph = graph as FlowGraph
        graph.localVariables[key] = value
        setOutput(1, value)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
    }

    override fun readString(name: String, value: String) {
        if (name == "type") type = value
        else super.readString(name, value)
    }

    companion object {
        val inputs = listOf("String", "Name", "?", "New Value")
        val outputs = listOf("?", "Current Value")
    }
}