package me.anno.ecs.components.mesh.spline

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.spline.SplineMesh.Companion.createEndPiece
import me.anno.ecs.components.mesh.spline.SplineMesh.Companion.generateSplineMesh
import me.anno.ecs.components.mesh.spline.SplineMesh.Companion.merge
import me.anno.fonts.mesh.Triangulation
import me.anno.utils.structures.tuples.get
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

class SplineCrossing : ProceduralMesh() {

    var autoSort = false
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
        val entity = entity
        if (entity == null) {
            lastWarning = "Missing entity"
            invalidateMesh()
            return
        }

        var streets = entity.children.mapNotNull { it.getComponent(SplineControlPoint::class) }

        when (streets.size) {
            0 -> {
                lastWarning = "Missing control points"
                invalidateMesh()
                return
            }
            1 -> {
                lastWarning = null
                createEndPiece(streets.first(), useRight, pointsPerRadiant, mesh)
            }
            2 -> {
                // just connect them normally
                lastWarning = null
                val profile = streets.first().profile
                SplineMesh.generateLineMesh(
                    streets[0], streets[1], mesh, profile,
                    profile.isClosed, closedStart = false, closedEnd = false,
                    isStrictlyUp = false
                )
            }
            else -> {

                // consists of the main area (middle segment), and then, given by the profile,
                // outer paths can be implemented using half profiles

                val profile = streets.first().profile
                val halfProfile = profile.split()[!useRight]
                val center = Vector3d()
                val yAxis = Vector3d()
                val tmp = Vector3d()
                val tmp2 = Vector3d()
                val tmp3 = Vector3f()
                for (street in streets) {
                    center.add(street.transform!!.localPosition)
                    yAxis.add(street.transform!!.localRotation.transform(tmp.set(0.0, 1.0, 0.0)))
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
                        atan2(zAxis.dot(tmp), xAxis.dot(tmp))
                    }
                }

                val meshes = ArrayList<Mesh>()
                val centerPoints = ArrayList<Vector3f>()

                fun createPoint(p: SplineControlPoint, f: Boolean): SplineControlPoint {
                    val clone = p.clone() as SplineControlPoint
                    val cloneEntity = Entity()
                    // rotate 180° if looking wrong direction (can be found using its spline, or atan2)
                    // rotate 180° * f
                    // copy transform
                    val correct = p.getLocalPosition(tmp, 0.0).dot(p.getLocalForward(tmp2)) > 0.0
                    val angle = (correct.toInt() + f.toInt() + useRight.toInt()) * PI
                    val pe = p.entity!!
                    cloneEntity.position = pe.position
                    cloneEntity.rotation = cloneEntity.rotation
                        .set(pe.rotation)
                        .rotateY(angle) // correct coordinate system?
                    cloneEntity.scale = cloneEntity.scale
                    cloneEntity.validateTransform()
                    cloneEntity.add(clone)
                    return clone
                }

                for (index in streets.indices) {
                    val s0 = streets[index]
                    val s1 = streets[(index + 1) % streets.size]
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
                            centerPoints.add(
                                Vector3f(
                                    (a.x + b.x).toFloat(),
                                    (a.y + b.y).toFloat(),
                                    (a.z + b.z).toFloat()
                                ).mul(0.5f)
                            )
                        }
                    }
                    meshes.add(
                        generateSplineMesh(
                            null, halfProfile,
                            isClosed = false, closedStart0 = false, closedEnd0 = false, isStrictlyUp = false,
                            splinePoints
                        )
                    )
                }

                if (coverTop || coverBottom) {

                    if (coverBottom && !coverTop) {
                        centerPoints.reverse()
                    }

                    val triangulation = Triangulation
                        .ringToTrianglesVec3f(centerPoints)

                    // find central color
                    // interpolation won't work -> use single color
                    var topY = 0f
                    var topColor = -1
                    var topScore = Float.NEGATIVE_INFINITY
                    var bottomY = 0f
                    var bottomColor = -1
                    var bottomScore = Float.NEGATIVE_INFINITY
                    for (i in profile.positions.indices) {
                        val pos = profile.getPosition(i)
                        val tScore = pos.y + abs(pos.x)
                        if (tScore > topScore) {
                            topScore = tScore
                            topColor = profile.getColor(i, true)
                            topY = pos.y
                        }
                        val bScore = -pos.y + abs(pos.x)
                        if (bScore > bottomScore) {
                            bottomScore = bScore
                            bottomColor = profile.getColor(i, true)
                            bottomY = pos.y
                        }
                    }

                    if (coverTop) {
                        // raise all points up
                        tmp3.set(yAxis).mul(topY)
                        for (p in centerPoints) p.add(tmp3)
                        meshes.add(triToMesh(triangulation, yAxis, topColor))
                        if (coverBottom) {
                            tmp3.set(yAxis).mul(bottomY - topY)
                            for (p in centerPoints) p.add(tmp3)
                            yAxis.mul(-1.0)
                            meshes.add(triToMesh(triangulation.reversed(), yAxis, bottomColor))
                        }
                    } else {
                        tmp3.set(yAxis).mul(bottomY)
                        for (p in centerPoints) p.add(tmp3)
                        yAxis.mul(-1.0)
                        meshes.add(triToMesh(triangulation, yAxis, bottomColor))
                    }

                }

                lastWarning = null
                merge(meshes, mesh)
            }
        }
    }

    private fun triToMesh(tri: List<Vector3f>, up: Vector3d, color: Int): Mesh {
        val mesh = Mesh()
        val nx = up.x.toFloat()
        val ny = up.y.toFloat()
        val nz = up.z.toFloat()
        val pos = FloatArray(tri.size * 3)
        var k = 0
        for (i in tri.indices) {
            val p = tri[i]
            pos[k++] = p.x
            pos[k++] = p.y
            pos[k++] = p.z
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

    override val className: String get() = "SplineCrossing"

}