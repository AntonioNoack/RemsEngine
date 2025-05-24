package me.anno.tests.engine.animation

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.HelperMesh.Companion.updateHelperMeshes
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshBufferUtils.addNormalAttribute
import me.anno.ecs.components.mesh.MeshBufferUtils.addUVAttributes
import me.anno.ecs.components.mesh.MeshBufferUtils.putNormal
import me.anno.ecs.components.mesh.MeshBufferUtils.putPosition
import me.anno.ecs.components.mesh.MeshBufferUtils.putTangent
import me.anno.ecs.components.mesh.MeshBufferUtils.putUVs
import me.anno.ecs.components.mesh.MeshBufferUtils.replaceBuffer
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.ecs.components.mesh.utils.MeshVertexData.Companion.flatNormalsFragment
import me.anno.ecs.components.mesh.utils.MeshVertexData.Companion.flatNormalsNorTan
import me.anno.ecs.components.mesh.utils.MeshVertexData.Companion.noColors
import me.anno.ecs.components.mesh.utils.MorphTarget
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// todo morph-targets probably should be implemented using textures like skeletal animations, so they are supported on all devices,
//  and be limited to four at a time, too

fun sphereToCube(mesh: Mesh): MorphTarget {
    val positions = mesh.positions!!.copyOf()
    val base = Vector3f()
    val norm = Vector3f()
    for (i in positions.indices step 3) {
        base.set(positions, i)
        norm.set(base).div(max(abs(base.x), max(abs(base.y), abs(base.z))))
        norm.get(positions, i)
    }
    return MorphTarget("s2c", positions)
}

fun sphereToCylinder(mesh: Mesh): MorphTarget {
    val positions = mesh.positions!!.copyOf()
    val base = Vector3f()
    val norm = Vector3f()
    for (i in positions.indices step 3) {
        base.set(positions, i)
        norm.set(base).div(max(length(base.x, base.z), abs(base.y)))
        norm.get(positions, i)
    }
    return MorphTarget("s2c", positions)
}

val morphVertexData = MeshVertexData(
    listOf(
        ShaderStage(
            "morph-lp", listOf(
                Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
                Variable(GLSLType.V3F, "coords0", VariableMode.ATTR),
                Variable(GLSLType.V3F, "coords1", VariableMode.ATTR),
                Variable(GLSLType.V2F, "morph"),
                Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
            ), "localPosition =\n" +
                    "positions * (1.0-(morph.x+morph.y)) +\n" +
                    "coords0 * morph.x +\n" +
                    "coords1 * morph.y;\n"
        )
    ),
    listOf(flatNormalsNorTan),
    listOf(noColors),
    MeshVertexData.DEFAULT.loadMotionVec, // todo would need to be modified, prevMorph
    listOf(flatNormalsFragment),
)

class MorphMesh : Mesh() {
    override val vertexData: MeshVertexData
        get() = morphVertexData

    override fun createMeshBuffer() {
        createMeshBufferImpl1()
    }
}

fun Mesh.createMeshBufferImpl1() {
    needsMeshUpdate = false

    // not the safest, but well...
    val positions = positions ?: return
    if (positions.isEmpty()) return

    val morphs = morphTargets.map { it.positions }

    ensureNorTanUVs()

    val normals = normals!!
    val tangents = tangents

    val uvs = uvs
    val hasUVs = hasUVs

    val vertexCount = min(positions.size, normals.size) / 3
    val indices = indices
    hasBonesInBuffer = false
    hasVertexColors = 0

    val hasHighPrecisionNormals = hasHighPrecisionNormals

    val attributes = ArrayList<Attribute>()
    attributes += Attribute("positions", 3)
    for (i in morphs.indices) {
        attributes += Attribute("coords$i", 3)
    }
    addNormalAttribute(attributes, hasHighPrecisionNormals)
    if (hasUVs) addUVAttributes(attributes)

    val name = refOrNull?.absolutePath ?: name.ifEmpty { "Mesh" }
    val buffer = replaceBuffer(name, attributes, vertexCount, buffer)
    buffer.drawMode = drawMode
    this.buffer = buffer

    triBuffer = replaceBuffer(buffer, indices, triBuffer)
    triBuffer?.drawMode = drawMode

    for (i in 0 until vertexCount) {

        // upload all data of one vertex

        val i3 = i * 3
        val i4 = i * 4

        putPosition(buffer, positions, i3)
        for (mi in morphs.indices) {
            putPosition(buffer, morphs[mi], i3)
        }
        putNormal(buffer, normals, i3, hasHighPrecisionNormals)

        if (hasUVs) {
            putUVs(buffer, uvs, i * 2)
            putTangent(buffer, tangents, i4)
        }
    }

    updateHelperMeshes()
}

/**
 * demonstrate how mesh morphing could be implemented on GPU
 * */
fun main() {

    val material = Material()
    material.shaderOverrides["morph"] = TypeValue(GLSLType.V2F, Vector2f(0.5f, 0f))

    val mesh = MorphMesh()
    IcosahedronModel.createIcosphere(4, 1f, mesh)
    mesh.materials = listOf(material.ref)

    mesh.morphTargets = listOf(
        sphereToCube(mesh),
        sphereToCylinder(mesh)
    )

    val scene = Entity()
        .add(MeshComponent(mesh))
        .add(object : Component(), OnUpdate {
            override fun onUpdate() {
                val t = (Time.gameTime % 4.0).toFloat()
                val x = clamp(1.17f - abs(t - 1.33f))
                val y = clamp(1.17f - abs(t - 2.67f))
                material.shaderOverrides["morph"] = TypeValue(GLSLType.V2F, Vector2f(x, y))
            }
        })
    testSceneWithUI("Morphing", scene)
}