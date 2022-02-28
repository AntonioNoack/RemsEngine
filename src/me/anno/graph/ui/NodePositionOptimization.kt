package me.anno.graph.ui

import me.anno.graph.Node
import me.anno.maths.Optimization
import kotlin.math.max

object NodePositionOptimization {

    fun calculateNodePositions(nodes: List<Node>) {

        val size = nodes.size

        // define the node positions as a general optimization problem
        val solution = Optimization.simplexAlgorithm(
            DoubleArray(size * 2) { if (it.and(1) == 1) 0.0 else it.toDouble() },
            0.1, 0.0, 10000
        ) { v ->
            // place all nodes
            for (i in 0 until size) {
                val node = nodes[i]
                val j = i * 2
                val pos = node.position
                pos.x = v[j]
                pos.y = v[j + 1]
                // place all connectors
                val out = node.outputs
                if (out != null) {
                    for (conIndex in out.indices) {
                        val pos2 = out[conIndex].position.set(pos)
                        pos2.x += 5.0
                        pos2.y += conIndex.toDouble()
                    }
                }
                val input = node.inputs
                if (input != null) {
                    for (conIndex in input.indices) {
                        val pos2 = input[conIndex].position.set(pos)
                        pos2.x -= 5.0
                        pos2.y += conIndex.toDouble()
                    }
                }
            }
            // compute error
            nodes.sumOf {
                // nodes should not spread too much
                var error = it.position.lengthSquared()
                val out = it.outputs
                if (out != null) {
                    for (index in out.indices) {
                        val outNode = out[index]
                        for (inNode in outNode.others) {
                            // they should keep their distance based on connectors
                            error += max(inNode.position.x - outNode.position.y, 0.0)
                        }
                    }
                }
                // they should not overlap with others
                for (other in nodes) {
                    error += 1.0 / max(
                        other.position.distanceSquared(it.position),
                        0.01
                    )
                }
                error
            }
        }


        for (i in 0 until size) {
            nodes[i].position.set(
                solution[i * 2],
                solution[i * 2 + 1],
                0.0
            ).mul(500.0)
        }

    }

}