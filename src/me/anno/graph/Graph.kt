package me.anno.graph

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.maths.solver.LinearSystem
import kotlin.math.ceil
import kotlin.math.sqrt


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

    fun calculateNodePositions() {


        // to do optimize the placement of the nodes
        // to do code flows from left to right
        // to do idea: pretty much there are dependencies from one node to another
        // to do idea: and these dependencies must be fulfilled
        // this would mean a large system of linear equations, and we'd need a solver
        // issue: the system would probably be relatively often not solvable, and some things
        // are more important than others

        // simple and stupid first:
        val nodes = nodes
        val size = nodes.size

        // x, y, condition-error, bounds
        val ls = LinearSystem(size * 2 + 2)
        val error = size * 2
        val bounds = size * 2 + 1

        ls.registerVariable(error, 1)
        ls.registerVariable(bounds, 1)

        for (i in nodes.indices) {
            ls.registerVariable(nodes[i], 2)
        }

        // all connections with distance rules
        for (i in nodes.indices) {
            val outNode = nodes[i]
            val outputs = outNode.outputs
            if (outputs != null) {
                for (output in outputs) {
                    for (input in output.others) {
                        val inNode = input.node
                        ls.lessThan(inNode, 0, 1.0, outNode, 0, -1.0, -1.0)
                    }
                }
            }
        }

        // todo we miss a second dimension, and it's probably impossible/hard to declare distance rules...
        // todo define error and bounds


        ls.minimize(error, 0)
        ls.minimize(bounds, 0)
        val solution = ls.solve()
        if (solution != null) {
            for (i in nodes.indices) {
                nodes[i].position.set(
                    solution[i * 2],
                    solution[i * 2 + 1],
                    0.0
                ).mul(700.0)
            }
        } else {
            val width = ceil(sqrt(size.toFloat()) * 1.1f).toInt()
            val height = (size + width - 1) / width
            for (i in 0 until size) {
                val x = i % width
                val y = i / width
                nodes[i].position.set(
                    (x - (width - 1) * 0.5) * 2.0,
                    (y - (height - 1) * 0.5) * 1.25,
                    0.0
                ).mul(700.0)
            }
        }

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