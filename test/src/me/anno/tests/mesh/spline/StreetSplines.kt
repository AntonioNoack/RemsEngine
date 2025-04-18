package me.anno.tests.mesh.spline

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.HelperMesh.Companion.updateHelperMeshes
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshIterators.forEachLineIndex
import me.anno.ecs.components.mesh.MeshIterators.forEachPointIndex
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineCrossing
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAU
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.Color.a
import me.anno.utils.Color.argb
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.toARGB
import me.anno.utils.OS.res
import org.joml.Vector3d
import org.joml.Vector4f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun mulARGB(ca: Int, cb: Vector4f): Int {
    val a = (ca.a() * cb.w).toInt()
    val r = (ca.r() * cb.x).toInt()
    val g = (ca.g() * cb.y).toInt()
    val b = (ca.b() * cb.z).toInt()
    return argb(a, r, g, b)
}

/**
 * function to extract colors from multiple materials onto single material and vertex colors
 * */
fun mergeMaterials(mesh: Mesh): Mesh {
    val n = mesh.numMaterials
    if (n < 2) return mesh
    mesh.updateHelperMeshes()
    val clone = mesh.clone() as Mesh
    val materials = (0 until n).map {
        MaterialCache[mesh.materials.getOrNull(it)] ?: Material.defaultMaterial
    }
    val materialToTint = materials.map { it.diffuseBase }
    val colors = IntArray(mesh.positions!!.size / 3)
    val baseColor = mesh.color0
    val helperMeshes = mesh.helperMeshes!!
    materialToTint.mapIndexed { mi, tint ->
        val tintRGB = tint.toARGB()
        helperMeshes[mi]?.forEachPointIndex { pi ->
            colors[pi] = if (baseColor != null) {
                mulARGB(baseColor[pi], tint)
            } else tintRGB
            false
        }
    }
    clone.color0 = colors
    clone.hasVertexColors = 1
    val material0 = materials[0].clone() as Material
    material0.unlinkPrefab()
    material0.diffuseBase = Vector4f(1f)
    clone.numMaterials = 1
    clone.materials = listOf(material0.ref)
    clone.materialIds = null
    clone.helperMeshes = null
    return clone
}

/**
 * extract all triangles with this material into a line...:
 *    only keep points with z ~ 0, extract x,y
 *    only track lines, where all points are on the line
 * */
fun meshToPathProfile(mesh: Mesh): List<ColoredProfile> {
    mesh.updateHelperMeshes()
    val helperMeshes = mesh.helperMeshes
    if (helperMeshes != null) {
        return helperMeshes.withIndex()
            .filter { it.value != null }
            .mapNotNull { (index, helper) ->
                val builder = ProfileBuilder(mesh)
                mesh.forEachLineIndex(helper!!) { ai, bi ->
                    builder.addLine(ai, bi); false
                }
                val profile = builder.build()
                if (profile != null) {
                    val material = mesh.materials.getOrNull(index) ?: InvalidRef
                    ColoredProfile(profile, material)
                } else null
            }
    } else {
        // there is only a single material
        val builder = ProfileBuilder(mesh)
        mesh.forEachLineIndex { ai, bi ->
            builder.addLine(ai, bi); false
        }
        val profile = builder.build()
        return if (profile != null) {
            listOf(ColoredProfile(profile, mesh.material))
        } else emptyList()
    }
}

fun main() {

    OfficialExtensions.initForTests()

    val testProfileMesh = MeshCache[res.getChild("meshes/StreetProfile.glb")]!!
    val testProfileMesh1 = mergeMaterials(testProfileMesh)
    val testProfiles = meshToPathProfile(testProfileMesh1)[0]

    fun Entity.addSplineMesh(): Entity {
        val (testProfile, testMaterial) = testProfiles
        return add(SplineMesh().apply {
            profile = testProfile
            materials = listOf(testMaterial)
        })
    }

    fun Entity.addCrossing(): Entity {
        val (_, testMaterial) = testProfiles
        return add(SplineCrossing().apply {
            materials = listOf(testMaterial)
        })
    }

    fun Entity.addControl(): Entity {
        val (testProfile, _) = testProfiles
        return add(SplineControlPoint().apply {
            profile = testProfile
        })
    }

    // use multiple SplineMeshes on the same entity to build a street:
    //  - street, lamps, water outlets, stripes?, ...
    // -> see SplineSpawnerTest as an example on how to do that

    testUI("SplineMesh") {
        ECSRegistry.init()

        val world = Entity("World")
        val splineEntity = Entity("Spline", world)
            .addSplineMesh()

        fun add(parent: Entity, p: Vector3d) {
            Entity(parent)
                .setPosition(p)
                .addControl()
        }

        add(splineEntity, Vector3d(0.0, 3.0, 30.0))
        add(splineEntity, Vector3d(0.0, 0.0, 20.0))
        add(splineEntity, Vector3d(3.0, 0.0, 10.0))
        add(splineEntity, Vector3d(0.0, 0.0, 0.0))

        val endEntity = Entity("End Piece", world)
            .setPosition(0.0, 3.0, 30.0)
            .setRotation(0f, PIf, 0f)
        add(endEntity, Vector3d())
        endEntity.addCrossing()

        val crossEntity = Entity("Crossing", world)
            .setPosition(30.0, 0.0, 0.0)
            .addCrossing()

        val l = 7
        val r0 = 15.0
        for (i in 0 until l) {
            val angle = i * TAU / l
            val rx = cos(angle)
            val rz = sin(angle)
            val ry = -angle + PI * 0.5
            val cross = Entity("Cross $i", crossEntity)
                .setPosition(rx * r0, 0.0, rz * r0)
                .setRotation(0f, ry.toFloat(), 0f)
                .addControl()
            val street = Entity("Street $i", cross)
                .addSplineMesh()
            add(street, Vector3d(0.0, 0.0, 5.0))
            add(street, Vector3d())
        }

        testScene(world)
    }
}