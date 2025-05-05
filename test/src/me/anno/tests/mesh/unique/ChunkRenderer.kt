package me.anno.tests.mesh.unique

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.unique.UniqueMeshRenderer
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.io.files.FileReference
import me.anno.tests.utils.TestWorld
import me.anno.utils.Color.convertABGR2ARGB
import org.joml.Vector3i

class ChunkRenderer(val material: Material, val world: TestWorld) :
    UniqueMeshRenderer<Vector3i, Mesh>(attributes, blockVertexData, DrawMode.TRIANGLES) {

    companion object {
        val attributes = bind(
            Attribute("positions", 3),
            Attribute("colors0", AttributeType.UINT8_NORM, 4)
        )
        val blockVertexData = MeshVertexData(
            listOf(
                ShaderStage(
                    "positions", listOf(
                        Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
                    ), "localPosition = positions;\n"
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
                // calculate normals using cross product
                ShaderStage(
                    "px-nor", listOf(
                        Variable(GLSLType.V3F, "finalPosition"),
                        Variable(GLSLType.V3F, "normal", VariableMode.OUT)
                    ), "normal = normalize(cross(dFdx(finalPosition), dFdy(finalPosition)));\n"
                )
            ),
        )
    }

    val csx = world.sizeX
    val csy = world.sizeY
    val csz = world.sizeZ

    override val hasVertexColors: Int get() = 1
    override val materials: List<FileReference> = listOf(material.ref)
    override val numMaterials: Int get() = 1

    override fun getTransformAndMaterial(key: Vector3i, transform: Transform): Material {
        transform.setLocalPosition(
            (key.x * csx).toDouble(),
            (key.y * csy).toDouble(),
            (key.z * csz).toDouble(),
        )
        transform.teleportUpdate()
        return material
    }

    override fun getData(key: Vector3i, mesh: Mesh): StaticBuffer? {
        if (mesh.numPrimitives == 0L) return null
        val pos = mesh.positions!!
        val col = mesh.color0!!
        val buffer = StaticBuffer("chunk$key", attributes, pos.size / 3)
        val data = buffer.getOrCreateNioBuffer()
        val dx = key.x * csx
        val dy = key.y * csy
        val dz = key.z * csz
        for (i in 0 until buffer.vertexCount) {
            data.putFloat(dx + pos[i * 3])
            data.putFloat(dy + pos[i * 3 + 1])
            data.putFloat(dz + pos[i * 3 + 2])
            data.putInt(convertABGR2ARGB(col[i]))
        }
        buffer.isUpToDate = false
        return buffer
    }
}