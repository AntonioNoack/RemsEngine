package me.anno.bullet

import com.bulletphysics.collision.narrowphase.GjkEpaSolver
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.TriangleShape
import com.bulletphysics.linearmath.Transform
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.maths.chunks.spherical.HexagonTriangleQuery
import me.anno.utils.Color.a
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toIntOr
import me.anno.utils.types.Triangles
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import speiger.primitivecollections.LongToObjectHashMap
import kotlin.math.max

class HexagonSpherePhysics(
    val sphere: HexagonSphere,
    val shape: ConvexShape,
    val triangleQuery: HexagonTriangleQuery
) {

    val scale = 1.0 / sphere.len

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
        hexagon = sphere.findClosestHexagon(pos)
        currPosition.set(pos)
        if (setMotionNull) velocity.set(0f)
    }

    fun update(dt0: Float) {
        if (hexagon == null) teleport(currPosition, false)

        // if the velocity is too large, apply sub-steps
        val steps = max(1, (2f * velocity.length() / sphere.len).toIntOr())
        val dt = dt0 / steps
        val up = JomlPools.vec3f.borrow()
        repeat(steps) {

            // apply gravity
            // hexagon.center must be used instead of position,
            // because the position introduces small drift towards the hexagon center,
            // because it doesn't align 100% with the current hexagon's triangles
            hexagon!!.center.normalize(up)
            accelerate(up, dt * gravity)

            // apply motion against all colliders
            velocity.mulAdd(dt, currPosition, nextPosition)
            applyCollisions(dt)

            // move
            lastPosition.set(currPosition)
            currPosition.set(nextPosition)

            // update hexagon
            hexagon = sphere.findClosestHexagon(currPosition, hexagon!!)

        }
    }

    private val nullTransform = Transform()

    val yMin: Float
    val yMax: Float

    init {
        nullTransform.basis.identity()
        val aabbMin = Vector3d()
        val aabbMax = Vector3d()
        shape.getBounds(nullTransform, aabbMin, aabbMax)
        yMin = aabbMin.y.toFloat() * sphere.len
        yMax = aabbMax.y.toFloat() * sphere.len
    }

    fun applyCollisions(dt: Float) {
        val center = currPosition.length()
        val bottom = max(center + yMin, 0f)
        val top = center + yMax
        val hex = hexagon!!
        ensureNeighbors(hex)
        defineLocalTransform(currPosition, currTransform)
        defineLocalTransform(nextPosition, nextTransform)
        triangleQuery.query(hex, bottom, top) { a, b, c ->
            applyCollision(a, b, c, dt)
        }
        val neighbors = hex.neighbors
        for (i in neighbors.indices) {
            val neighbor = neighbors[i] ?: continue
            triangleQuery.query(hex, neighbor, i, bottom, top) { a, b, c ->
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
    val tmpQ2 = Quaterniond()

    fun defineLocalTransform(pos: Vector3f, transform: Transform) {
        // define a local transform for the shape :)
        pos.normalToQuaternionY(tmpQ1)
        tmpQ2.set(tmpQ1)
        tmpQ2.conjugate()
        transform.basis.set(tmpQ2)
        transform.origin.set(pos.x * scale, pos.y * scale, pos.z * scale)
    }

    fun applyCollision(a: Vector3f, b: Vector3f, c: Vector3f, dt: Float): Boolean {

        Triangles.subCross(a, b, c, normal)
        if (normal.dot(velocity) < 0f) return false // backside

        triangle.vertices[0].set(a.x * scale, a.y * scale, a.z * scale)
        triangle.vertices[1].set(b.x * scale, b.y * scale, b.z * scale)
        triangle.vertices[2].set(c.x * scale, c.y * scale, c.z * scale)

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
            val da = Vector3d(a)
            val db = Vector3d(b)
            val dc = Vector3d(c)
            DebugShapes.debugLines.add(DebugLine(da, db, color, 0f))
            DebugShapes.debugLines.add(DebugLine(db, dc, color, 0f))
            DebugShapes.debugLines.add(DebugLine(dc, da, color, 0f))
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
        return if (solver.collide(
                shape, localTransform,
                triangle, nullTransform,
                0.0, results
            )
        ) results.depth else 0.0
    }

    fun ensureNeighbors(hex: Hexagon) {
        if (hex.neighbors.any { it == null }) {
            val hexMap = LongToObjectHashMap<Hexagon>()
            hexMap.put(hex.index, hex)
            sphere.ensureNeighbors(arrayListOf(hex), hexMap, 0)
        }
    }

    fun accelerate(dir: Vector3f, dt: Float) {
        accelerate(dir.x, dir.y, dir.z, dt)
    }

    fun accelerate(dx: Float, dy: Float, dz: Float, dt: Float) {
        if (!dt.isFinite()) return
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