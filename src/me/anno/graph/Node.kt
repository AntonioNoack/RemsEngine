package me.anno.graph

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.types.FlowGraph
import me.anno.graph.ui.GraphPanel
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.maths.Maths.hasFlag
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
        if (inputs.size.hasFlag(1)) throw IllegalArgumentException("Each input must be defined as type + name, got ${inputs.size} args")
        if (outputs.size.hasFlag(1)) throw IllegalArgumentException("Each output must be defined as type + name, got ${outputs.size} args")
        this.inputs = Array(inputs.size shr 1) { NodeInput(inputs[it * 2], inputs[it * 2 + 1], this, false) }
        this.outputs = Array(outputs.size shr 1) { NodeOutput(outputs[it * 2], outputs[it * 2 + 1], this, false) }
    }

    // make name final
    final override var name = ""

    open fun createUI(g: GraphPanel, list: PanelList, style: Style) {}

    val position = Vector3d()

    // multiple layers would be great for large functions :D
    // even though they really should be split...
    // but we may zoom into other functions :)
    var layer = 0

    var inputs: Array<NodeInput>? = null
    var outputs: Array<NodeOutput>? = null

    var color = 0
    var graph: Graph? = null

    open fun canAddInput(type: String, index: Int) = false
    open fun canAddOutput(type: String, index: Int) = false
    open fun canRemoveInput(type: String, index: Int) = false
    open fun canRemoveOutput(type: String, index: Int) = false
    open fun supportsMultipleInputs(con: NodeConnector) = false
    open fun supportsMultipleOutputs(con: NodeConnector) = false

    fun isConnected(): Boolean {
        val inputs = inputs
        if (inputs != null && inputs.any { !it.isEmpty() }) {
            return true
        }
        val outputs = outputs
        if (outputs != null && outputs.any { !it.isEmpty() }) {
            return true
        }
        return false
    }

    fun getOutput(index: Int): Any? {
        return outputs?.getOrNull(index)?.currValue
    }

    fun setOutput(value: Any?, index: Int = 0) {
        val output = outputs!![index]
        output.currValue = value
        val graph = graph
        if (graph is FlowGraph) {
            val inputs = output.others
            for (i in inputs.indices) {
                val input = inputs[i] as? NodeInput ?: continue
                input.validate(value, graph)
            }
        }
    }

    fun getInputNode(i: Int, j: Int = 0): Node? {
        return inputs?.get(i)?.others?.getOrNull(j)?.node
    }

    fun getOutputNode(i: Int, j: Int = 0): Node? {
        return outputs?.get(i)?.others?.getOrNull(j)?.node
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
        graph?.nodes?.remove(this)
    }

    // the node ofc needs to save its custom content and behaviour as well
    override fun save(writer: BaseWriter) {
        // super.save(writer) // just name + desc;
        // and those are defined by the type
        // if you need them yourself, just create your own node type;

        // if valid, just save connections and values (should be much slimmer :))
        val inputs = inputs
        val outputs = outputs
        if (inputs == null || inputs.any { it.isCustom || it.currValue != null || it.others.isNotEmpty() })
            writer.writeObjectArray(this, "inputs", inputs)
        if (outputs == null || outputs.any { it.isCustom || it.currValue != null || it.others.isNotEmpty() })
            writer.writeObjectArray(this, "outputs", outputs)
        writer.writeInt("layer", layer)
        writer.writeVector3d("position", position)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "inputs" -> {
                val newbies = values.filterIsInstance<NodeInput>().toTypedArray()
                val originals = this.inputs
                for (i in newbies.indices) {
                    val newbie = newbies[i]
                    newbie.node = this
                    val original = originals?.getOrNull(i)
                    if (original?.isCustom == false) {
                        newbie.name = original.name
                        newbie.type = original.type
                        newbie.description = original.description
                    }
                }
                this.inputs = newbies
            }
            "outputs" -> {
                val newbies = values.filterIsInstance<NodeOutput>().toTypedArray()
                val originals = this.outputs
                for (i in newbies.indices) {
                    val newbie = newbies[i]
                    newbie.node = this
                    val original = originals?.getOrNull(i)
                    if (original?.isCustom == false) {
                        newbie.name = original.name
                        newbie.type = original.type
                        newbie.description = original.description
                    }
                }
                this.outputs = newbies
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

    fun connectTo(otherNode: Node, inputIndex: Int) {
        connectTo(0, otherNode, inputIndex)
    }

    fun connectTo(outputIndex: Int, otherNode: Node, inputIndex: Int) {

        val output = outputs!![outputIndex]
        // todo check if the node connector can have multiple outputs
        // flow only can have one,
        // values can have many

        val input = otherNode.inputs!![inputIndex]
        // todo check if the node connector can have multiple inputs
        // flow can have many,
        // values only can have one

        output.others += input
        input.others += output

    }

    fun setInput(index: Int, value: Any?, validId: Int) {
        val c = inputs!![index]
        c.lastValidId = validId
        c.currValue = value
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

    /**
     * if you want to change this function, please be aware, that node cloning is very complex!
     * you need to clone all connectors, their values (names, descriptions, types, ...), and potentially neighboring nodes
     * */
    override fun clone(): Node {
        // not ideal, but probably good enough for now and manual graph creation
        if (!isConnected()) {
            val clone = javaClass.newInstance()
            copyInto(clone)
            return clone
        }
        return TextReader.readFirst(TextWriter.toText(this, InvalidRef), InvalidRef)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Node
        dst.position.set(position)
        dst.layer = layer
        dst.graph = graph
        dst.color = color
    }

}