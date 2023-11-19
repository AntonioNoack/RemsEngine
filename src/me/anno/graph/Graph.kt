package me.anno.graph

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.render.NodeGroup
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter


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

    override fun listChildTypes() = "ng"
    override fun getChildListByType(type: Char): List<PrefabSaveable> {
        return if (type == 'n') nodes else groups
    }

    override fun getValidTypesForChild(child: PrefabSaveable): String {
        return when (child) {
            is Node -> "n"
            is NodeGroup -> "g"
            else -> super.getValidTypesForChild(child)
        }
    }

    override fun addChildByType(index: Int, type: Char, child: PrefabSaveable) {
        when (child) {
            is Node -> nodes.add(child)
            is NodeGroup -> groups.add(child)
            else -> throw IllegalArgumentException()
        }
    }

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

    fun addAllConnected(vararg nodes: Node) {
        val nodes1 = HashSet<Node>()
        fun process(node: Node) {
            fun process(con: NodeConnector) {
                for (other in con.others) {
                    val nodeI = other.node
                    if (nodeI != null) process(nodeI)
                }
            }
            if (nodes1.add(node)) {
                add(node)
                val inputs = node.inputs
                if (inputs != null) {
                    for (c in inputs) {
                        process(c)
                    }
                }
                val outputs = node.outputs
                if (outputs != null) {
                    for (c in outputs) {
                        process(c)
                    }
                }
            }
        }
        for (node in nodes) {
            process(node)
        }
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
        val clone = JsonStringReader.readFirst(JsonStringWriter.toText(this, InvalidRef), InvalidRef, false) as Graph
        for (node in clone.nodes) node.graph = clone
        return clone
    }
}