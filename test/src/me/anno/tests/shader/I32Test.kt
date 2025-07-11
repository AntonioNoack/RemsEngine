package me.anno.tests.shader

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.InstancedI32Stack
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector3f
import kotlin.math.sin

class TestI32Stack(val space: Float) : InstancedI32Stack(
    MeshInstanceData(
        listOf(
            ShaderStage(
                "i32-pos", listOf(
                    Variable(GLSLType.V1I, "instanceI32", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.M4x3, "localTransform"),
                    Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                ),
                "vec3 tmpPosition = localPosition;\n" +
                        "tmpPosition.z += float(instanceI32)*$space;\n" +
                        "finalPosition = matMul(localTransform, vec4(tmpPosition, 1.0));\n"
            )
        ),
        MeshInstanceData.DEFAULT.transformNorTan,
        MeshInstanceData.DEFAULT.transformColors,
        MeshInstanceData.DEFAULT.transformMotionVec
    )
)

/**
 * Shows how to use an i32-MeshSpawner
 *
 * todo bug: this has become invisible
 * */
fun main() {

    val inch = 0.5f // 2.54e-2f
    val width = 17f / 16f * inch
    val height = 35f / 16f * inch
    val thickness = 7f / 16f * inch
    val space = (width + height).toDouble()

    val mesh = flatCube.linear(Vector3f(), Vector3f(width, height, thickness)).front
    val material = defaultMaterial

    val spawner = object : MeshSpawner() {

        val maxCount = 30
        val count get() = ((sin(Time.gameTime) * 0.5 + 0.5) * maxCount).roundToIntOr()

        override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
            // the size is changing constantly, so it would be best to calculate the maximum size
            // if you're too lazy, use aabb.all()
            val dx = width.toDouble()
            val dy = height.toDouble()
            dstUnion.setMin(-dx, -dy, -thickness.toDouble())
            dstUnion.setMax(+dx, +dy, (maxCount - 1) * space + thickness.toDouble())
        }

        override fun forEachMesh(callback: (IMesh, Material?, Transform) -> Boolean) {
            for (i in 0 until count) {
                val tr = getTransform(i)
                tr.setLocalPosition(0.0, 0.0, i * space)
                if (callback(mesh, material, tr)) break
            }
        }

        override fun fill(pipeline: Pipeline, transform: Transform) {
            clickId = pipeline.getClickId(this)
            val stack = getOrPutI32Stack(pipeline, mesh, material, TestI32Stack::class) {
                TestI32Stack(space.toFloat())
            }
            val buffer = stack.start(gfxId, transform.globalTransform)
            for (i in 0 until count) buffer.add(i)
        }
    }

    testSceneWithUI("I32 Spawner", Entity().add(spawner))
}