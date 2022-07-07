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
import me.anno.mesh.assimp.AnimGameItem.Companion.getScaleFromAABB
import me.anno.utils.LOGGER
import me.anno.utils.pooling.JomlPools
import org.joml.*
import kotlin.math.max

object ThumbsExt {

    fun createCameraMatrix(aspectRatio: Float): Matrix4f {
        val stack = Matrix4f()
        Perspective.setPerspective(stack, 0.7f, aspectRatio, 0.001f, 10f, 0f, 0f)
        return stack
    }

    fun createModelMatrix(): Matrix4x3f {
        val stack = Matrix4x3f()
        stack.translate(0f, 0f, -1f)// move the camera back a bit
        stack.rotateX(Math.toRadians(15f))// rotate it into a nice viewing angle
        stack.rotateY(Math.toRadians(-25f))
        // calculate the scale, such that everything can be visible
        // half, because it's half the size, 1.05f for a small border
        stack.scale(1.05f * 0.5f)
        return stack
    }

    fun Mesh.drawAssimp(
        aspectRatio: Float,
        comp: MeshComponentBase?,
        useMaterials: Boolean,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ) = drawAssimp(
        createCameraMatrix(aspectRatio),
        createModelMatrix(), comp,
        useMaterials, centerMesh,
        normalizeScale
    )

    fun Mesh.drawAssimp(
        cameraMatrix: Matrix4f,
        modelMatrix: Matrix4x3f,
        comp: MeshComponentBase?,
        useMaterials: Boolean,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ) {

        val shader = ECSShaderLib.pbrModelShader.value
        shader.use()

        if (normalizeScale || centerMesh) {
            if (normalizeScale) modelMatrix.scale(getScaleFromAABB(aabb))
            if (centerMesh) centerMesh(cameraMatrix, modelMatrix, this)
        }

        val materials0 = materials
        val materials1 = comp?.materials

        if (useMaterials && (materials0.isNotEmpty() || (materials1 != null && materials1.isNotEmpty()))) {
            for (index in materials0.indices) {
                val m0 = materials1?.getOrNull(index)?.nullIfUndefined()
                val m1 = m0 ?: materials0.getOrNull(index)
                val material = MaterialCache[m1, Mesh.defaultMaterial]
                val shader2 = material.shader?.value ?: shader
                bindShader(shader2, cameraMatrix, modelMatrix)
                material.bind(shader2)
                draw(shader2, index)
            }
        } else {
            bindShader(shader, cameraMatrix, modelMatrix)
            val material = Mesh.defaultMaterial
            material.bind(shader)
            for (materialIndex in 0 until max(1, materials0.size)) {
                draw(shader, materialIndex)
            }
        }

    }

    fun bindShader(shader: Shader, cameraMatrix: Matrix4f, modelMatrix: Matrix4x3f) {
        shader.use()
        shaderColor(shader, "tint", -1)
        shader.v1b("hasAnimation", false)
        shader.m4x3("localTransform", modelMatrix)
        shader.v1f("worldScale", 1f)
        GFXx3D.shader3DUniforms(shader, cameraMatrix, -1)
        GFXx3D.uploadAttractors0(shader)
    }

    fun Collider.drawAssimp(
        stack: Matrix4f,
        localStack: Matrix4x3f?
    ) {
        drawShape()
        finishLines(stack, localStack)
    }

    fun Collider.findModelMatrix(
        cameraMatrix: Matrix4f,
        modelMatrix: Matrix4x3f,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ): Matrix4x3f {
        if (normalizeScale || centerMesh) {
            val aabb = AABBd()
            fillSpace(Matrix4x3d(), aabb)
            if (normalizeScale) modelMatrix.scale(getScaleFromAABB(aabb))
            if (centerMesh) centerMesh(cameraMatrix, modelMatrix, this)
        }
        return modelMatrix
    }

    fun Collider.drawAssimp(
        cameraMatrix: Matrix4f,
        modelMatrix: Matrix4x3f,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ) {
        findModelMatrix(cameraMatrix, modelMatrix, centerMesh, normalizeScale)
        drawAssimp(cameraMatrix, modelMatrix)
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