package me.anno.ecs.components.mesh.spline

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.spline.SplineMesh.Companion.dirX
import me.anno.ecs.components.mesh.spline.SplineMesh.Companion.merge
import me.anno.fonts.mesh.Triangulation
import me.anno.maths.Maths.PIf
import me.anno.utils.structures.tuples.get
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3fc
import kotlin.math.*

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

    var useRightForEnd = false
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

                // generate end piece: rotational
                val point = streets.first()
                val profile = point.profile
                val halfProfile = profile.split()[!useRightForEnd]
                val numAngles = 1 + max(1, pointsPerRadiant.roundToInt())
                val profileSize = halfProfile.positions.size
                val numQuads = (profileSize - 1) * (numAngles - 1)
                val numPoints = numQuads * 6
                val pos = mesh.positions.resize(numPoints * 3)
                val nor = mesh.normals.resize(numPoints * 3)
                val col = mesh.color0.resize(numPoints)
                val dirY = Vector3f().set(point.getLocalUp(Vector3d()))
                val angleOffset = useRightForEnd.toInt() * PI

                // generate all values
                val n0 = Vector2f()
                val n1 = Vector2f()

                val p0a = Vector3d()
                val p0b = Vector3d()
                val p1a = Vector3d()
                val p1b = Vector3d()

                val dirX0 = Vector3f()
                val dirX1 = Vector3f()

                // calculate points p0a,p0b
                val cos0 = cos(angleOffset)
                point.getLocalPosition(p0a, -cos0)
                point.getLocalPosition(p0b, +cos0)

                dirX(p0a, p0b, dirX0)

                var k = 0
                for (ai in 1 until numAngles) {
                    val angle = ai * PI / (numAngles - 1) + angleOffset
                    val cos1 = cos(angle)
                    val sin1 = sin(angle)
                    var p0 = halfProfile.positions[0]

                    // calculate points p1a,p1b
                    point.getLocalPosition(p1a, -cos1, -sin1)
                    point.getLocalPosition(p1b, +cos1, +sin1) // flip sign as well? yes, 180° rotated

                    dirX(p1a, p1b, dirX1)

                    for (si in 1 until profileSize) {

                        val p1 = halfProfile.positions[si]
                        val c0 = halfProfile.getColor(si - 1, true)
                        val c1 = halfProfile.getColor(si, false)

                        halfProfile.getNormal(si - 1, false, n0)
                        halfProfile.getNormal(si - 1, true, n1)

                        SplineMesh.add(pos, nor, col, k++, p0a, p0b, p0, n0, c0, dirX0, dirY)
                        SplineMesh.add(pos, nor, col, k++, p1a, p1b, p1, n1, c1, dirX1, dirY)
                        SplineMesh.add(pos, nor, col, k++, p1a, p1b, p0, n0, c0, dirX1, dirY)

                        SplineMesh.add(pos, nor, col, k++, p0a, p0b, p1, n1, c1, dirX0, dirY)
                        SplineMesh.add(pos, nor, col, k++, p1a, p1b, p1, n1, c1, dirX1, dirY)
                        SplineMesh.add(pos, nor, col, k++, p0a, p0b, p0, n0, c0, dirX0, dirY)

                        p0 = p1

                    }

                    p0a.set(p1a)
                    p0b.set(p1b)
                    dirX0.set(dirX1)

                }
                mesh.positions = pos
                mesh.normals = nor
                mesh.color0 = col
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
                val profile = streets.first().profile
                val (leftProfile, rightProfile) = profile.split()
                val center = Vector3d()
                val up = Vector3d()
                val tmp = Vector3d()
                for (street in streets) {
                    center.add(street.transform!!.localPosition)
                    up.add(street.transform!!.localRotation.transform(tmp.set(0.0, 1.0, 0.0)))
                }
                center.div(streets.size.toDouble())
                up.normalize()
                // todo find x and z axis

                // auto sort by angle?
                if (autoSort) {
                    streets = streets.sortedBy {
                        it.getLocalPosition(tmp, -1.0)
                        atan2(tmp.z, tmp.x) // todo use local coordinates
                    }
                }
                val meshes = ArrayList<Mesh>()
                val centerPoints = ArrayList<Vector3f>()
                for (index in streets.indices) {
                    val s0 = streets[index]
                    // todo add end path
                    val s1 = streets[(index + 1) % streets.size]
                    // todo connect both smoothly
                    // todo add start path
                }
                val triangulation = Triangulation
                    .ringToTrianglesVec3f(centerPoints)

                // find central color
                // interpolation won't work -> use single color
                var centralColor = -1
                var bestScore = Float.NEGATIVE_INFINITY
                for (i in profile.positions.indices) {
                    val pos = profile.getPosition(i)
                    val score = pos.y + abs(pos.x)
                    if (score > bestScore) {
                        bestScore = score
                        centralColor = profile.getColor(i, true)
                    }
                }
                meshes.add(triToMesh(triangulation, up, centralColor))

                // todo consists of the main area (middle segment), and then, given by the profile,
                // todo outer paths from edge to edge; defined by profile
                // todo outer paths can be implemented using half profiles

                lastWarning = "Hasn't been properly implemented yet"
                merge(meshes, mesh)
            }
        }
    }

    private fun triToMesh(tri: List<Vector3fc>, up: Vector3d, color: Int): Mesh {
        val mesh = Mesh()
        val nx = up.x.toFloat()
        val ny = up.y.toFloat()
        val nz = up.z.toFloat()
        val positions = FloatArray(tri.size * 3)
        var k = 0
        for (i in tri.indices) {
            val p = tri[i]
            positions[k++] = p.x()
            positions[k++] = p.y()
            positions[k++] = p.z()
        }
        val normals = FloatArray(tri.size * 3)
        k = 0
        for (i in tri.indices) {
            normals[k++] = nx
            normals[k++] = ny
            normals[k++] = nz
        }
        val colors = IntArray(tri.size)
        colors.fill(color)
        mesh.positions = positions
        mesh.normals = normals
        mesh.color0 = colors
        return mesh
    }

    override fun clone(): ProceduralMesh {
        val clone = SplineCrossing()
        copy(clone)
        return clone
    }

    override val className = "SplineCrossing"

}