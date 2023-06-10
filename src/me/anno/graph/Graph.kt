package me.anno.graph

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.render.NodeGroup
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter


// todo for editing just copy them
// todo for the final shader graph, maybe just use versions, e.g. v01, v02, and the user can select the base for their graph

// todo shader graph = dependency & processing graph
// todo render pipeline graph = dependency & processing graph
// todo scripting graphs = flow graph
// todo animations graphs = state graphs
// todo play graphs/story-graphs, e.g. with Q&As
// todo many game-internal machines could be built with state machines :3

open class Graph : PrefabSaveable() {

    // nodes without connections
    // could be all nodes as well, wouldn't really hurt space, because we save pointers anyway
    val nodes = ArrayList<Node>()
    val groups = ArrayList<NodeGroup>()

    fun add(node: Node): Node {
        node.graph?.remove(node)
        nodes.add(node)
        node.graph = this
        return node
    }

    fun remove(node: Node) {
        if (nodes.remove(node))
            node.graph = null
    }

    fun addAll(nodes: List<Node>) {
        this.nodes.addAll(nodes)
        for (node in nodes) node.graph = this
    }

    fun addAll(vararg nodes: Node) {
        this.nodes.addAll(nodes)
        for (node in nodes) node.graph = this
    }

    open fun canConnectTo(self: NodeConnector, other: NodeConnector): Boolean {
        if (self.javaClass == other.javaClass) return false
        // todo when connecting flows, ensure that no loops are formed between primary inputs
        //  secondary flow inputs could be used for "break" or such
        return if (self is NodeInput) {
            canConnectTypeToOtherType(self.type, other.type)
        } else {
            canConnectTypeToOtherType(other.type, self.type)
        }
    }

    open fun canConnectTypeToOtherType(srcType: String, dstType: String): Boolean {
        return ((srcType == "Flow") == (dstType == "Flow"))
    }

    override val className: String get() = "Graph"
    override val approxSize get() = 1000
    override fun isDefaultValue(): Boolean = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(null, "nodes", nodes)
        writer.writeObjectList(null, "groups", groups)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "nodes" -> {
                nodes.clear()
                nodes.addAll(values.filterIsInstance<Node>())
            }
            "groups" -> {
                groups.clear()
                groups.addAll(values.filterIsInstance<NodeGroup>())
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override fun clone(): PrefabSaveable {
        return TextReader.readFirst(TextWriter.toText(this, InvalidRef), InvalidRef, false)
    }

}