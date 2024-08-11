package me.anno.ecs.components.mesh.spline

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.spline.SplineMesh.Companion.createEndPiece
import me.anno.ecs.components.mesh.spline.SplineMesh.Companion.generateSplineMesh
import me.anno.ecs.components.mesh.spline.SplineMesh.Companion.merge
import me.anno.mesh.Triangulation
import me.anno.utils.Color.white
import me.anno.utils.structures.tuples.get
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.atan2

class SplineCrossing : ProceduralMesh() {

    var autoSort = true
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    var pointsPerRadiant = 10.0
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    var useRight = false
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    var coverTop = true
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    var coverBottom = false
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    override fun generateMesh(mesh: Mesh) {
        val children = (entity?.children ?: emptyList())
        generateMesh(mesh, children.mapNotNull { it.getComponent(SplineControlPoint::class) })
    }

    fun generateMesh(mesh: Mesh, streets: List<SplineControlPoint>) {
        when (streets.size) {
            0 -> {
                lastWarning = "Missing control points"
                mesh.positions = null
            }
            1 -> {
                lastWarning = null
                createEndPiece(streets.first(), useRight, pointsPerRadiant, mesh)
            }
            2 -> generateMesh2(mesh, streets)
            else -> generateMeshN(mesh, streets)
        }
    }

    private fun generateMesh2(mesh: Mesh, streets0: List<SplineControlPoint>) {
        // just connect them normally
        lastWarning = null
        val profile = streets0.first().profile
        val p0 = streets0[0]
        val p1 = streets0[1]
        generateSplineMesh(
            mesh, profile, false, false, false, false,
            listOf(
                p0.getLocalPosition(Vector3d(), -1.0),
                p0.getLocalPosition(Vector3d(), +1.0),
                p1.getLocalPosition(Vector3d(), +1.0),
                p1.getLocalPosition(Vector3d(), -1.0),
            )
        )
    }

    private fun generateMeshN(mesh: Mesh, streets0: List<SplineControlPoint>) {
        var streets = streets0

        // consists of the main area (middle segment), and then, given by the profile,
        // outer paths can be implemented using half profiles

        val center = Vector3d()
        val yAxis = Vector3d()
        val tmp = Vector3d()
        val tmp2 = Vector3d()
        val tmp3 = Vector3f()
        for (street in streets) {
            val transform = street.transform ?: continue
            center.add(transform.localPosition)
            yAxis.add(transform.localRotation.transform(tmp.set(0.0, 1.0, 0.0)))
        }
        center.div(streets.size.toDouble())
        yAxis.normalize()

        val xAxis = Vector3d()
        val zAxis = Vector3d()
        yAxis.findSystem(xAxis, zAxis)

        // auto sort by angle?
        if (autoSort) {
            streets = streets.sortedBy {
                it.getLocalPosition(tmp, 0.0)
                atan2(xAxis.dot(tmp), zAxis.dot(tmp))
            }
        }

        val meshes = ArrayList<Mesh>()
        val centerPoints = ArrayList<Vector3d>()

        fun createPoint(p: SplineControlPoint, f: Boolean): SplineControlPoint {
            val clone = p.clone() as SplineControlPoint
            val cloneEntity = Entity()
            // rotate 180° if looking wrong direction (can be found using its spline, or atan2)
            // rotate 180° * f
            // copy transform
            val correct = p.getLocalPosition(tmp, 0.0).dot(p.getLocalForward(tmp2)) > 0.0
            val angle = (correct.toInt() + f.toInt() + useRight.toInt()) * PI
            val pe = p.entity!!
            cloneEntity.transform.localPosition = pe.transform.localPosition
            cloneEntity.rotation = cloneEntity.rotation
                .set(pe.rotation)
                .rotateY(angle) // correct coordinate system?
            cloneEntity.transform.localScale = cloneEntity.transform.localScale
            cloneEntity.validateTransform()
            cloneEntity.add(clone)
            return clone
        }

        for (index in streets.indices) {
            val s0 = streets[index]
            val s1 = if (index == streets.lastIndex) streets[0] else streets[index + 1]
            val points = listOf(
                createPoint(s1, false),
                createPoint(s0, true),
            )
            val splinePoints = SplineMesh.generateSplinePoints(points, pointsPerRadiant, isClosed = false)
            if (coverTop || coverBottom) {
                centerPoints.ensureCapacity(centerPoints.size + splinePoints.size / 2)
                for (i in splinePoints.size / 2 - 1 downTo 0) {
                    val j = i * 2
                    val a = splinePoints[j]
                    val b = splinePoints[j + 1]
                    centerPoints.add(a.add(b, Vector3d()).mul(0.5))
                }
            }
            val halfProfile = s0.profile
                .split().get(isFirst = !useRight)
            val meshI = generateSplineMesh(
                Mesh(), halfProfile,
                isClosed = false, closedStart0 = false, closedEnd0 = false,
                isStrictlyUp = true, splinePoints
            )
            // todo why do we need to flip the triangles here???
            //  and the normals look wrong, too
            val numPositions = meshI.positions!!.size / 3
            meshI.indices = IntArray(numPositions) { numPositions - 1 - it }
            meshes.add(meshI)
        }

        if (coverTop || coverBottom) {

            if (coverBottom && !coverTop) {
                centerPoints.reverse()
            }

            val triangulation = Triangulation
                .ringToTrianglesVec3d(centerPoints)

            // find central color
            // interpolation won't work -> use single color
            val profile = streets.first().profile
            val profileHalf = profile.split().get(isFirst = !useRight)
            val i1 = profileHalf.positions.lastIndex
            // todo test if these are correct for both left and right
            val topY = profileHalf.positions[i1].y
            val topColor = profile.colors?.getOrNull(i1) ?: white
            val bottomY = profileHalf.positions[0].y
            val bottomColor = profileHalf.colors?.getOrNull(0) ?: white

            if (coverTop) {
                // raise all points up
                tmp3.set(yAxis).mul(topY)
                for (p in centerPoints) p.add(tmp3)
                meshes.add(triangleListToMesh(triangulation, yAxis, topColor))
                if (coverBottom) {
                    tmp3.set(yAxis).mul(bottomY - topY)
                    for (p in centerPoints) p.add(tmp3)
                    yAxis.mul(-1.0)
                    meshes.add(triangleListToMesh(triangulation.reversed(), yAxis, bottomColor))
                }
            } else {
                tmp3.set(yAxis).mul(bottomY)
                for (p in centerPoints) p.add(tmp3)
                yAxis.mul(-1.0)
                meshes.add(triangleListToMesh(triangulation, yAxis, bottomColor))
            }
        }

        lastWarning = null
        merge(meshes, mesh)
    }

    private fun triangleListToMesh(tri: List<Vector3d>, up: Vector3d, color: Int): Mesh {
        val mesh = Mesh()
        val nx = up.x.toFloat()
        val ny = up.y.toFloat()
        val nz = up.z.toFloat()
        val pos = FloatArray(tri.size * 3)
        var k = 0
        for (i in tri.indices) {
            val p = tri[i]
            pos[k++] = p.x.toFloat()
            pos[k++] = p.y.toFloat()
            pos[k++] = p.z.toFloat()
        }
        val nor = FloatArray(tri.size * 3)
        k = 0
        for (i in tri.indices) {
            nor[k++] = nx
            nor[k++] = ny
            nor[k++] = nz
        }
        val colors = IntArray(tri.size)
        colors.fill(color)
        mesh.positions = pos
        mesh.normals = nor
        mesh.color0 = colors
        return mesh
    }
}