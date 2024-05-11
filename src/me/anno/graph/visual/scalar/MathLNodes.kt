@file:Suppress("unused")

package me.anno.graph.visual.scalar

import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.FlowGraphNodeUtils.getLongInput
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import me.anno.utils.structures.maps.LazyMap

private val types = "Int,Long".split(',')
private val dataI1 = LazyMap { type: String ->
    MathNodeData(
        IntMathsUnary.entries,
        listOf(type), type,
        IntMathsUnary::id, IntMathsUnary::glsl
    )
}

class MathI1Node : TypedMathNode<IntMathsUnary>(dataI1, types) {
    override fun compute() {
        setOutput(0, enumType.long(getLongInput(0)))
    }
}

private val dataI2 = LazyMap { type: String ->
    MathNodeData(
        IntMathsBinary.entries,
        listOf(type, type), type,
        IntMathsBinary::id, IntMathsBinary::glsl
    )
}

class MathI2Node : TypedMathNode<IntMathsBinary>(dataI2, types), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, enumType.calculate(getLongInput(0), getLongInput(1)))
    }
}

private val dataI3 = LazyMap { type: String ->
    MathNodeData(
        IntMathsTernary.entries,
        listOf(type, type, type), type,
        IntMathsTernary::id, IntMathsTernary::glsl
    )
}

class MathI3Node : TypedMathNode<IntMathsTernary>(dataI3, types), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutput(0, enumType.long(getLongInput(0), getLongInput(1), getLongInput(2)))
    }
}