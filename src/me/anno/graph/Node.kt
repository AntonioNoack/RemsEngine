package me.anno.graph

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.ui.base.groups.PanelList
import me.anno.ui.style.Style
import org.joml.Vector3d

abstract class Node() : PrefabSaveable() {

    /**
     * @param name initial node name
     * */
    constructor(name: String) : this() {
        this.name = name
    }

    /**
     * @param name initial node name
     * @param inputs [type, name, [type, name], [type, name], ...
     * @param outputs [type, name], [type, name], [type, name], ...
     * */
    constructor(name: String, inputs: List<String>, outputs: List<String>) : this(name) {
        this.inputs = Array(inputs.size / 2) { NodeInput(inputs[it * 2], inputs[it * 2 + 1], this) }
        this.outputs = Array(outputs.size / 2) { NodeOutput(outputs[it * 2], outputs[it * 2 + 1], this) }
    }

    // make name final
    final override var name = ""

    open fun createUI(list: PanelList, style: Style) {}

    val position = Vector3d()

    // multiple layers would be great for large functions :D
    // even though they really should be split...
    // but we may zoom into other functions :)
    var layer = 0

    var inputs: Array<NodeInput>? = null
    var outputs: Array<NodeOutput>? = null

    // todo use this color, if defined
    var color = 0

    open fun canAddInput(type: String) = false
    open fun canAddOutput(type: String) = false
    open fun canRemoveInput(type: String) = false
    open fun canRemoveOutput(type: String) = false
    open fun supportsMultipleInputs(con: NodeConnector) = false
    open fun supportsMultipleOutputs(con: NodeConnector) = false

    fun setOutput(value: Any?, index: Int) {
        val node = outputs!![index]
        node.value = value
        node.others.forEach { it.invalidate() }
    }

    fun delete(graph: Graph?) {
        val inputs = inputs
        if (inputs != null) for (con in inputs) {
            con.disconnectAll()
        }
        val outputs = outputs
        if (outputs != null) for (con in outputs) {
            con.disconnectAll()
        }
        // todo you might not be allowed to delete this node
        if (graph != null) {
            graph.inputs.remove(this)
            graph.outputs.remove(this)
            graph.nodes.remove(this)
        }
    }

    // the node ofc needs to save its custom content and behaviour as well
    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectArray(this, "inputs", inputs)
        writer.writeObjectArray(this, "outputs", outputs)
        writer.writeInt("layer", layer)
        writer.writeVector3d("position", position)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "inputs" -> {
                inputs = values.filterIsInstance<NodeInput>().toTypedArray()
                inputs?.forEach { it.node = this }
            }
            "outputs" -> {
                outputs = values.filterIsInstance<NodeOutput>().toTypedArray()
                outputs?.forEach { it.node = this }
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "layer" -> layer = value
            else -> super.readInt(name, value)
        }
    }

    override fun readVector3d(name: String, value: Vector3d) {
        when (name) {
            "position" -> position.set(value)
            else -> super.readVector3d(name, value)
        }
    }

    fun connectTo(otherNode: Node) {
        connectTo(0, otherNode, 0)
    }

    fun connectTo(otherNode: Node, othersInputIndex: Int) {
        connectTo(0, otherNode, othersInputIndex)
    }

    fun connectTo(outputIndex: Int, otherNode: Node, othersInputIndex: Int) {

        val output = outputs!![outputIndex]
        // todo check if the node connector can have multiple outputs
        // flow only can have one,
        // values can have many

        val input = otherNode.inputs!![othersInputIndex]
        // todo check if the node connector can have multiple inputs
        // flow can have many,
        // values only can have one

        output.others += input
        input.others += output

    }

    fun setInput(index: Int, value: Any?, validId: Int) {
        val c = inputs!![index]
        c.lastValidId = validId
        c.value = value
    }

    fun setInput(index: Int, value: Any?) {
        setInput(index, value, -1)
    }

    fun setInputs(inputValues: List<Any?>, validId: Int) {
        for ((index, value) in inputValues.withIndex()) {
            setInput(index, value, validId)
        }
    }

    fun setInputs(inputValues: List<Any?>) {
        setInputs(inputValues, -1)
    }

    override fun clone(): Node {// not ideal, but probably good enough for now and manual graph creation
        return TextReader.readFirst(TextWriter.toText(this, InvalidRef), InvalidRef)
    }

}