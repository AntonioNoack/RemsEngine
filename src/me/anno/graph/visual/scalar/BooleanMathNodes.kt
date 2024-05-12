package me.anno.graph.visual.scalar

import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.FlowGraphNodeUtils.getBoolInput
import me.anno.graph.visual.render.compiler.GLSLFuncNode

class NotNode : ComputeNode("Binary Not", listOf("Boolean", "Value"), "Boolean") {
    override fun compute() {
        setOutput(0, !getBoolInput(0))
    }
}

private val dataB2 = MathNodeData(
    BooleanMathType.entries,
    listOf("Boolean", "Boolean"), "Boolean",
    BooleanMathType::id, BooleanMathType::glsl2d
)

class MathB2Node : MathNode<BooleanMathType>(dataB2), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, enumType.compute2d(getBoolInput(0), getBoolInput(1)))
    }
}

private val dataB3 = MathNodeData(
    BooleanMathType.entries,
    listOf("Boolean", "Boolean", "Boolean"), "Boolean",
    BooleanMathType::id, BooleanMathType::glsl3d,
    dataB2.names.map { "$it x3" }
)

class MathB3Node : MathNode<BooleanMathType>(dataB3), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, enumType.compute3d(getBoolInput(0), getBoolInput(1), getBoolInput(2)))
    }
}
