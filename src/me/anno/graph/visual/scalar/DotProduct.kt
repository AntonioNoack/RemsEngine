@file:Suppress("unused")

package me.anno.graph.visual.scalar

import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.render.compiler.GLSLFuncNode
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

class DotProductF2 : ComputeNode("Vector2f Dot", listOf("Vector2f", "A", "Vector2f", "B"), "Float"), GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int): String = "dot"
    override fun compute() {
        val a = getInput(0) as Vector2f
        val b = getInput(1) as Vector2f
        setOutput(0, a.dot(b))
    }
}

class DotProductF3 : ComputeNode("Vector3f Dot", listOf("Vector3f", "A", "Vector3f", "B"), "Float"), GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int): String = "dot"
    override fun compute() {
        val a = getInput(0) as Vector3f
        val b = getInput(1) as Vector3f
        setOutput(0, a.dot(b))
    }
}

class DotProductF4 : ComputeNode("Vector4f Dot", listOf("Vector4f", "A", "Vector4f", "B"), "Float"), GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int): String = "dot"
    override fun compute() {
        val a = getInput(0) as Vector4f
        val b = getInput(1) as Vector4f
        setOutput(0, a.dot(b))
    }
}

class DotProductD2 : ComputeNode("Vector2d Dot", listOf("Vector2d", "A", "Vector2d", "B"), "Double"), GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int): String = "dot"
    override fun compute() {
        val a = getInput(0) as Vector2d
        val b = getInput(1) as Vector2d
        setOutput(0, a.dot(b))
    }
}

class DotProductD3 : ComputeNode("Vector3d Dot", listOf("Vector3d", "A", "Vector3d", "B"), "Double"), GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int): String = "dot"
    override fun compute() {
        val a = getInput(0) as Vector3d
        val b = getInput(1) as Vector3d
        setOutput(0, a.dot(b))
    }
}

class DotProductD4 : ComputeNode("Vector4d Dot", listOf("Vector4d", "A", "Vector4d", "B"), "Double"), GLSLFuncNode {
    override fun getShaderFuncName(outputIndex: Int): String = "dot"
    override fun compute() {
        val a = getInput(0) as Vector4d
        val b = getInput(1) as Vector4d
        setOutput(0, a.dot(b))
    }
}