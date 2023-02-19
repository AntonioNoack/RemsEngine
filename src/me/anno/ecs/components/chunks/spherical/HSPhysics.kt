package me.anno.ecs.components.chunks.spherical

import com.bulletphysics.collision.narrowphase.GjkEpaSolver
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.TriangleShape
import com.bulletphysics.linearmath.Transform
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes.debugLines
import me.anno.utils.Color.a
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles
import me.anno.utils.types.Vectors.normalToQuaternion
import org.joml.Quaternionf
import org.joml.Vector3f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3d
import kotlin.math.max

class HSPhysics(
    val world: LargeHexagonSphere,
    val shape: ConvexShape,
    val triangleQuery: TriangleQuery
) {

    val scale = 1.0 / world.len

    interface TriangleQuery {
        fun run(
            hex1: Hexagon, minY: Float, maxY: Float,
            callback: (Vector3f, Vector3f, Vector3f) -> Boolean
        )

        fun run(
            hex1: Hexagon, hex2: Hexagon, i: Int, minY: Float, maxY: Float,
            callback: (Vector3f, Vector3f, Vector3f) -> Boolean
        )
    }

    val lastPosition = Vector3f()
    val currPosition = Vector3f()
    val nextPosition = Vector3f()

    val velocity = Vector3f()
    var mass = 1f
    var gravity = -9.81f
    var hexagon: Hexagon? = null

    fun init(pos: Vector3f) {
        teleport(pos, false)
    }

    fun teleport(pos: Vector3f, setMotionNull: Boolean) {
        hexagon = world.findClosestHexagon(pos)
        currPosition.set(pos)
        if (setMotionNull) velocity.set(0f)
    }

    fun update(dt0: Float) {
        if (hexagon == null) teleport(currPosition, false)

        val steps = 1
        for (i in 0 until steps) {

            val dt = dt0 / steps

            // apply gravity
            // hexagon.center must be used instead of position,
            // because the position introduces small drift towards the hexagon center,
            // because it doesn't align 100% with the current hexagon's triangles
            val up = JomlPools.vec3f.borrow()
            hexagon!!.center.normalize(up)
            accelerate(up, dt * gravity)

            // apply motion against all colliders
            velocity.mulAdd(dt, currPosition, nextPosition)
            applyCollisions(dt)

            // todo if the velocity is too large, apply sub-steps

            // move
            lastPosition.set(currPosition)
            currPosition.set(nextPosition)

            // update hexagon
            hexagon = world.findClosestHexagon(currPosition, hexagon!!)

        }
    }

    private val nullTransform = Transform()
    val yMin: Float
    val yMax: Float

    init {
        nullTransform.basis.setIdentity()
        val aabbMin = Vector3d()
        val aabbMax = Vector3d()
        shape.getAabb(nullTransform, aabbMin, aabbMax)
        yMin = aabbMin.y.toFloat() * world.len
        yMax = aabbMax.y.toFloat() * world.len
    }

    fun applyCollisions(dt: Float) {
        val center = currPosition.length()
        val bottom = max(center + yMin, 0f)
        val top = center + yMax
        val hex = hexagon!!
        ensureNeighbors(hex)
        defineLocalTransform(currPosition, currTransform)
        defineLocalTransform(nextPosition, nextTransform)
        triangleQuery.run(hex, bottom, top) { a, b, c ->
            applyCollision(a, b, c, dt)
        }
        val neighbors = hex.neighbors
        for (i in neighbors.indices) {
            val neighbor = neighbors[i] ?: continue
            triangleQuery.run(hex, neighbor, i, bottom, top) { a, b, c ->
                applyCollision(a, b, c, dt)
            }
        }
    }

    val currTransform = Transform()
    val nextTransform = Transform()

    val triangle = TriangleShape()
    val results = GjkEpaSolver.Results()
    val solver = GjkEpaSolver()

    val normal = Vector3f()
    val tmpQ1 = Quaternionf()
    val tmpQ2 = Quat4f()

    fun defineLocalTransform(pos: Vector3f, transform: Transform) {
        // define a local transform for the shape :)
        pos.normalToQuaternion(tmpQ1)
        tmpQ2.set(-tmpQ1.x, -tmpQ1.y, -tmpQ1.z, tmpQ1.w)
        transform.basis.set(tmpQ2)
        transform.origin.set(pos.x * scale, pos.y * scale, pos.z * scale)
    }

    fun applyCollision(a: Vector3f, b: Vector3f, c: Vector3f, dt: Float): Boolean {

        Triangles.subCross(a, b, c, normal)
        if (normal.dot(velocity) < 0f) return false // backside

        triangle.vertices1[0].set(a.x * scale, a.y * scale, a.z * scale)
        triangle.vertices1[1].set(b.x * scale, b.y * scale, b.z * scale)
        triangle.vertices1[2].set(c.x * scale, c.y * scale, c.z * scale)

        val nextDepth = measureCollisionDepth(nextTransform)
        if (nextDepth == 0.0) {
            showTriangle(a, b, c, debugMeshInactiveColor)
            return false
        }

        val currDepth = measureCollisionDepth(currTransform)
        return if (currDepth < nextDepth) { // motion would be bad
            normal.normalize()
            normal.mulAdd(-normal.dot(velocity), velocity, velocity)
            velocity.mulAdd(dt, currPosition, nextPosition)
            defineLocalTransform(nextPosition, nextTransform)
            showTriangle(a, b, c, debugMeshActiveColor)
            true
        } else {
            showTriangle(a, b, c, debugMeshInactiveColor)
            false
        }
    }

    fun showTriangle(a: Vector3f, b: Vector3f, c: Vector3f, color: Int) {
        if (color.a() > 0) {
            debugLines.add(DebugLine(org.joml.Vector3d(a), org.joml.Vector3d(b), color, 0.0))
            debugLines.add(DebugLine(org.joml.Vector3d(b), org.joml.Vector3d(c), color, 0.0))
            debugLines.add(DebugLine(org.joml.Vector3d(c), org.joml.Vector3d(a), color, 0.0))
        }
    }

    /**
     * set an opaque color to enable debugging:
     * then all triangles will be shown in edit mode
     * */
    var debugMeshActiveColor = 0

    /**
     * set an opaque color to enable debugging:
     * then all triangles will be shown in edit mode
     * */
    var debugMeshInactiveColor = 0

    fun measureCollisionDepth(localTransform: Transform): Double {
        return if (solver.collide(shape, localTransform, triangle, nullTransform, 0.0, results))
            results.depth else 0.0
    }

    fun ensureNeighbors(hex: Hexagon) {
        if (hex.neighbors.any { it == null })
            world.ensureNeighbors(arrayListOf(hex), hashMapOf(hex.index to hex), 0)
    }

    fun accelerate(dir: Vector3f, dt: Float) {
        accelerate(dir.x, dir.y, dir.z, dt)
    }

    fun accelerate(dx: Float, dy: Float, dz: Float, dt: Float) {
        if (!dt.isFinite()) throw IllegalArgumentException("Acceleration must be finite")
        velocity.add(dt * dx, dt * dy, dt * dz)
    }

    @Suppress("unused")
    fun addForce(dir: Vector3f, dt: Float) {
        addForce(dir.x, dir.y, dir.z, dt)
    }

    fun addForce(dx: Float, dy: Float, dz: Float, dt: Float) {
        accelerate(dx, dy, dz, dt / mass)
    }

}