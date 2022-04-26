package me.anno.graph

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter


// todo for editing just copy them
// todo for the final shader graph, maybe just use versions, e.g. v01, v02, and the user can select the base for their graph


// todo graph panel: connect different types of nodes
// todo edit properties inside the nodes
// todo move the nodes around


// todo shader graph = dependency & processing graph
// todo render pipeline graph = dependency & processing graph
// todo scripting graphs = flow graph
// todo animations graphs = state graphs
// todo play graphs/story-graphs, e.g. with Q&As
// todo many game-internal machines could be built with state machines :3

open class Graph : NamedSaveable() {

    var inputs = ArrayList<Node>()
    var outputs = ArrayList<Node>()

    // nodes without connections
    // could be all nodes as well, wouldn't really hurt space, because we save pointers anyways
    var nodes = ArrayList<Node>()

    fun addAll(nodes: List<Node>) {
        this.nodes.addAll(nodes)
    }

    override val className: String = "Graph"
    override val approxSize: Int = 1000
    override fun isDefaultValue(): Boolean = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "inputs", inputs)
        writer.writeObjectList(this, "outputs", outputs)
        writer.writeObjectList(this, "nodes", nodes)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "inputs" -> inputs = ArrayList(values.filterIsInstance<Node>())
            "outputs" -> outputs = ArrayList(values.filterIsInstance<Node>())
            "nodes" -> nodes = ArrayList(values.filterIsInstance<Node>())
            else -> super.readObjectArray(name, values)
        }
    }

}