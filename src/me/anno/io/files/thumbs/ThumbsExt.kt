package me.anno.io.files.thumbs

import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.shader.Shader
import me.anno.mesh.MeshUtils.centerMesh
import me.anno.mesh.assimp.AnimGameItem
import me.anno.utils.pooling.JomlPools
import org.joml.*
import org.joml.Math
import kotlin.math.max

object ThumbsExt {

    var defaultAngleY = -25f

    fun createPerspective(y: Float, aspectRatio: Float, stack: Matrix4f) {

        Perspective.setPerspective(stack, 0.7f, aspectRatio, 0.001f, 10f, 0f, 0f)
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateX(Math.toRadians(15f))// rotate it into a nice viewing angle
        stack.rotateY(Math.toRadians(y))

        // calculate the scale, such that everything can be visible
        // half, because it's half the size, 1.05f for a small border
        stack.scale(1.05f * 0.5f)

    }

    fun createPerspectiveList(y: Float, aspectRatio: Float): Matrix4fArrayList {
        val stack = Matrix4fArrayList()
        createPerspective(y, aspectRatio, stack)
        return stack
    }

    fun createPerspective(y: Float, aspectRatio: Float): Matrix4f {
        val stack = Matrix4f()
        createPerspective(y, aspectRatio, stack)
        return stack
    }

    fun Mesh.drawAssimp(
        stack: Matrix4f,
        comp: MeshComponentBase?,
        useMaterials: Boolean,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ) {

        val shader = ECSShaderLib.pbrModelShader.value
        shader.use()

        val localStack = if (normalizeScale || centerMesh) {
            val localStack = Matrix4x3f()
            if (normalizeScale) {
                val scale = AnimGameItem.getScaleFromAABB(aabb)
                localStack.scale(scale)
            }
            if (centerMesh) {
                centerMesh(stack, localStack, this)
            }
            localStack
        } else null

        val mesh = this
        val materials = materials
        val materialOverrides = comp?.materials ?: emptyList()

        if (useMaterials && (materials.isNotEmpty() || materialOverrides.isNotEmpty())) {
            for (index in materials.indices) {
                val m0 = materialOverrides.getOrNull(index)?.nullIfUndefined()
                val m1 = m0 ?: materials.getOrNull(index)
                val material = MaterialCache[m1, Mesh.defaultMaterial]
                val shader2 = material.shader?.value ?: shader
                bindShader(shader2, stack, localStack)
                material.defineShader(shader2)
                mesh.draw(shader2, index)
            }
        } else {
            bindShader(shader, stack, localStack)
            val material = Mesh.defaultMaterial
            material.defineShader(shader)
            for (materialIndex in 0 until max(1, materials.size)) {
                mesh.draw(shader, materialIndex)
            }
        }
    }

    fun bindShader(shader: Shader, stack: Matrix4f, localStack: Matrix4x3f?) {
        shader.use()
        shaderColor(shader, "tint", -1)
        shader.v1b("hasAnimation", false)
        shader.m4x4("transform", stack)
        shader.m4x3("localTransform", localStack)
        shader.v1f("worldScale", 1f)
        GFXx3D.shader3DUniforms(shader, stack, -1)
        GFXx3D.uploadAttractors0(shader)
    }

    fun Collider.drawAssimp(
        stack: Matrix4f,
        localStack: Matrix4x3f?
    ) {
        drawShape()
        finishLines(stack, localStack)
    }

    fun Collider.findLocalStack(
        stack: Matrix4f,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ): Matrix4x3f? {
        return if (normalizeScale || centerMesh) {
            val aabb = AABBd()
            fillSpace(Matrix4x3d(), aabb)
            val localStack = Matrix4x3f()
            if (normalizeScale) {
                val scale = AnimGameItem.getScaleFromAABB(aabb)
                localStack.scale(scale)
            }
            if (centerMesh) {
                centerMesh(stack, localStack, this)
            }
            localStack
        } else null
    }

    fun Collider.drawAssimp(
        stack: Matrix4f,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ) {
        val localStack = findLocalStack(stack, centerMesh, normalizeScale)
        drawAssimp(stack, localStack)
    }

    fun finishLines(cameraMatrix: Matrix4f, worldMatrix: Matrix4x3f? = null): Boolean {
        return if (LineBuffer.bytes.position() > 0) {
            if (worldMatrix == null) {
                LineBuffer.finish(cameraMatrix)
            } else {
                val m = JomlPools.mat4f.create()
                m.set(cameraMatrix)
                m.mul(worldMatrix)
                LineBuffer.finish(m)
                JomlPools.mat4f.sub(1)
            }
            true
        } else false
    }

}