package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.HelperMesh.Companion.updateHelperMeshes
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshIterators.forEachLineIndex
import me.anno.ecs.components.mesh.spline.PathProfile
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineCrossing
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.spline.Splines
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.image.ImageWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.Color.white
import me.anno.utils.OS.res
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

fun isOnLine(p: Vector3f): Boolean {
    return abs(p.z) < 0.03f
}

// todo function to extract colors from multiple materials onto single material and vertex colors

class ProfileBuilder(mesh: Mesh) {

    private val pos: FloatArray = mesh.positions!!
    private val uvs: FloatArray? = mesh.uvs
    private val colors: IntArray? = mesh.color0

    private val connectivity = HashMap<Vector3f, ArrayList<Vector3f>>()
    private val uvMap = HashMap<Vector3f, Float>()
    private val colorMap = HashMap<Vector3f, Int>()

    private fun addPoint(index: Int): Vector3f? {
        val point = Vector3f(pos, index * 3)
        return if (isOnLine(point)) {
            if (uvs != null && index * 2 + 1 < uvs.size) {
                uvMap[point] = uvs[index * 2]
            }
            if (colors != null && index < colors.size) {
                colorMap[point] = colors[index]
            }
            point
        } else null
    }

    private fun addLink(a: Vector3f, b: Vector3f) {
        connectivity.getOrPut(a, ::ArrayList).add(b)
    }

    fun addLine(ai: Int, bi: Int) {
        val a = addPoint(ai)
        val b = addPoint(bi)
        if (a != null && b != null) {
            addLink(a, b)
            addLink(b, a)
        }
    }

    fun build(): PathProfile? {
        val starts = connectivity.filter { it.value.size == 1 }
        val start = starts.keys.minByOrNull { it.x } ?: return null
        val pointList = ArrayList<Vector3f>()
        pointList.add(start)
        while (true) {
            val point = pointList.last()
            val predecessor = pointList.getOrNull(pointList.size - 2)
            val nextPoints = connectivity[point]!!
            val nextPoint = nextPoints.firstOrNull2 { it != predecessor } ?: break
            pointList.add(nextPoint)
        }
        val dst = PathProfile()
        dst.positions = pointList.map { Vector2f(it.x, it.y) }
        if (uvMap.isNotEmpty()) {
            val uvs1 = FloatArrayList(pointList.size)
            for (i in pointList.indices) {
                uvs1.add(uvMap[pointList[i]] ?: 0f)
            }
            dst.uvs = uvs1
        }
        if (colorMap.isNotEmpty()) {
            val col1 = IntArrayList(pointList.size)
            for (i in pointList.indices) {
                col1.add(colorMap[pointList[i]] ?: white)
            }
            dst.colors = col1
        }
        return dst
    }
}

/**
 * extract all triangles with this material into a line...:
 *    only keep points with z ~ 0, extract x,y
 *    only track lines, where all points are on the line
 * */
fun meshToPathProfile(mesh: Mesh): List<Pair<PathProfile, FileReference>> {
    mesh.updateHelperMeshes()
    val helperMeshes = mesh.helperMeshes
    if (helperMeshes != null) {
        return helperMeshes.withIndex()
            .filter { it.value != null }
            .mapNotNull { (index, helper) ->
                val builder = ProfileBuilder(mesh)
                mesh.forEachLineIndex(helper!!) { ai, bi ->
                    builder.addLine(ai, bi)
                }
                val profile = builder.build()
                if (profile != null) {
                    val material = mesh.materials.getOrNull(index) ?: InvalidRef
                    profile to material
                } else null
            }
    } else {
        // there is only a single material
        val builder = ProfileBuilder(mesh)
        mesh.forEachLineIndex { ai, bi ->
            builder.addLine(ai, bi)
        }
        val profile = builder.build()
        return if (profile != null) {
            listOf(profile to mesh.material)
        } else emptyList()
    }
}

fun main() {

    OfficialExtensions.initForTests()

    // todo test the looks of this profile
    val testProfileMesh = MeshCache[res.getChild("meshes/StreetProfile.glb")]!!
    val testProfiles = meshToPathProfile(testProfileMesh)

    // todo for extruding, extrude UVs
    // todo for copying, allow custom spacing
    // todo use multiple SplineMeshes on the same entity to build a street:
    //  - street, lamps, water outlets, stripes?, ...

    // test interpolation with 1 and 2 intermediate points
    // interpolation with 1 point: just a line, and therefore useless

    testUI("SplineMesh") {
        ECSRegistry.init()

        val world = Entity("World")
        val splineEntity = Entity("Spline")
        splineEntity.add(SplineMesh())
        fun add(e: Entity, p: Vector3f, r: Quaternionf = Quaternionf()) {
            val child = Entity(e)
            child.position = child.position.set(p)
            child.rotation = child.rotation.set(r)
            child.add(SplineControlPoint())
        }
        add(splineEntity, Vector3f())
        add(splineEntity, Vector3f(0f, 0f, 10f))
        add(splineEntity, Vector3f(3f, 0f, 10f))
        add(splineEntity, Vector3f(0f, 0f, 30f))
        world.add(splineEntity)

        val endEntity = Entity("End Piece")
        endEntity.setPosition(0.0, 3.0, 0.0)
        endEntity.add(SplineCrossing())
        add(endEntity, Vector3f())
        world.add(endEntity)

        val crossEntity = Entity("Crossing")
        crossEntity.setPosition(0.0, 6.0, 0.0)
        crossEntity.add(SplineCrossing())
        val l = 15
        for (i in 0 until l) {
            val angle = i * PI * 2.0 / l
            val child = Entity()
            child.setPosition(cos(angle) * 20f, 0.0, sin(angle) * 20f)
            child.setRotation(0.0, -angle + PI * 0.5, 0.0)
            child.add(SplineControlPoint())
            // todo add streets as control
            crossEntity.add(child)
        }
        world.add(crossEntity)

        testScene(world)
    }

    val size = 512

    val p0 = Vector3d()
    val p1 = Vector3d(1.0)
    val n0 = Vector3d(0.0, +1.0, 0.0)
    val n1 = Vector3d(0.0, +1.0, 0.0)

    for (d in listOf(p0, p1)) {
        d.mul(0.8)
        d.add(0.1, 0.1, 0.1)
        d.mul(size.toDouble())
    }

    val imm0 = Vector3d()
    val imm1 = Vector3d()
    Splines.getIntermediatePointsForSpline(p0, n0, p1, n1, imm0, imm1)

    val points = ArrayList<Vector2f>()

    val dst = Vector3d()
    val steps = size / 3 + 1
    for (i in 0 until steps) {
        Splines.interpolate(p0, imm0, imm1, p1, i / (steps - 1.0), dst)
        points.add(Vector2f(dst.x.toFloat(), dst.y.toFloat()))
    }

    ImageWriter.writeImageCurve(size, size, false, 255 shl 24, -1, 5, points, "spline1.png")
}