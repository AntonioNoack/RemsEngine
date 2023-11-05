package me.anno.tests.mesh.unique

import me.anno.Build
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshVertexData
import me.anno.ecs.components.mesh.UniqueMeshRenderer
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths
import me.anno.maths.patterns.SpiralPattern.spiral2d
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.tests.utils.TestWorld
import me.anno.utils.hpc.ProcessingQueue
import org.joml.Vector3i

fun main() {

    Build.isDebug = false

    // todo load/unload a big voxel world without any stutter
    val attributes = listOf(
        Attribute("coords", AttributeType.SINT32, 3, true),
        Attribute("colors0", AttributeType.UINT8_NORM, 4)
    )
    val vertexData = MeshVertexData(
        listOf(
            ShaderStage(
                "coords", listOf(
                    Variable(GLSLType.V3I, "coords", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
                ), "" +
                        "localPosition = vec3(coords);\n" // stupidly simple xD
            )
        ),
        listOf(
            ShaderStage(
                "nor", listOf(
                    Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                    Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
                ), "normal = vec3(0.0); tangent = vec4(0.0);\n"
            )
        ),
        MeshVertexData.DEFAULT.loadColors,
        MeshVertexData.DEFAULT.loadMotionVec,
        listOf(
            // calculate normals from cross product
            ShaderStage(
                "px-nor", listOf(
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.V3F, "normal", VariableMode.OUT)
                ), "" +
                        // todo check front/back
                        "normal = normalize(cross(dFdx(finalPosition), dFdy(finalPosition)));\n"
            )
        ),
    )
    val world = TestWorld
    val csx = 64
    val csy = 32
    // todo load world in spiral pattern
    val urm = object : UniqueMeshRenderer<Vector3i>(attributes, vertexData, defaultMaterial, DrawMode.TRIANGLES) {
        override val hasVertexColors: Int get() = 1
        override fun getData(key: Vector3i, mesh: Mesh): StaticBuffer? {
            if (mesh.numPrimitives == 0L) return null
            val pos = mesh.positions!!
            val col = mesh.color0!!
            val buffer = StaticBuffer("cnk", attributes, pos.size / 3)
            val data = buffer.nioBuffer!!
            val dx = key.x * csx
            val dy = key.y * csy
            val dz = key.z * csx
            for (i in 0 until buffer.vertexCount) {
                data.putInt(dx + pos[i * 3].toInt())
                data.putInt(dy + pos[i * 3 + 1].toInt())
                data.putInt(dz + pos[i * 3 + 2].toInt())
                data.putInt(Maths.convertABGR2ARGB(col[i]))
            }
            buffer.isUpToDate = false
            return buffer
        }
    }
    val scene = Entity("Scene")
    scene.add(urm)
    scene.add(object : Component() {

        val worker = ProcessingQueue("chunks")
        val elements = spiral2d(100, 0, true).iterator()

        fun generate(idx: Vector3i) {
            val x0 = idx.x * csx
            val y0 = idx.y * csy
            val z0 = idx.z * csx
            val mesh = world.createTriangleMesh2(x0, y0, z0, csx, csy, csx)
            addEvent { // change back to GPU thread
                urm.add(idx, mesh)
            }
        }

        override fun onUpdate(): Int {
            // load next mesh
            if (worker.remaining == 0) {
                if (elements.hasNext()) {
                    val idx = elements.next()
                    worker += { generate(idx) }
                }
            }
            return 1
        }
    })
    testSceneWithUI("Unique Mesh Renderer", scene)
}