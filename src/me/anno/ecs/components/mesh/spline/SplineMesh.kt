package me.anno.ecs.components.mesh.spline

import me.anno.Build
import me.anno.Time
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.EntityQuery.hasComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.spline.Splines.generateSplinePoints
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.EditorState
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.utils.Color.white
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.tuples.get
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Matrix4x3f
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * spline meshes are parts of many simulator games, e.g. street building
 * */
class SplineMesh : ProceduralMesh(), OnUpdate {

    var profile: SplineProfile = TestProfiles.cubeProfile
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

    // todo update based on distance

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

    var pointsPerRadian = 10.0
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
                invalidateMesh()
            }
        }

    var roundStart = false
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    var closedEnd = true
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    var roundEnd = false
        set(value) {
            if (field != value) {
                field = value
                invalidateMesh()
            }
        }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SplineMesh) return
        dst.isStrictlyUp = isStrictlyUp
        dst.pointsPerRadian = pointsPerRadian
        dst.piecewiseLinear = piecewiseLinear
        dst.isClosed = isClosed
        dst.profile = profile
    }

    override fun onUpdate() {
        // if a child is selected, invalidate this
        if (Build.isDebug) {
            val children = entity?.children ?: return
            val lastSelection = EditorState.lastSelection
            for (i in children.indices) {
                val child = children[i]
                if (child.hasComponent(SplineControlPoint::class) && child === lastSelection) {
                    invalidateMesh()
                    break
                }
            }
        }
    }

    override fun generateMesh(mesh: Mesh) {
        val entity = entity
        if (entity == null) {
            lastWarning = "Missing entity, $parent, ${Time.gameTimeN}"
            invalidateMesh()
            return
        }
        val points = entity.children
            .mapNotNull { it.getComponent(SplineControlPoint::class) }
        if (points.size < 2) {
            lastWarning = "SplineMesh doesn't have enough points"
            invalidateMesh()
            return
        }
        lastWarning = null
        if (piecewiseLinear) {
            val list = ArrayList<Mesh>()
            val profile = profile
            for (i in 1 until points.size) {
                list.add(
                    generateLinearMesh(
                        points[i - 1], points[i], Mesh(),
                        // closed start/end is a bit questionable here
                        profile, isClosed, closedStart, closedEnd, isStrictlyUp
                    )
                )
            }
            merge(list, mesh)
        } else {
            generateSplineMesh(
                points, pointsPerRadian, mesh,
                profile, isClosed, closedStart, closedEnd, isStrictlyUp
            )
        }
    }

    companion object {

        fun generateLinearMesh(
            p0: SplineControlPoint, p1: SplineControlPoint, mesh: Mesh,
            profile: SplineProfile, isClosed: Boolean, closedStart: Boolean, closedEnd: Boolean, isStrictlyUp: Boolean
        ) = generateSplineMesh(
            mesh, profile, isClosed, closedStart, closedEnd, isStrictlyUp,
            listOf(
                p0.getLocalPosition(Vector3d(), -1.0),
                p0.getLocalPosition(Vector3d(), +1.0),
                p1.getLocalPosition(Vector3d(), -1.0),
                p1.getLocalPosition(Vector3d(), +1.0),
            )
        )

        fun generateSplineMesh(
            points: List<SplineControlPoint>,
            perRadiant: Double, mesh: Mesh,
            profile: SplineProfile, isClosed: Boolean,
            closedStart0: Boolean, closedEnd0: Boolean,
            isStrictlyUp: Boolean
        ): Mesh {
            val splinePoints = generateSplinePoints(points, perRadiant, isClosed)
            return generateSplineMesh(mesh, profile, isClosed, closedStart0, closedEnd0, isStrictlyUp, splinePoints)
        }

        private fun addStartFace(
            p0a: Vector3d, p0b: Vector3d, nx: Float, profile: SplineProfile, profileFacade: List<Vector2f>,
            dirY0: Vector3f, pos: FloatArray, nor: FloatArray, col: IntArray, k0: Int
        ): Int {
            // add profile
            val ptToColor = HashMap<Vector2f, Int>(profile.positions.size)
            for (i in profile.positions.indices) {// not necessarily correct color, but cannot triangulate it correctly atm anyway
                ptToColor[profile.getPosition(i)] = profile.getColor(i, true)
            }
            // find normal: (p0b-p0a) x dirY
            val normal = dirY0.cross(
                (p0a.x - p0b.x).toFloat(),
                (p0a.y - p0b.y).toFloat(),
                (p0a.z - p0b.z).toFloat(),
                Vector3f()
            ).normalize(nx)
            var k = k0
            if (nx < 0f) {
                for (i in profileFacade.indices.reversed()) {
                    val xy = profileFacade[i]
                    val color = ptToColor[xy] ?: white
                    add2(pos, nor, col, k++, p0a, p0b, xy, color, dirY0, normal)
                }
            } else {
                for (i in profileFacade.indices) {
                    val xy = profileFacade[i]
                    val color = ptToColor[xy] ?: white
                    add2(pos, nor, col, k++, p0a, p0b, xy, color, dirY0, normal)
                }
            }
            return k
        }

        fun generateSplineMesh(
            mesh: Mesh,
            profile: SplineProfile,
            isClosed: Boolean,
            closedStart0: Boolean,
            closedEnd0: Boolean,
            isStrictlyUp: Boolean,
            splinePoints: List<Vector3d>
        ): Mesh {

            val profileFacade = if (!isClosed && (closedStart0 || closedEnd0)) profile.getFacade() else null
            val closedStart = profileFacade != null && closedStart0
            val closedEnd = profileFacade != null && closedEnd0

            // generate the mesh
            val profileSize = profile.getSize2()
            val splineSize = splinePoints.size.shr(1) - 1
            val numPoints = 6 * profileSize * splineSize +
                    (profileFacade?.size ?: 0) * (closedStart.toInt() + closedEnd.toInt())
            val numCoords = 3 * numPoints
            val pos = mesh.positions.resize(numCoords)
            val nor = mesh.normals.resize(numCoords)
            val col = mesh.color0.resize(numPoints)
            val uvs = mesh.uvs.resize(numPoints * 2)
            var k = 0
            val n0 = Vector2f()
            val n1 = Vector2f()
            val dirY0 = Vector3f(0f, 1f, 0f)
            val dirY1 = Vector3f(0f, 1f, 0f)

            // todo option for round start/end

            if (closedStart) {
                val p0a = splinePoints[0]
                val p0b = splinePoints[1]
                if (!isStrictlyUp) findDirY(p0a, p0b, splinePoints[3], dirY0)
                k = addStartFace(
                    p0a, p0b, 1f,
                    profile, profileFacade!!, dirY0,
                    pos, nor, col, k
                )
            }

            if (closedEnd) {
                val p0a = splinePoints[splinePoints.size - 2]
                val p0b = splinePoints[splinePoints.size - 1]
                if (!isStrictlyUp) findDirY(
                    splinePoints[splinePoints.size - 4],
                    splinePoints[splinePoints.size - 3],
                    splinePoints[splinePoints.size - 2],
                    dirY0
                )
                k = addStartFace(
                    p0a, p0b, -1f,
                    profile, profileFacade!!, dirY0,
                    pos, nor, col, k
                )
            }

            val dirX0a = Vector3f()
            val dirX1a = Vector3f()
            val dirX0b = Vector3f()
            val dirX1b = Vector3f()

            for (j in 0 until profileSize) {

                val pro0 = profile.getPosition(j)
                val pro1 = profile.getPosition(j + 1)

                profile.getNormal(j, false, n0)
                profile.getNormal(j, true, n1)

                val c0 = profile.getColor(j, true)
                val c1 = profile.getColor(j + 1, false)

                var p0a = splinePoints[0]
                var p0b = splinePoints[1]

                if (!isStrictlyUp) {
                    if (splinePoints.size > 3) {
                        findDirY(p0a, p0b, splinePoints[3], dirY0)
                    } else {
                        // if spline size is 2
                        dirY0.set(0f, 1f, 0f)
                        dirY1.set(0f, 1f, 0f)
                    }
                }

                fun findDirXs(dirX0a: Vector3f, dirX0b: Vector3f, dirY0: Vector3f, i2: Int) {
                    val sp = splinePoints.size
                    val closed0 = isClosed && i2 == 0
                    val closed1 = isClosed && i2 + 2 == sp
                    val i0 = if (closed0) sp - 2 else max(i2 - 2, 0)
                    val i1 = if (closed1) 0 else min(i2 + 2, sp - 2)
                    dirX(splinePoints[i0], splinePoints[i1], dirY0, dirX0a)
                    dirX(splinePoints[i0 + 1], splinePoints[i1 + 1], dirY0, dirX0b)
                }

                findDirXs(dirX0a, dirX0b, dirY0, 0)

                val u0 = profile.getU(j, true)
                val u1 = profile.getU(j + 1, false)

                var v0 = 0f
                for (i in 0 until splineSize) {

                    val i2 = i * 2
                    val p1a = splinePoints[i2 + 2]
                    val p1b = splinePoints[i2 + 3]

                    val v1 = v0 + (p0a.distance(p1a) + p0b.distance(p1b)).toFloat() * 0.5f

                    if (!isStrictlyUp && i2 + 5 < splinePoints.size) {
                        findDirY(p1a, p1b, splinePoints[i2 + 5], dirY1)
                    }

                    findDirXs(dirX1a, dirX1b, dirY1, i2)

                    // 012 230
                    add(pos, nor, col, uvs, k++, p0a, p0b, pro0, n0, c0, dirX0a, dirX0b, dirY0, u0, v0)
                    add(pos, nor, col, uvs, k++, p1a, p1b, pro1, n1, c1, dirX1a, dirX1b, dirY1, u1, v1)
                    add(pos, nor, col, uvs, k++, p1a, p1b, pro0, n0, c0, dirX1a, dirX1b, dirY1, u1, v0)

                    add(pos, nor, col, uvs, k++, p0a, p0b, pro1, n1, c1, dirX0a, dirX0b, dirY0, u0, v1)
                    add(pos, nor, col, uvs, k++, p1a, p1b, pro1, n1, c1, dirX1a, dirX1b, dirY1, u1, v1)
                    add(pos, nor, col, uvs, k++, p0a, p0b, pro0, n0, c0, dirX0a, dirX0b, dirY0, u0, v0)

                    p0a = p1a
                    p0b = p1b

                    dirX0a.set(dirX1a)
                    dirX0b.set(dirX1b)
                    dirY0.set(dirY1)
                    v0 = v1
                }
            }
            return createMesh(mesh, pos, nor, col, uvs)
        }

        fun createMesh(
            mesh: Mesh?,
            positions: FloatArray,
            normals: FloatArray,
            colors: IntArray,
            uvs: FloatArray
        ): Mesh {
            val mesh1 = mesh ?: Mesh()
            mesh1.positions = positions
            mesh1.normals = normals
            mesh1.color0 = colors
            mesh1.uvs = uvs
            return mesh1
        }

        private fun findDirY(p1a: Vector3d, p1b: Vector3d, p2b: Vector3d, dst: Vector3f = Vector3f()) {
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

        fun dirX(p1a: Vector3d, p1b: Vector3d, dst: Vector3f) {
            dst.set(
                (p1b.x - p1a.x).toFloat(),
                (p1b.y - p1a.y).toFloat(),
                (p1b.z - p1a.z).toFloat()
            ).normalize()
        }

        fun dirX(p1a: Vector3d, p1b: Vector3d, up: Vector3f, dst: Vector3f) {
            dst.set(
                (p1b.x - p1a.x).toFloat(),
                (p1b.y - p1a.y).toFloat(),
                (p1b.z - p1a.z).toFloat()
            ).cross(up).normalize()
        }

        fun add(
            positions: FloatArray, normals: FloatArray, colors: IntArray, uvs: FloatArray,
            k: Int, p0: Vector3d, p1: Vector3d, profile: Vector2f, n: Vector2f, c: Int,
            dirXa: Vector3f, dirXb: Vector3f, dirY: Vector3f, u: Float, v: Float
        ) {
            val dirX = if (n.x > 0f) dirXa else dirXb
            val k2 = k * 2
            val k3 = k2 + k
            val px = (profile.x * .5f + .5f).toDouble()
            val py = profile.y
            positions[k3 + 0] = (mix(p0.x, p1.x, px) + py * dirY.x).toFloat()
            positions[k3 + 1] = (mix(p0.y, p1.y, px) + py * dirY.y).toFloat()
            positions[k3 + 2] = (mix(p0.z, p1.z, px) + py * dirY.z).toFloat()
            normals[k3 + 0] = n.x * dirX.x + n.y * dirY.x
            normals[k3 + 1] = n.x * dirX.y + n.y * dirY.y
            normals[k3 + 2] = n.x * dirX.z + n.y * dirY.z
            uvs[k2] = u
            uvs[k2 + 1] = v
            colors[k] = c
        }

        private fun add2(
            positions: FloatArray, normals: FloatArray, colors: IntArray,
            k: Int, p0: Vector3d, p1: Vector3d, profile: Vector2f, color: Int,
            dirY: Vector3f, normal: Vector3f
        ) {
            val k3 = k * 3
            val px = profile.x * 0.5 + 0.5
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
            colors[k] = color
        }

        fun createEndPiece(
            point: SplineControlPoint,
            useRightForEnd: Boolean, pointsPerRadiant: Double,
            mesh: Mesh,
        ) {

            // generate end piece: rotational
            val profile = point.profile
            val halfProfile = profile.split()[!useRightForEnd]
            val numAngles = 1 + max(1, (pointsPerRadiant * PI).roundToIntOr())
            val profileSize = halfProfile.positions.size
            val numQuads = (profileSize - 1) * (numAngles - 1)
            val numPoints = numQuads * 6
            val pos = mesh.positions.resize(numPoints * 3)
            val nor = mesh.normals.resize(numPoints * 3)
            val col = mesh.color0.resize(numPoints)
            val uvs = mesh.uvs.resize(numPoints * 2)
            val dirY = point.getLocalUp(Vector3f())
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

                    val u0 = halfProfile.getU(si - 1, true)
                    val u1 = halfProfile.getU(si, false)

                    // todo find proper vs by length somehow...
                    val v0 = 0f
                    val v1 = 1f

                    add(pos, nor, col, uvs, k++, p0a, p0b, p0, n0, c0, dirX0, dirX0, dirY, u0, v0)
                    add(pos, nor, col, uvs, k++, p1a, p1b, p1, n1, c1, dirX1, dirX1, dirY, u1, v1)
                    add(pos, nor, col, uvs, k++, p1a, p1b, p0, n0, c0, dirX1, dirX1, dirY, u1, v0)

                    add(pos, nor, col, uvs, k++, p0a, p0b, p1, n1, c1, dirX0, dirX0, dirY, u0, v1)
                    add(pos, nor, col, uvs, k++, p1a, p1b, p1, n1, c1, dirX1, dirX1, dirY, u1, v1)
                    add(pos, nor, col, uvs, k++, p0a, p0b, p0, n0, c0, dirX0, dirX0, dirY, u0, v0)

                    p0 = p1
                }

                p0a.set(p1a)
                p0b.set(p1b)
                dirX0.set(dirX1)
            }
            mesh.positions = pos
            mesh.normals = nor
            mesh.color0 = col
            mesh.uvs = uvs
        }

        fun merge(meshes: List<Mesh>, dst: Mesh) {
            object : MeshJoiner<Mesh>(
                hasColors = true,
                hasBones = false,
                mayHaveUVs = true
            ) {
                override fun getMesh(element: Mesh): Mesh = element
                override fun getTransform(element: Mesh, dst: Matrix4x3f) {}
            }.join(dst, meshes)
        }
    }
}