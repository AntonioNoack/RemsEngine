package me.anno.graph.visual.node

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.Graph
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.maths.Maths.min
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelList
import me.anno.ui.editor.graph.GraphPanel
import me.anno.utils.types.Booleans.hasFlag
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
        this.inputs.ensureCapacity(inputs.size ushr 1)
        this.outputs.ensureCapacity(outputs.size ushr 1)
        for (it in 0 until (inputs.size ushr 1)) {
            this.inputs.add(NodeInput(inputs[it * 2], inputs[it * 2 + 1], this, false))
        }
        for (it in 0 until (outputs.size ushr 1)) {
            this.outputs.add(NodeOutput(outputs[it * 2], outputs[it * 2 + 1], this, false))
        }
    }

    // make name final
    final override var name = ""

    open fun createUI(g: GraphPanel, list: PanelList, style: Style) {}

    var position = Vector3d()
        set(value) {
            field.set(value)
        }

    // multiple layers would be great for large functions :D
    // even though they really should be split...
    // but we may zoom into other functions :)
    var layer = 0

    val inputs = ArrayList<NodeInput>()
    val outputs = ArrayList<NodeOutput>()

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
        val outputs = outputs
        return inputs.any { !it.isEmpty() } || outputs.any { !it.isEmpty() }
    }

    fun getOutput(index: Int): Any? {
        return outputs.getOrNull(index)?.currValue
    }

    fun setOutput(index: Int, value: Any?) {
        val output = outputs[index]
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
        return inputs[i].others.getOrNull(j)?.node
    }

    fun getOutputNode(i: Int, j: Int = 0): Node? {
        return outputs[i].others.getOrNull(j)?.node
    }

    fun delete(graph: Graph?) {
        val inputs = inputs
        for (con in inputs) {
            con.disconnectAll()
        }
        val outputs = outputs
        for (con in outputs) {
            con.disconnectAll()
        }
        // todo you might not be allowed to delete this node
        graph?.nodes?.remove(this)
    }

    // the node ofc needs to save its custom content and behaviour as well
    override fun save(writer: BaseWriter) {
        super.save(writer)

        // if valid, just save connections and values (should be much slimmer :))
        val inputs = inputs
        val outputs = outputs
        if (inputs.any { it.isCustom || it.currValue != null || it.others.isNotEmpty() })
            writer.writeObjectList(this, "inputs", inputs)
        if (outputs.any { it.isCustom || it.currValue != null || it.others.isNotEmpty() })
            writer.writeObjectList(this, "outputs", outputs)
        writer.writeInt("layer", layer)
        writer.writeVector3d("position", position)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "inputs" -> {
                val values = value as? List<*> ?: return
                cloneAssign(values, inputs)
            }
            "outputs" -> {
                val values = value as? List<*> ?: return
                cloneAssign(values, outputs)
            }
            "layer" -> layer = value as? Int ?: return
            "position" -> position.set(value as? Vector3d ?: return)
            else -> super.setProperty(name, value)
        }
    }

    private inline fun <reified V : NodeConnector> cloneAssign(values: List<*>, self: MutableList<V>) {
        val newbies = values.filterIsInstance<V>()
        for (i in newbies.indices) {
            val newbie = newbies[i]
            newbie.node = this
            val original = self.getOrNull(i)
            if (original?.isCustom == false) {
                newbie.name = original.name
                newbie.type = original.type
                newbie.description = original.description
            }
        }
        self.clear()
        self.addAll(newbies)
    }

    fun connectTo(otherNode: Node) {
        connectTo(0, otherNode, 0)
    }

    fun connectTo(otherNode: Node, inputIndex: Int) {
        connectTo(0, otherNode, inputIndex)
    }

    fun connectTo(outputIndex: Int, otherNode: Node, inputIndex: Int) {

        val output = outputs[outputIndex]
        // todo check if the node connector can have multiple outputs
        // flow only can have one,
        // values can have many

        val input = otherNode.inputs[inputIndex]
        // todo check if the node connector can have multiple inputs
        // flow can have many,
        // values only can have one

        output.others += input
        input.others += output
    }

    fun setInput(index: Int, value: Any?, validId: Int) {
        val c = inputs[index]
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
        return if (isConnected()) {
            // not ideal, but probably good enough for now and manual graph creation
            JsonStringReader.readFirst(JsonStringWriter.toText(this, InvalidRef), InvalidRef)
        } else super.clone() as Node
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as Node
        dst.position.set(position)
        dst.layer = layer
        dst.graph = graph
        dst.color = color
        val si = inputs
        val di = dst.inputs
        for (i in 0 until min(si.size, di.size)) {
            di[i].currValue = si[i].currValue
        }
    }
}