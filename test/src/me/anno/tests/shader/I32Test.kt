package me.anno.tests.shader

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshInstanceData
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.pipeline.InstancedI32Stack
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.mesh.Shapes.flatCube
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3f
import kotlin.math.sin

class TestI32Stack(val space: Float) : InstancedI32Stack(
    MeshInstanceData(
        listOf(
            ShaderStage(
                "i32-pos", listOf(
                    Variable(GLSLType.V1I, "instanceI32"),
                    Variable(GLSLType.V3F, "localPosition"),
                    Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                ),
                "finalPosition = localPosition + vec3(0.0,0.0,float(instanceI32)*$space);\n"
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
 * todo this is broken :(
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

        val count get() = ((sin(Time.gameTime) + 1.0) * 30).toInt()

        override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
            // the size is changing constantly, so it would be best to calculate the maximum size
            // for this sample, we just set it to always render
            aabb.all()
            return true
        }

        override fun forEachMesh(run: (Mesh, Material?, Transform) -> Unit) {
            for (i in 0 until count) {
                val tr = getTransform(i)
                tr.setLocalPosition(0.0, 0.0, i * space)
                run(mesh, material, tr)
            }
        }

        override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
            lastDrawn = Time.gameTimeN
            this.clickId = clickId
            val stack = getOrPutI32Stack(pipeline, mesh, material, TestI32Stack::class) {
                TestI32Stack(space.toFloat())
            }
            val buffer = stack.begin(gfxId, transform!!.globalTransform)
            for (i in 0 until count) buffer.add(i)
            stack.end()
            return clickId + 1
        }
    }

    testSceneWithUI("I32 Spawner", Entity(spawner))
}