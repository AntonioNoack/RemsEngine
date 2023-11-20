package me.anno.graph.types.flow.maths

import me.anno.graph.types.flow.CalculationNode
import me.anno.io.base.BaseWriter

class ValueNode private constructor(type: String, inputs: List<String>, outputs: List<String>) :
    CalculationNode(type, inputs, outputs) {

    private constructor(type: String, list: List<String>) :
            this(type, list, list)

    constructor(type: String) : this(type, listOf(type, "Value"))

    var type: String = type
        set(value) {
            field = value
            inputs!![0].type = value
            outputs!![0].type = value
            name = value
        }

    override fun calculate() = getInput(0)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
    }

    override fun readString(name: String, value: String) {
        if (name == "type") type = value
        else super.readString(name, value)
    }

}