package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.sq
import me.anno.utils.types.AABBs.reset
import me.anno.utils.types.AABBs.set
import me.anno.utils.types.AABBs.transformAABB
import me.anno.utils.types.Triangles.rayTriangleIntersection
import me.anno.utils.types.Vectors.toVector3f
import org.joml.*

object Raycast {

    // todo radius for the ray, like sphere-trace, e.g. for bullets

    // todo function for raycast collider (faster but less accurate for meshes)

    /**
     * returns whether something was hit,
     * more information is saved in the result
     * todo this function doesn't seem to be working for large distances, and idk why...
     * */
    fun raycastTriangles(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        maxLength: Double,
        collisionMask: Int = -1,
        includeDisabled: Boolean = false,
        tmpAABB: AABBd = AABBd(),
        result: RayHit = RayHit()
    ): RayHit? {
        if (maxLength <= 0) return null
        result.distance = maxLength
        direction.normalize()
        val end = Vector3d(direction).mul(maxLength).add(start)
        return raycastTriangles(entity, start, direction, end, collisionMask, includeDisabled, tmpAABB, result)
    }

    /**
     * finds the minimum distance triangle
     * returns whether something was hit
     * */
    fun raycastTriangles(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        collisionMask: Int = -1,
        includeDisabled: Boolean = false,
        tmpAABB: AABBd,
        result: RayHit = RayHit()
    ): RayHit? {
        if (result.distance <= 0) return null
        val startDistance = result.distance
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (includeDisabled || component.isEnabled) {
                when (component) {
                    is MeshComponent -> {
                        val mesh = component.mesh ?: continue
                        /*if (raycastMeshTriangleOld(entity, mesh, start, direction, end, Matrix4x3d(), result)) {
                            result.meshComponent = component
                        }*/
                        if (raycastMeshTriangle(entity, mesh, start, direction, end, tmpAABB, result)) {
                            result.meshComponent = component
                        }
                    }
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if ((includeDisabled || child.isEnabled)) {
                if (testLineAABB(child.aabb, start, end)) {
                    raycastTriangles(
                        child, start, direction, end,
                        collisionMask, includeDisabled, tmpAABB, result
                    )
                }
            }
        }
        return if (result.distance < startDistance) result else null
    }

    fun testLineAABB(aabb: AABBd, start: Vector3d, end: Vector3d): Boolean {

        // this didn't work for 1e100, but now it does... what happened??...
        return aabb.testRay(start.x, start.y, start.z, end.x - start.x, end.y - start.y, end.z - start.z)

        // todo this function looks accurate, but apparently, it's not... why???
        // todo when returning true, the correct triangles are found
        // todo using by tests, they are not found...
        // todo I need a padding of some kind???...
        // todo maybe test when and how the results look like
        // return true
        // finite values result in issues
        var sx = start.x
        var sy = start.y
        var sz = start.z
        var ex = end.x
        var ey = end.y
        var ez = end.z
        val cx = (aabb.minX + aabb.maxX) * 0.5
        val cy = (aabb.minY + aabb.maxY) * 0.5
        val cz = (aabb.minZ + aabb.maxZ) * 0.5
        for (i in 0 until 50) {// multiple iterations to improve the accuracy, if start and end are extremely far from one-another
            if (aabb.testPoint(sx, sy, sz) || aabb.testPoint(ex, ey, ez)) {
                // println("simple return")
                return true
            }
            val distSq = sq(ex - sx, ey - sy, ez - sz)
            val fraction = ((cx - sx) * (ex - sx) +
                    (cy - sy) * (ey - sy) +
                    (cz - sz) * (ez - sz)) / distSq
            if (fraction < 0.0 || fraction > 1.0 || !fraction.isFinite()) {
                // the point is outside, or lays on already tested points
                // when the point is 0.0 or 1.0, that may not actually be true ->
                // give these cases more iterations
                // println("fraction invalid $fraction")
                return false
            }
            val mx = mix(sx, ex, fraction)
            val my = mix(sy, ey, fraction)
            val mz = mix(sz, ez, fraction)
            // the closest point of the line to the (center of the) box is (mx,my,mz)
            if (aabb.testPoint(mx, my, mz)) {
                // println("fraction $fraction was correct: $mx in ${aabb.minX} .. ${aabb.maxX}")
                return true
            } else {

                // may only help in non-realistic scenarios...

                // maybe the true result is in the neighborhood:
                // test a smaller area

                // just using +/- 1e-8 would use steps of size 10^8, which may be much too slow, e.g. for 1^100
                val accuracy = 1e-12 // guessed accuracy; = 1.0 / worst case step, so a smaller value is faster
                val fract0 = fraction * (1.0 - accuracy)
                val fract1 = fraction * (1.0 + accuracy)

                val sx2 = mix(sx, ex, fract0)
                val sy2 = mix(sy, ey, fract0)
                val sz2 = mix(sz, ez, fract0)

                ex = mix(sx, ex, fract1)
                ey = mix(sy, ey, fract1)
                ez = mix(sz, ez, fract1)

                sx = sx2
                sy = sy2
                sz = sz2

                // println("testing $i from $fraction on ${sqrt(distSq)}, ${start.x} - ${end.y}, $sx .. $ex")

            }
        }
        return false
    }

    fun testLineAABB(aabb: AABBf, start: Vector3f, end: Vector3f): Boolean {
        // return testLineAABB(AABBd().set(aabb), Vector3d(start), Vector3d(end))
        if (aabb.testPoint(start) || aabb.testPoint(end)) return true
        // finite values result in issues
        val cx = (aabb.minX + aabb.maxX) * 0.5f
        val cy = (aabb.minY + aabb.maxY) * 0.5f
        val cz = (aabb.minZ + aabb.maxZ) * 0.5f
        val fraction = ((cx - start.x) * (end.x - start.x) +
                (cy - start.y) * (end.y - start.y) +
                (cz - start.z) * (end.z - start.z)) / start.distanceSquared(end)
        if (fraction <= 0.0f || fraction >= 1.0f) {
            // the point is outside, or lays on already tested points
            return false
        }
        val mx = mix(start.x, end.x, fraction)
        val my = mix(start.y, end.y, fraction)
        val mz = mix(start.z, end.z, fraction)
        // the closest point of the line to the (center of the) box is (mx,my,mz)
        return aabb.testPoint(mx, my, mz)
    }

    /**
     * casts a ray onto a mesh
     * the mesh should not be too large, as all triangles will be tested
     * */
    fun raycastMeshTriangle(
        entity: Entity, mesh: Mesh,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        tmpAABB: AABBd,
        result: RayHit,
    ): Boolean {

        // transform the ray into local mesh coordinates
        val globalTransform = entity.transform.globalTransform // local -> global

        // first test whether the aabbs really overlap
        val globalAABB = tmpAABB.set(mesh.aabb)
        transformAABB(globalAABB, globalTransform)

        if (testLineAABB(globalAABB, start, end)) {

            val startDistance = result.distance

            val ga = result.tmpV3a
            val gb = result.tmpV3b
            val gc = result.tmpV3c

            mesh.forEachTriangle(ga, gb, gc) { a, b, c ->

                globalTransform.transformPosition(a)
                globalTransform.transformPosition(b)
                globalTransform.transformPosition(c)

                val tmpPosition = result.tmpPosition
                val tmpNormal = result.tmpNormal
                val distance = rayTriangleIntersection(
                    start, direction, a, b, c, result.distance, tmpNormal, tmpPosition
                )

                if (distance < result.distance) {
                    result.distance = distance
                    result.positionWS.set(tmpPosition)
                    result.normalWS.set(tmpNormal)
                }
            }

            return result.distance < startDistance

        } else return false

    }

    fun raycastMeshTriangleOld(
        entity: Entity, mesh: Mesh,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        inverse: Matrix4x3d, result: RayHit,
    ): Boolean {

        var hasHitTriangle = false

        // transform the ray into local mesh coordinates
        val globalTransform = entity.transform.globalTransform // local -> global
        inverse.set(globalTransform).invert()
        val localStart = inverse.transformPosition(Vector3d(start)).toVector3f()
        val localDir = inverse.transformDirection(Vector3d(direction)).toVector3f().normalize()
        val localEnd = inverse.transformPosition(Vector3d(end)).toVector3f()

        // if any coordinates of start or end are invalid, work in global coordinates
        val hasValidCoordinates =
            localStart.x.isFinite() && localStart.y.isFinite() && localStart.z.isFinite() &&
                    localDir.x.isFinite() && localDir.y.isFinite() && localDir.z.isFinite()

        // the fast method only works, if the numbers have roughly the same order of magnitude
        val relativePositionsSquared = (localStart.lengthSquared() + 1e-7f) / (localEnd.lengthSquared() + 1e-7f)
        val orderOfMagnitudeIsFine = relativePositionsSquared in 0.001f..1000f

        // todo if it is animated, we should ignore the aabb, and must apply the appropriate bone transforms
        if (hasValidCoordinates && orderOfMagnitudeIsFine) {

            // println(Vector3f(localEnd).sub(localStart).normalize().dot(localDir))

            // test whether we intersect the aabb of this mesh
            if (testLineAABB(mesh.aabb, localStart, localEnd)) {

                // test whether we intersect any triangle of this mesh
                var maxDistance = localStart.distance(localEnd)
                val tmpPosition = Vector3f()
                val tmpNormal = Vector3f()
                val localPosition = Vector3f()
                val localNormal = Vector3f()
                mesh.forEachTriangle { a, b, c ->
                    // check collision of localStart-localEnd with triangle a,b,c
                    val localDistance = rayTriangleIntersection(
                        localStart, localDir, a, b, c, maxDistance, tmpNormal, tmpPosition
                    )
                    if (localDistance < maxDistance) {
                        maxDistance = localDistance
                        hasHitTriangle = true
                        localPosition.set(tmpPosition)
                        localNormal.set(tmpNormal)
                    }
                }

                if (hasHitTriangle) {
                    result.setFromLocal(globalTransform, localPosition, localNormal, start, direction, end)
                }

            }

        } else {

            // mesh is scaled to zero on some axis, need to work in global coordinates
            // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates

            // first test whether the aabbs really overlap
            val globalAABB = AABBd().set(mesh.aabb)
            transformAABB(globalAABB, globalTransform)

            if (testLineAABB(globalAABB, start, end)) {

                val ga = Vector3d()
                val gb = Vector3d()
                val gc = Vector3d()

                val tmpNormal = Vector3d()
                val tmpHit = Vector3d()
                mesh.forEachTriangle { a, b, c ->
                    globalTransform.transformPosition(ga.set(a))
                    globalTransform.transformPosition(gb.set(b))
                    globalTransform.transformPosition(gc.set(c))
                    val distance = rayTriangleIntersection(
                        start, direction, ga, gb, gc, result.distance, tmpNormal, tmpHit
                    )
                    if (distance < result.distance) {
                        result.distance = distance
                        hasHitTriangle = true
                        result.positionWS.set(tmpHit)
                        result.normalWS.set(tmpNormal)
                    }
                }

            }

        }

        return hasHitTriangle

    }

    @JvmStatic
    fun main(args: Array<String>) {

        val y = 8e26
        val z = 15e21

        val f = 1e-15

        val start = Vector3d(-1e150, y, z)
        val end = Vector3d(1e100, y, z)

        val aabb = AABBd()

        for (i in 0 until 1000) {

            val x = 1e31 * Math.random()
            println(x)

            aabb.reset()
            aabb.union(x * (1 - f), y * (1 - f), z * (1 - f))
            aabb.union(x * (1 + f), y * (1 + f), z * (1 + f))

            val result = testLineAABB(aabb, start, end)
            if (!result) throw RuntimeException()

        }

    }

}