package me.anno.graph.visual.vector

import me.anno.graph.visual.CalculationNode
import me.anno.graph.visual.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.visual.scalar.GLSLFuncNode
import org.joml.Vector2f
import kotlin.math.cos
import kotlin.math.sin

// order should be the same as in JOML

class RotateF2Node : CalculationNode(
    "Vector2f Rotate",
    listOf("Vector2f", "Vector", "Vector2f", "Center", "Float", "Angle(Rad)"),
    "Vector2f"
), GLSLFuncNode {

    init {
        setInput(0, Vector2f())
        setInput(1, Vector2f())
        setInput(2, 0f)
    }

    override fun calculate(): Vector2f {
        val r = getInput(0) as Vector2f
        val q = getInput(1) as Vector2f
        val a = getFloatInput(2)
        val c = cos(a)
        val s = sin(a)
        val vx = r.x - q.x
        val vy = r.y - q.y
        val rx = c * vx - s * vy + q.x
        val ry = s * vx + c * vy + q.y
        return Vector2f(rx, ry)
    }

    override fun getShaderFuncName(outputIndex: Int) = "rot2"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec2 r, vec2 q, float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c)*(r-q)+q;}"
    }

}