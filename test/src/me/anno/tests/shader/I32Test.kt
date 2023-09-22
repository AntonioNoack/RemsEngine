package me.anno.tests.shader

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector3f
import kotlin.math.sin

/**
 * Shows how to create an i32-MeshSpawner
 *
 * test build-up/changing i32 size
 * -> fixed a bug :)
 * */
fun main() {

    val inch = 0.5f // 2.54e-2f
    val width = 17f / 16f * inch
    val height = 35f / 16f * inch
    val thickness = 7f / 16f * inch
    val space = (width + height).toDouble()

    val mesh = flatCube.linear(Vector3f(), Vector3f(width, height, thickness)).front

    val shader1 = object : ECSMeshShader("i32") {
        override fun createVertexStages(flags: Int): List<ShaderStage> {
            val defines = createDefines(flags)
            val variables = createVertexVariables(flags) +
                    Variable(GLSLType.V1I, "instanceI32", VariableMode.ATTR)
            val stage = ShaderStage(
                "vertex",
                variables, defines.toString() +
                        "localPosition = coords + vec3(0.0,0.0,float(instanceI32)*$space);\n" + // is output, so no declaration needed
                        motionVectorInit +

                        instancedInitCode +

                        animCode0() +
                        normalInitCode +
                        animCode1 +

                        applyTransformCode +
                        colorInitCode +
                        "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                        motionVectorCode +
                        ShaderLib.positionPostProcessing
            )
            return listOf(stage)
        }
    }

    val material = Material().apply { shader = shader1 }
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

        override fun forEachMeshGroupI32(run: (Mesh, Material?, Matrix4x3d) -> ExpandingIntArray): Boolean {
            val list = run(mesh, material, transform!!.globalTransform)
            for (i in 0 until count) list.add(i)
            return true
        }
    }

    testSceneWithUI("I32 Spawner", Entity(spawner))

}