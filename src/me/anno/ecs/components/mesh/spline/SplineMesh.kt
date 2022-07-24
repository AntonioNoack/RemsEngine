package me.anno.ecs.components.mesh.spline

import me.anno.Build
import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * spline meshes are parts of many simulator games, e.g. street building
 * */
class SplineMesh : ProceduralMesh() {

    var profile: PathProfile? = TestProfiles.cubeProfile
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }


    // todo respect height

    // using N points, define a spline
    // done N = 2 = a line
    // done N > 2 = an actual spline
    // todo N = 1 = a pillar, maybe

    // children as control points

    // kind of done when changed, update the mesh automatically
    // only if debugging the mesh

    // todo update based on distance

    // todo load profile from mesh file?

    var isClosed = false
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    var piecewiseLinear = false
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    var isStrictlyUp = false
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

    var closedStart = true
        set(value) {
            if (field != value) {
                field = value
                if (!isClosed) invalidateMesh()
            }
        }

    var closedEnd = true
        set(value) {
            if (field != value) {
                field = value
                if (!isClosed) invalidateMesh()
            }
        }


    override fun clone(): SplineMesh {
        val clone = SplineMesh()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SplineMesh
        clone.isStrictlyUp = isStrictlyUp
        clone.pointsPerRadiant = pointsPerRadiant
        clone.piecewiseLinear = piecewiseLinear
        clone.isClosed = isClosed
        clone.profile = profile
    }

    override fun onUpdate(): Int {
        super.onUpdate()
        // if a child is selected, invalidate this
        if (Build.isDebug) {
            val children = entity?.children ?: return 16
            val lastSelection = EditorState.lastSelection
            for (i in children.indices) {
                val child = children[i]
                if (child.hasComponent(SplineControlPoint::class) && child === lastSelection) {
                    invalidateMesh()
                    return 1
                }
            }
        }
        return 32
    }

    fun set(t: SplineTmpMesh) {
        data.positions = t.positions
        data.normals = t.normals
        data.color0 = t.colors
    }

    fun replace(v0: IntArray?, size: Int) = if (v0 != null && v0.size == size) v0 else IntArray(size)
    fun replace(v0: FloatArray?, size: Int) = if (v0 != null && v0.size == size) v0 else FloatArray(size)

    fun set(ts: List<SplineTmpMesh>) {
        val colSize = ts.sumOf { it.colors.size }
        val posSize = colSize * 3
        val col = replace(data.color0, colSize)
        val pos = replace(data.positions, posSize)
        val nor = replace(data.normals, posSize)
        var i = 0
        var j = 0
        for (t in ts) {
            val pi = t.positions
            val ni = t.normals
            val ci = t.colors
            System.arraycopy(ci, 0, col, j, ci.size)
            System.arraycopy(pi, 0, pos, i, pi.size)
            System.arraycopy(ni, 0, nor, i, ni.size)
            i += pi.size
            j += ci.size
        }
        data.positions = pos
        data.normals = nor
        data.color0 = col
    }

    override fun generateMesh(mesh: Mesh) {
        val entity = entity
        if (entity == null) {
            lastWarning = "Missing entity, $parent, ${Engine.gameTime}"
            invalidateMesh()
            return
        }
        val points = entity.children.mapNotNull { it.getComponent(SplineControlPoint::class) }
        when (points.size) {
            0 -> {
                lastWarning = "SplineMesh has no points"
                invalidateMesh()
            }
            1 -> {
                lastWarning = "SplineMesh has not enough points, only one"
                invalidateMesh()
            }
            2 -> set(generateLineMesh(points[0], points[1], mesh))
            else -> {
                lastWarning = null
                if (piecewiseLinear) {
                    val list = ArrayList<SplineTmpMesh>()
                    for (i in 1 until points.size) {
                        list.add(generateLineMesh(points[i - 1], points[i], null))
                    }
                    set(list)
                } else {
                    set(generateSplineMesh(points, pointsPerRadiant, mesh))
                }
            }
        }
    }

    private fun generateLineMesh(p0: SplineControlPoint, p1: SplineControlPoint, mesh: Mesh?) =
        generateSplineMesh(listOf(p0, p1), 0.0, mesh)

    private fun generateSplineMesh(
        points: List<SplineControlPoint>,
        perRadiant: Double,
        mesh: Mesh?
    ): SplineTmpMesh {

        val isClosed = isClosed
        val posNormals = Array((points.size + isClosed.toInt()) * 4) { Vector3d() }
        for (i in 0 until (points.size + isClosed.toInt())) {
            val i4 = i * 4
            val pt = points[i % points.size]
            pt.getLocalPosition(posNormals[i4], -1.0)
            pt.getLocalForward(posNormals[i4 + 1])
            pt.getLocalPosition(posNormals[i4 + 2], +1.0)
            pt.getLocalForward(posNormals[i4 + 3])
        }

        val profile = profile!!
        val profileSize = profile.getSize2()

        val profileFacade = if (!isClosed && (closedStart || closedEnd)) profile.getFacade() else null
        val closedStart = profileFacade != null && closedStart
        val closedEnd = profileFacade != null && closedEnd

        // todo also list profiles, and height
        val splinePoints = Splines.generateSplineLinePair(posNormals, perRadiant, isClosed)

        // generate the mesh
        val splineSize = splinePoints.size / 2 - 1
        val numPoints = 6 * profileSize * splineSize +
                (profileFacade?.size ?: 0) * (closedStart.toInt() + closedEnd.toInt())
        val numCoords = 3 * numPoints
        val pos = mesh?.positions.resize(numCoords)
        val nor = mesh?.normals.resize(numCoords)
        val col = mesh?.color0.resize(numPoints)
        var k = 0
        val n0 = Vector2f()
        val n1 = Vector2f()
        val dirY0 = Vector3f()
        val dirY1 = Vector3f()

        fun addProfile(p0a: Vector3d, p0b: Vector3d, nx: Float) {
            // add profile
            val ptToColor = HashMap<Vector2f, Int>(profile.positions.size)
            for (i in profile.positions.indices) {
                ptToColor[profile.getPosition(i)] = profile.getColor(i)
            }
            // find normal: (p0b-p0a) x dirY
            val normal = Vector3f(dirY0)
                .cross(
                    (p0a.x - p0b.x).toFloat(),
                    (p0a.y - p0b.y).toFloat(),
                    (p0a.z - p0b.z).toFloat()
                ).normalize(nx)
            profileFacade!!
            if (nx < 0f) {
                for (i in profileFacade.indices.reversed()) {
                    val pro0 = profileFacade[i]
                    val c = ptToColor[pro0] ?: -1
                    add2(pos, nor, col, k++, p0a, p0b, pro0, n0, c, dirY0, normal)
                }
            } else {
                for (i in profileFacade.indices) {
                    val pro0 = profileFacade[i]
                    val c = ptToColor[pro0] ?: -1
                    add2(pos, nor, col, k++, p0a, p0b, pro0, n0, c, dirY0, normal)
                }
            }
        }

        if (closedStart) {
            val p0a = splinePoints[0]
            val p0b = splinePoints[1]
            findDirY(p0a, p0b, splinePoints[3], dirY0)
            addProfile(p0a, p0b, 1f)
        }

        if (closedEnd) {
            val p0a = splinePoints[splinePoints.size - 2]
            val p0b = splinePoints[splinePoints.size - 1]
            findDirY(
                splinePoints[splinePoints.size - 4],
                splinePoints[splinePoints.size - 3],
                splinePoints[splinePoints.size - 2],
                dirY0
            )
            addProfile(p0a, p0b, -1f)
        }

        for (j in 0 until profileSize) {

            val pro0 = profile.getPosition(j)
            val pro1 = profile.getPosition(j + 1)

            profile.getNormal(j, false, n0)
            profile.getNormal(j, true, n1)

            val c = profile.getColor(j)

            var p0a = splinePoints[0]
            var p0b = splinePoints[1]

            if (splinePoints.size > 3) {
                findDirY(p0a, p0b, splinePoints[3], dirY0)
            } else dirY0.set(0f, 1f, 0f)
            dirY1.set(dirY0) // if spline size is 2

            for (i in 0 until splineSize) {

                val i2 = i * 2
                val p1a = splinePoints[i2 + 2]
                val p1b = splinePoints[i2 + 3]

                if (i2 + 5 < splinePoints.size) {
                    findDirY(p1a, p1b, splinePoints[i2 + 5], dirY1)
                }

                // 012 230
                add(pos, nor, col, k++, p0a, p0b, pro0, n0, c, dirY0)
                add(pos, nor, col, k++, p1a, p1b, pro1, n1, c, dirY1)
                add(pos, nor, col, k++, p1a, p1b, pro0, n0, c, dirY1)

                add(pos, nor, col, k++, p0a, p0b, pro1, n1, c, dirY0)
                add(pos, nor, col, k++, p1a, p1b, pro1, n1, c, dirY1)
                add(pos, nor, col, k++, p0a, p0b, pro0, n0, c, dirY0)

                p0a = p1a
                p0b = p1b

                dirY0.set(dirY1)

            }

        }
        return SplineTmpMesh(pos, nor, col)
    }

    private fun findDirY(p1a: Vector3d, p1b: Vector3d, p2b: Vector3d, dst: Vector3f = Vector3f()) {
        if (isStrictlyUp) {
            dst.set(0f, 1f, 0f)
        } else {
            val ax = p1b.x - p1a.x // dx
            val ay = p1b.y - p1a.y
            val az = p1b.z - p1a.z
            val bx = p2b.x - p1b.x // dz
            val by = p2b.y - p1b.y
            val bz = p2b.z - p1b.z
            dst.set(// 23 31 12 -> dy
                ay * bz - az * by,
                az * bx - ax * bz,
                ax * by - ay * bx
            ).normalize()
        }
    }

    private fun add(
        positions: FloatArray, normals: FloatArray, colors: IntArray,
        k: Int, p0: Vector3d, p1: Vector3d, profile: Vector2f, n: Vector2f, c: Int,
        dirY: Vector3f
    ) {
        val k3 = k * 3
        val px = profile.x.toDouble()
        val py = profile.y
        val dirX = JomlPools.vec3f.borrow()
        dirX.set(
            JomlPools.vec3d.borrow()
                .set(p1).sub(p0).normalize()
        )
        positions[k3 + 0] = (mix(p0.x, p1.x, px) + py * dirY.x).toFloat()
        positions[k3 + 1] = (mix(p0.y, p1.y, px) + py * dirY.y).toFloat()
        positions[k3 + 2] = (mix(p0.z, p1.z, px) + py * dirY.z).toFloat()
        normals[k3 + 0] = n.x * dirX.x + n.y * dirY.x
        normals[k3 + 1] = n.x * dirX.y + n.y * dirY.y
        normals[k3 + 2] = n.x * dirX.z + n.y * dirY.z
        colors[k] = c
    }

    private fun add2(
        positions: FloatArray, normals: FloatArray, colors: IntArray,
        k: Int, p0: Vector3d, p1: Vector3d, profile: Vector2f, n: Vector2f, c: Int,
        dirY: Vector3f, normal: Vector3f
    ) {
        val k3 = k * 3
        val px = profile.x.toDouble()
        val py = profile.y
        val dirX = JomlPools.vec3f.borrow()
        dirX.set(
            JomlPools.vec3d.borrow()
                .set(p1).sub(p0).normalize()
        )
        positions[k3 + 0] = (mix(p0.x, p1.x, px) + py * dirY.x).toFloat()
        positions[k3 + 1] = (mix(p0.y, p1.y, px) + py * dirY.y).toFloat()
        positions[k3 + 2] = (mix(p0.z, p1.z, px) + py * dirY.z).toFloat()
        normals[k3 + 0] = normal.x
        normals[k3 + 1] = normal.y
        normals[k3 + 2] = normal.z
        colors[k] = c
    }

    override val className: String = "SplineMesh"

}