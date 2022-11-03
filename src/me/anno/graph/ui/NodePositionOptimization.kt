package me.anno.graph.ui

import me.anno.graph.Node
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
                val inputs = node.inputs
                if (inputs != null) for (it in inputs) {
                    it.position.add(dx, dy, dz)
                }
                val outputs = node.outputs
                if (outputs != null) for (it in outputs) {
                    it.position.add(dx, dy, dz)
                }
            }
            // compute error
            val err = nodes.sumOf {
                // nodes should not spread too much
                var error = 0.01 * it.position.lengthSquared()
                val out = it.outputs
                if (out != null) {
                    for (index in out.indices) {
                        val output = out[index]
                        for (input in output.others) {
                            val op = output.position
                            error += input.position.distanceSquared(op.x + 100.0, op.y, op.z)
                        }
                    }
                }
                error
            }
            err
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