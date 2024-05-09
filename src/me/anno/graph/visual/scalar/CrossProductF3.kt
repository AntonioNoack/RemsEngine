@file:Suppress("unused")

package me.anno.graph.visual.scalar

import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

class CrossProductF2 : ComputeNode("Vector2f Cross", listOf("Vector2f", "A", "Vector2f", "B"), "Float"), GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int) = "cross2d"
    override fun defineShaderFunc(outputIndex: Int) = "(vec2 a, vec2 b){return a.x*b.y-a.y*b.x;}"
    override fun compute() {
        val a = getInput(0) as Vector2f
        val b = getInput(1) as Vector2f
        setOutput(0, a.cross(b))
    }
}

class CrossProductF3 : ComputeNode("Vector3f Cross", listOf("Vector3f", "A", "Vector3f", "B"), "Vector3f"),
    GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int): String = "cross"
    override fun compute() {
        val a = getInput(0) as Vector3f
        val b = getInput(1) as Vector3f
        setOutput(0, a.cross(b, Vector3f()))
    }
}

class CrossProductD2 : ComputeNode("Vector2d Cross", listOf("Vector2d", "A", "Vector2d", "B"), "Double"), GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int) = "cross2d"
    override fun defineShaderFunc(outputIndex: Int) = "(vec2 a, vec2 b){return a.x*b.y-a.y*b.x;}"
    override fun compute() {
        val a = getInput(0) as Vector2d
        val b = getInput(1) as Vector2d
        setOutput(0, a.cross(b))
    }
}

class CrossProductD3 : ComputeNode("Vector3f Cross", listOf("Vector3d", "A", "Vector3d", "B"), "Vector3d") {
    override fun compute() {
        val a = getInput(0) as Vector3d
        val b = getInput(1) as Vector3d
        setOutput(0, a.cross(b, Vector3d()))
    }
}
