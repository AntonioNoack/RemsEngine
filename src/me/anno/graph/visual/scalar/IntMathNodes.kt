package me.anno.graph.visual.scalar

import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import me.anno.utils.structures.maps.LazyMap

private val types = "Int,Long".split(',')
private val dataI1 = LazyMap { type: String ->
    MathNodeData(
        IntMathUnary.entries,
        listOf(type), type,
        IntMathUnary::id, IntMathUnary::glsl
    )
}

private fun Node.setOutputI(v: Long) {
    setOutput(0, if (outputs[0].type == "Int") v.toInt() else v)
}

class MathI1Node : TypedMathNode<IntMathUnary>(dataI1, types) {
    override fun compute() {
        setOutputI(enumType.long(getLongInput(0)))
    }
}

private val dataI2 = LazyMap { type: String ->
    MathNodeData(
        IntMathBinary.entries,
        listOf(type, type), type,
        IntMathBinary::id, IntMathBinary::glsl
    )
}

class MathI2Node : TypedMathNode<IntMathBinary>(dataI2, types), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutputI(enumType.calculate(getLongInput(0), getLongInput(1)))
    }
}

private val dataI3 = LazyMap { type: String ->
    MathNodeData(
        IntMathTernary.entries,
        listOf(type, type, type), type,
        IntMathTernary::id, IntMathTernary::glsl
    )
}

class MathI3Node : TypedMathNode<IntMathTernary>(dataI3, types), EnumNode, GLSLFuncNode {
    override fun compute() {
        setOutputI(enumType.long(getLongInput(0), getLongInput(1), getLongInput(2)))
    }
}