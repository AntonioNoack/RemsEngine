package me.anno.graph

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter

// todo graph panel: connect different types of nodes
// todo edit properties inside the nodes
// todo move the nodes around


// todo shader graph
// todo render pipeline graph
// todo scripting graphs
// todo animations graphs
// todo play graphs/story-graphs, e.g. with Q&As

class Graph : NamedSaveable() {

    var inputs = ArrayList<Node>()
    var outputs = ArrayList<Node>()

    // nodes without connections
    // could be all nodes as well, wouldn't really hurt space, because we save pointers anyways
    var tanglingNodes = ArrayList<Node>()

    // todo we need a graph ui to display these

    override val className: String = "Graph"
    override val approxSize: Int = 1000
    override fun isDefaultValue(): Boolean = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "inputs", inputs)
        writer.writeObjectList(this, "outputs", outputs)
        writer.writeObjectList(this, "tangling", tanglingNodes)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "inputs" -> inputs = ArrayList(values.filterIsInstance<Node>())
            "outputs" -> outputs = ArrayList(values.filterIsInstance<Node>())
            "tangling" -> tanglingNodes = ArrayList(values.filterIsInstance<Node>())
            else -> super.readObjectArray(name, values)
        }
    }

}