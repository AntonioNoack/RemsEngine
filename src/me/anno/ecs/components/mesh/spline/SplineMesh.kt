package me.anno.ecs.components.mesh.spline

import me.anno.Build
import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.spline.Splines.getIntermediates
import me.anno.ecs.components.mesh.spline.Splines.interpolate
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.image.ImageWriter.writeImageCurve
import me.anno.maths.Maths.mix
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.toVector3f
import org.apache.logging.log4j.LogManager
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


    // todo respect width and height

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
        mesh2.positions = t.positions
        mesh2.normals = t.normals
        mesh2.color0 = t.colors
    }

    fun replace(v0: IntArray?, size: Int) = if (v0 != null && v0.size == size) v0 else IntArray(size)
    fun replace(v0: FloatArray?, size: Int) = if (v0 != null && v0.size == size) v0 else FloatArray(size)

    fun set(ts: List<SplineTmpMesh>) {
        val colSize = ts.sumOf { it.colors.size }
        val posSize = colSize * 3
        val col = replace(mesh2.color0, colSize)
        val pos = replace(mesh2.positions, posSize)
        val nor = replace(mesh2.normals, posSize)
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
        mesh2.positions = pos
        mesh2.normals = nor
        mesh2.color0 = col
    }

    override fun generateMesh(mesh: Mesh) {
        println("generating spline mesh")
        val entity = entity
        if (entity == null) {
            lastWarning = "Missing entity, $parent, ${Engine.gameTime}"
            return
        }
        val points = entity.children.mapNotNull { it.getComponent(SplineControlPoint::class) }
        LOGGER.debug("Found ${points.size} children")
        when (points.size) {
            0 -> lastWarning = "SplineMesh has no points"
            1 -> lastWarning = "SplineMesh has not enough points, only one"
            2 -> set(generateLineMesh(points[0], points[1]))
            else -> {
                lastWarning = null
                if (piecewiseLinear) {
                    val list = ArrayList<SplineTmpMesh>()
                    for (i in 1 until points.size) {
                        list.add(generateLineMesh(points[i - 1], points[i]))
                    }
                    set(list)
                } else {
                    set(generateSplineMesh(points, pointsPerRadiant))
                }
            }
        }
    }

    private fun generateLineMesh(p0: SplineControlPoint, p1: SplineControlPoint): SplineTmpMesh {
        return generateSplineMesh(listOf(p0, p1), 0.0)
    }

    private fun generateSplineMesh(
        points: List<SplineControlPoint>,
        perRadiant: Double
    ): SplineTmpMesh {

        val posNormals = Array(points.size * 4) { Vector3d() }
        for (i in points.indices) {
            val i4 = i * 4
            val pt = points[i]
            pt.getP0(posNormals[i4])
            pt.getN0(posNormals[i4 + 1])
            pt.getP1(posNormals[i4 + 2])
            pt.getN1(posNormals[i4 + 3])
        }

        val profile = profile!!
        val profileSize = profile.getSize2()

        // todo also list profiles, and height
        val splinePoints = Splines.generateSplineLinePair(posNormals, perRadiant)

        // generate the mesh

        var splineSize = splinePoints.size / 2
        if (!isClosed) splineSize--
        val numPoints = 6 * profileSize * splineSize
        val pos = FloatArray(3 * numPoints)
        val nor = FloatArray(3 * numPoints)
        val col = IntArray(numPoints)
        var k = 0
        val n0 = Vector2f()
        val n1 = Vector2f()
        val dirY0 = Vector3f()
        val dirY1 = Vector3f()
        for (j in 0 until profileSize) {
            for (i in 0 until splineSize) {

                val i2 = i * 2

                val p0 = splinePoints[i2]
                val p1 = splinePoints[i2 + 1]
                val p2 = splinePoints[(i2 + 2) % splinePoints.size]
                val p3 = splinePoints[(i2 + 3) % splinePoints.size]

                findDirY(p0, p1, p3, dirY0)

                if (!isClosed && i2 + 5 > splinePoints.size) {
                    dirY1.set(dirY0)
                } else {
                    val p5 = splinePoints[(i2 + 5) % splinePoints.size]
                    findDirY(p2, p3, p5, dirY1)
                }

                val pro0 = profile.getPosition(j)
                val pro1 = profile.getPosition(j + 1)

                profile.getNormal(j, false, n0)
                profile.getNormal(j, true, n1)

                val c = profile.getColor(j)

                // 012 230
                add(pos, nor, col, k++, p0, p1, pro0, n0, c, dirY0)
                add(pos, nor, col, k++, p2, p3, pro1, n1, c, dirY1)
                add(pos, nor, col, k++, p2, p3, pro0, n0, c, dirY1)

                add(pos, nor, col, k++, p0, p1, pro1, n1, c, dirY0)
                add(pos, nor, col, k++, p2, p3, pro1, n1, c, dirY1)
                add(pos, nor, col, k++, p0, p1, pro0, n0, c, dirY0)

            }
        }
        return SplineTmpMesh(pos, nor, col)
    }

    private fun mirrorIfClosed(index: Int, size: Int): Int {
        return when {
            index < size -> index
            isClosed -> index - size
            else -> size * 2 - index
        }
    }

    private fun findDirY(p0: Vector3d, p1: Vector3d, p3: Vector3d, dst: Vector3f = Vector3f()) {
        if (isStrictlyUp) {
            dst.set(0f, 1f, 0f)
        } else {
            val ax = p1.x - p0.x
            val ay = p1.y - p0.y
            val az = p1.z - p0.z
            val bx = p3.x - p1.x
            val by = p3.y - p1.y
            val bz = p3.z - p1.z
            dst.set(// 23 31 12
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
        JomlPools.vec3d.borrow()
            .set(p1).sub(p0).normalize()
            .toVector3f(dirX)
        positions[k3 + 0] = (mix(p0.x, p1.x, px) + py * dirY.x).toFloat()
        positions[k3 + 1] = (mix(p0.y, p1.y, px) + py * dirY.y).toFloat()
        positions[k3 + 2] = (mix(p0.z, p1.z, px) + py * dirY.z).toFloat()
        normals[k3 + 0] = n.x * dirX.x + n.y * dirY.x
        normals[k3 + 1] = n.x * dirX.y + n.y * dirY.y
        normals[k3 + 2] = n.x * dirX.z + n.y * dirY.z
        colors[k] = c
    }

    override val className: String = "SplineMesh"

    companion object {

        val curveFactor = 0.8

        private val LOGGER = LogManager.getLogger(SplineMesh::class)

        @JvmStatic
        fun main(args: Array<String>) {

            // test interpolation with 1 and 2 intermediate points
            // interpolation with 1 point: just a line, and therefore useless

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
            getIntermediates(p0, n0, p1, n1, imm0, imm1)

            val points = ArrayList<Vector2f>()

            val dst = Vector3d()
            val steps = size / 3 + 1
            for (i in 0 until steps) {
                interpolate(p0, imm0, imm1, p1, i / (steps - 1.0), dst)
                points.add(Vector2f(dst.x.toFloat(), dst.y.toFloat()))
            }

            writeImageCurve(size, size, 255 shl 24, -1, 5, points, "spline1.png")

        }

    }

}