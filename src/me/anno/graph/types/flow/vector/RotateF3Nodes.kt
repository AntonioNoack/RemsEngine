package me.anno.graph.types.flow.vector

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.CalculationNode
import me.anno.graph.types.flow.maths.GLSLExprNode
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

// order should be the same as in JOML

class RotateF3XNode : CalculationNode(
    "Vector3f RotateX",
    listOf("Vector3f", "Vector", "Vector3f", "Center", "Float", "Angle(Rad)"),
    "Vector3f"
), GLSLExprNode {

    init {
        setInput(0, Vector3f())
        setInput(1, Vector3f())
        setInput(2, 0f)
    }

    override fun calculate(graph: FlowGraph): Vector3f {
        val r = getInput(graph, 0) as Vector3f
        val q = getInput(graph, 1) as Vector3f
        val a = getInput(graph, 2) as Float
        val c = cos(a)
        val s = sin(a)
        val vx = r.y - q.y
        val vy = r.z - q.z
        val ry = c * vx - s * vy + q.y
        val rz = s * vx + c * vy + q.z
        return Vector3f(r.x, ry, rz)
    }

    override fun getShaderFuncName(outputIndex: Int) = "rot3x"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec3 r, vec3 q, float a){float c=cos(a),s=sin(a);r.yz=mat2(c,-s,s,c)*(r.yz-q.yz)+q.yz;return r;}"
    }

}

class RotateF3YNode : CalculationNode(
    "Vector3f RotateY",
    listOf("Vector3f", "Vector", "Vector3f", "Center", "Float", "Angle(Rad)"),
    "Vector3f"
), GLSLExprNode {

    init {
        setInput(0, Vector3f())
        setInput(1, Vector3f())
        setInput(2, 0f)
    }

    override fun calculate(graph: FlowGraph): Vector3f {
        val r = getInput(graph, 0) as Vector3f
        val q = getInput(graph, 1) as Vector3f
        val a = getInput(graph, 2) as Float
        val c = cos(a)
        val s = sin(a)
        val vx = r.z - q.z
        val vy = r.x - q.x
        // correct? should be :)
        val rz = c * vx - s * vy + q.z
        val rx = s * vx + c * vy + q.x
        return Vector3f(rx, r.y, rz)
    }

    override fun getShaderFuncName(outputIndex: Int) = "rot3y"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec3 r, vec3 q, float a){float c=cos(a),s=sin(a);r.zx=mat2(c,-s,s,c)*(r.zx-q.zx)+q.xz;return r;}"
    }

}

class RotateF3ZNode : CalculationNode(
    "Vector3f RotateZ",
    listOf("Vector3f", "Vector", "Vector3f", "Center", "Float", "Angle(Rad)"),
    "Vector3f"
), GLSLExprNode {

    init {
        setInput(0, Vector3f())
        setInput(1, Vector3f())
        setInput(2, 0f)
    }

    override fun calculate(graph: FlowGraph): Vector3f {
        val r = getInput(graph, 0) as Vector3f
        val q = getInput(graph, 1) as Vector3f
        val a = getInput(graph, 2) as Float
        val c = cos(a)
        val s = sin(a)
        val vx = r.x - q.x
        val vy = r.y - q.y
        // correct? should be :)
        val rx = c * vx - s * vy + q.x
        val ry = s * vx + c * vy + q.y
        return Vector3f(rx, ry, r.z)
    }

    override fun getShaderFuncName(outputIndex: Int) = "rot3z"
    override fun defineShaderFunc(outputIndex: Int): String {
        return "(vec3 r, vec3 q, float a){float c=cos(a),s=sin(a);r.xy=mat2(c,-s,s,c)*(r.xy-q.xy)+q.xy;return r;}"
    }

}