package me.anno.graph.visual.scalar

import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.MaterialGraph.convert
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.graph.visual.vector.vectorTypes
import me.anno.utils.Logging.hash32raw
import me.anno.utils.structures.maps.LazyMap

class CompareNode() : TypedMathNode<CompareMode>(data, types) {

    constructor(type: String) : this() {
        setDataType(type)
    }

    fun apply(delta: Int): Boolean {
        return when (enumType) {
            CompareMode.LESS_THAN -> delta < 0
            CompareMode.LESS_OR_EQUALS -> delta <= 0
            CompareMode.EQUALS -> delta == 0
            CompareMode.GREATER_THAN -> delta > 0
            CompareMode.GREATER_OR_EQUALS -> delta >= 0
            CompareMode.NOT_EQUALS -> delta != 0
            else -> false
        }
    }

    fun compare(a: Any?, b: Any?): Int {
        if (a == b) return 0
        if (a == null) return -compare(b, null)
        if (a is Comparable<*>) {
            try {
                @Suppress("UNCHECKED_CAST")
                a as Comparable<Any?>
                return a.compareTo(b)
            } catch (_: Exception) {
            }
        }
        val ha = hash32raw(a)
        val hb = hash32raw(b)
        if (ha == hb) return -1
        return ha.compareTo(hb)
    }

    override fun compute() {
        val a = getInput(0)
        val b = getInput(1)
        val v = when (enumType) {
            CompareMode.IDENTICAL -> a === b
            CompareMode.NOT_IDENTICAL -> a !== b
            else -> apply(compare(a, b))
        }
        setOutput(0, v)
    }

    override fun buildExprCode(g: GraphCompiler, out: NodeOutput, n: Node) {
        val inputs = n.inputs
        val symbol = enumType.glsl
        val an = inputs[0]
        val bn = inputs[1]
        g.builder.append('(')
        g.expr(an) // first component
        g.builder.append(')').append(symbol).append('(')
        convert(g.builder, bn.type, g.aType(an, bn)) { g.expr(bn) }!! // second component
        g.builder.append(')')
    }

    companion object {
        private val data = LazyMap<String, MathNodeData<CompareMode>> { type ->
            MathNodeData(
                CompareMode.entries, listOf(type, type), "Boolean",
                CompareMode::id, CompareMode::glsl
            )
        }
        private val types = "?,String,Boolean,Float,Int,Double,Long".split(',') + vectorTypes
    }
}