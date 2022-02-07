package me.anno.remsstudio.objects.meshes

import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.shader.ShaderLib
import me.anno.mesh.MeshData
import me.anno.mesh.MeshUtils
import me.anno.mesh.assimp.AnimGameItem
import me.anno.remsstudio.objects.GFXTransform
import org.joml.Matrix4fArrayList
import org.joml.Matrix4x3fArrayList
import org.joml.Vector4fc

object MeshDataV2 {

    fun MeshData.drawAssimp2(
        useECSShader: Boolean,
        transform: GFXTransform?,
        stack: Matrix4fArrayList,
        time: Double,
        color: Vector4fc,
        animationName: String,
        useMaterials: Boolean,
        centerMesh: Boolean,
        normalizeScale: Boolean,
        drawSkeletons: Boolean
    ) {

        val baseShader = if (useECSShader) ECSShaderLib.pbrModelShader else ShaderLib.shaderAssimp
        val shader = baseShader.value
        shader.use()
        GFXx3D.shader3DUniforms(shader, stack, color)
        GFXTransform.uploadAttractors(transform, shader, time)

        val model0 = assimpModel!!
        val animation = model0.animations[animationName]
        val skinningMatrices = if (animation != null) {
            model0.uploadJointMatrices(shader, animation, time)
        } else null
        shader.v1b("hasAnimation", skinningMatrices != null)

        val localStack = Matrix4x3fArrayList()

        if (normalizeScale) {
            val scale = AnimGameItem.Companion.getScaleFromAABB(model0.staticAABB.value)
            localStack.scale(scale)
        }

        if (centerMesh) {
            MeshUtils.centerMesh(transform, stack, localStack, model0)
        }

        GFXx3D.transformUniform(shader, stack)

        drawHierarchy(
            shader,
            localStack,
            skinningMatrices,
            color,
            model0,
            model0.hierarchy,
            useMaterials,
            drawSkeletons
        )

        // todo line mode: draw every mesh as lines
        // todo draw non-indexed as lines: use an index buffer
        // todo draw indexed as lines: use a geometry shader, which converts 3 vertices into 3 lines

    }

}