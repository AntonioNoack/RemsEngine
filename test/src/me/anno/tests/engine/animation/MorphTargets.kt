package me.anno.tests.engine.animation

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
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
import me.anno.gpu.buffer.AttributeType
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
                Variable(GLSLType.V3F, "positions0", VariableMode.ATTR),
                Variable(GLSLType.V3F, "positions1", VariableMode.ATTR),
                Variable(GLSLType.V2F, "morph"),
                Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
            ), "localPosition =\n" +
                    "positions * (1.0-(morph.x+morph.y)) +\n" +
                    "positions0 * morph.x +\n" +
                    "positions1 * morph.y;\n"
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
}


private val pos0Type = Attribute("positions0", AttributeType.FLOAT, 3)
private val pos1Type = Attribute("positions1", AttributeType.FLOAT, 3)

var Mesh.positions0: FloatArray?
    get() = getAttr("positions0", FloatArray::class)
    set(value) = setAttr("positions0", value, pos0Type)

var Mesh.positions1: FloatArray?
    get() = getAttr("positions1", FloatArray::class)
    set(value) = setAttr("positions1", value, pos1Type)

/**
 * demonstrate how mesh morphing could be implemented on GPU
 * */
fun main() {

    val material = Material()
    material.shaderOverrides["morph"] = TypeValue(GLSLType.V2F, Vector2f(0.5f, 0f))

    val mesh = MorphMesh()
    IcosahedronModel.createIcosphere(4, 1f, mesh)
    mesh.materials = listOf(material.ref)

    mesh.positions0 = sphereToCube(mesh).positions
    mesh.positions1 = sphereToCylinder(mesh).positions

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