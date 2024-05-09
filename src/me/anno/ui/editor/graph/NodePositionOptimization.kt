package me.anno.ui.editor.graph

import me.anno.graph.visual.node.Node
import me.anno.maths.Optimization

object NodePositionOptimization {

    fun calculateNodePositions(nodes: List<Node>) {

        val size = nodes.size

        // define the node positions as a general optimization problem
        val (_, solution) = Optimization.simplexAlgorithm(
            DoubleArray(size * 2) { if (it.and(1) == 1) 0.0 else it.toDouble() },
            0.1, 0.0, 200
        ) { v ->
            // place all nodes
            for (i in 0 until size) {
                val node = nodes[i]
                val j = i * 2
                val pos = node.position
                val dx = v[j] - pos.x
                val dy = v[j + 1] - pos.y
                val dz = 0.0
                pos.set(v[j], v[j + 1], pos.z)
                // place all connectors
                for (it in node.inputs) {
                    it.position.add(dx, dy, dz)
                }
                for (it in node.outputs) {
                    it.position.add(dx, dy, dz)
                }
            }
            // compute error
            nodes.sumOf { node ->
                node.outputs.sumOf { output ->
                    output.others.sumOf { input ->
                        val op = output.position
                        input.position.distanceSquared(op.x + 100.0, op.y, op.z)
                    }
                } + 0.01 * node.position.lengthSquared() // nodes should not spread too much
            }
        }

        for (i in 0 until size) {
            nodes[i].position.set(
                solution[i * 2],
                solution[i * 2 + 1],
                0.0
            )
        }
    }
}