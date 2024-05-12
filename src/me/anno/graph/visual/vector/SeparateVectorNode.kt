package me.anno.graph.visual.vector

import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.graph.visual.scalar.TypedNode
import me.anno.graph.visual.scalar.TypedNodeData
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector

private val data = LazyMap { type: String ->
    val numComp = type[6] - '0'
    val iType = getVectorType(type)
    TypedNodeData(
        "Separate $type", "" to "",
        listOf(type, "Vector"),
        listOf(iType, "X", iType, "Y", iType, "Z", iType, "W").subList(0, numComp * 2)
    )
}

class SeparateVectorNode : TypedNode(data, vectorTypes) {
    override fun compute() {
        val outputType = outputs[0].type
        val vec = getInput(0) as Vector
        for (i in 0 until vec.numComponents) {
            val vi = vec.getCompOr(i)
            val viTyped = when (outputType) {
                "Int" -> vi.toInt()
                "Float" -> vi.toFloat()
                else -> vi
            }
            setOutput(i, viTyped)
        }
    }

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        val c = n.outputs.indexOf(out)
        g.builder.append('(')
        g.expr(n.inputs[0]) // vector
        g.builder.append(").").append("xyzw"[c])
    }
}