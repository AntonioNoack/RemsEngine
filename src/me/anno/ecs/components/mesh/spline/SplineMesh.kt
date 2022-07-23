package me.anno.ecs.components.mesh.spline

import me.anno.Build
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix2d
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import org.joml.Vector2f
import org.joml.Vector3d

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
        // if a child is selected, invalidate this
        return min(update(), super.onUpdate())
    }

    private fun update(): Int {
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
            lastWarning = "Missing entity -> cannot detect children"
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
                } else set(generateSplineMesh(points, pointsPerRadiant, mesh))
            }
        }
    }

    private fun generateLineMesh(p0: SplineControlPoint, p1: SplineControlPoint, src: Mesh?): SplineTmpMesh {
        return generateSplineMesh(listOf(p0, p1), 0.0, src)
    }

    private fun generateSplineMesh(
        points: List<SplineControlPoint>,
        perRadiant: Double,
        src: Mesh?
    ): SplineTmpMesh {

        val tmp = Array(points.size * 8) { Vector3d() }
        var j = 0
        for (i in points.indices) {
            val pt = points[i]
            pt.localToParentPos(-1.0, -1.0, tmp[j++])
            pt.localToParentDir(+1.0, -1.0, tmp[j++])
            pt.localToParentPos(+1.0, -1.0, tmp[j++])
            pt.localToParentDir(-1.0, -1.0, tmp[j++])
            pt.localToParentPos(-1.0, -1.0, tmp[j++])
            pt.localToParentDir(+1.0, +1.0, tmp[j++])
            pt.localToParentPos(+1.0, +1.0, tmp[j++])
            pt.localToParentDir(-1.0, -1.0, tmp[j++])
        }

        val splinePoints = Splines.generateSplineLineQuad(tmp, perRadiant)

        // generate the mesh

        val profile = profile!!
        val profileSize = profile.getSize2()

        var splineSize = splinePoints.size / 4
        if (!isClosed) splineSize--
        val numPoints = 6 * profileSize * splineSize
        val numCoords = 3 * numPoints
        val pos = src?.positions.resize(numCoords)
        val nor = src?.normals.resize(numCoords)
        val col = src?.color0.resize(numPoints)
        var k = 0
        val n0 = Vector2f()
        val n1 = Vector2f()
        for (q in 0 until profileSize) {

            val pro0 = profile.getPosition(q)
            val pro1 = profile.getPosition(q + 1)

            profile.getNormal(q, false, n0)
            profile.getNormal(q, true, n1)

            val color = profile.getColor(q)

            var pos0b = splinePoints[0]
            var pos1b = splinePoints[1]
            var pos0t = splinePoints[2]
            var pos1t = splinePoints[3]

            for (i in 0 until splineSize) {

                val i4 = i * 4
                val pos2b = splinePoints[(i4 + 4) % splinePoints.size]
                val pos3b = splinePoints[(i4 + 5) % splinePoints.size]
                val pos2t = splinePoints[(i4 + 6) % splinePoints.size]
                val pos3t = splinePoints[(i4 + 7) % splinePoints.size]

                // 012 230
                add(pos, nor, col, k++, pos0b, pos1b, pos0t, pos1t, pro0, n0, color)
                add(pos, nor, col, k++, pos2b, pos3b, pos2t, pos3t, pro1, n1, color)
                add(pos, nor, col, k++, pos2b, pos3b, pos2t, pos3t, pro0, n0, color)

                add(pos, nor, col, k++, pos0b, pos1b, pos0t, pos1t, pro1, n1, color)
                add(pos, nor, col, k++, pos2b, pos3b, pos2t, pos3t, pro1, n1, color)
                add(pos, nor, col, k++, pos0b, pos1b, pos0t, pos1t, pro0, n0, color)

                pos0b = pos2b
                pos1b = pos3b
                pos0t = pos2t
                pos1t = pos3t

            }
        }
        return SplineTmpMesh(pos, nor, col)
    }

    private fun add(
        positions: FloatArray, normals: FloatArray, colors: IntArray,
        k: Int, pos0b: Vector3d, pos1b: Vector3d, pos0t: Vector3d, pos1t: Vector3d,
        profile: Vector2f, n: Vector2f, color: Int,
    ) {
        val k3 = k * 3
        val px = (profile.x * .5f + .5f).toDouble()
        val py = (profile.y * .5f + .5f).toDouble()
        val dirX = JomlPools.vec3f.create()
        val tmp = JomlPools.vec3d.borrow()
        dirX.set(tmp.set(pos1b).add(pos1t).sub(pos0b).sub(pos0t).normalize())
        val dirY = JomlPools.vec3f.create()
        dirY.set(tmp.set(pos0t).add(pos1t).sub(pos0b).sub(pos1b).normalize())
        positions[k3 + 0] = mix2d(pos0b.x, pos0t.x, pos1b.x, pos1t.x, px, py).toFloat()
        positions[k3 + 1] = mix2d(pos0b.y, pos0t.y, pos1b.y, pos1t.y, px, py).toFloat()
        positions[k3 + 2] = mix2d(pos0b.z, pos0t.z, pos1b.z, pos1t.z, px, py).toFloat()
        normals[k3 + 0] = n.x * dirX.x + n.y * dirY.x
        normals[k3 + 1] = n.x * dirX.y + n.y * dirY.y
        normals[k3 + 2] = n.x * dirX.z + n.y * dirY.z
        colors[k] = color
        JomlPools.vec3f.sub(2)
    }

    override val className: String = "SplineMesh"

    companion object {
        val curveFactor = 0.8
    }

}