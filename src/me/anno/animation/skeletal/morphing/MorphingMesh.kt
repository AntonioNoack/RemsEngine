package me.anno.animation.skeletal.morphing

import me.anno.animation.skeletal.Skeleton
import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.structures.maps.WeightedMap

class MorphingMesh(
        val base: MorphingBase,
        val targetState: WeightedMap<MorphTarget>
) {

    constructor(base: MorphingBase) : this(base, WeightedMap())

    /**
     * xyz normal (uv stays constant)
     * */
    val points = base.createInstance()

    /**
     * has the base positions
     * */
    private var hasBase = false

    /**
     * the applied weights for delta calculation
     * */
    private val currentState = WeightedMap<MorphTarget>()

    fun create() {
        base.clear(points) // no full clear
        interpolatePoints()
        base.createSmoothNormals(points)
        hasBase = true
        currentState.set(targetState)
    }

    fun update() {
        if (!hasBase) return create()
        interpolatePointsDelta()
        base.createSmoothNormals(points)
    }

    fun interpolatePoints() {
        synchronized(this) {
            for ((target, weight) in targetState) {
                target.apply(points, weight)
            }
        }
    }

    fun interpolatePointsDelta() {
        synchronized(this) {
            for ((target, weight) in targetState) {
                val delta = weight - currentState[target]
                target.apply(points, delta)
            }
            for ((target, weight) in currentState) {
                if (target !in targetState) {// remove residual targets
                    target.apply(points, -weight)
                }
            }
            currentState.set(targetState) // our state now is now targets
        }
    }

    var staticBuffer: StaticBuffer? = null
    var skeletalBuffer: StaticBuffer? = null

    /*fun uploadStatic(): StaticBuffer {

        val faces = base.faces
        val points = base.points
        val vertexCount = faces.size * 3
        val buffer = staticBuffer ?: StaticBuffer(VisualObject.staticAttr, vertexCount, GL_DYNAMIC_DRAW)
        buffer.nioBuffer!!.position(0)

        val pts = this.points
        fun write(i: Int) {
            val pt = points[i]
            val i0 = i * 6
            buffer.put(pts[i0 + 0], pts[i0 + 1], pts[i0 + 2]) // position
            buffer.put(pts[i0 + 3], pts[i0 + 4], pts[i0 + 5]) // normal
            buffer.put(0f, 0f, 0f) // tangent
            buffer.put(pt.u, pt.v)
        }

        val bodyGroup = base.getGroup("body")
        for (faceIndex in bodyGroup) {
            val face = faces[faceIndex]
            write(face.a)
            write(face.b)
            write(face.c)
        }

        staticBuffer = buffer
        return buffer

    }*/

    fun uploadSkeleton(skeleton: Skeleton): StaticBuffer {
        skeletalBuffer = base.createSkeletalMesh(skeleton, points, skeletalBuffer)
        return skeletalBuffer!!
    }

    /*val simplestBuffer = lazy {
        create()
        val faces = base.faces
        val points = base.points
        val buffer = StaticBuffer(VisualObject.staticAttr, faces.size * 3)
        val pts = this.points
        fun write(i: Int) {
            val pt = points[i]
            val i0 = i * 6
            buffer.put(pts[i0 + 0], pts[i0 + 1], pts[i0 + 2]) // coordinates
            buffer.put(pts[i0 + 3], pts[i0 + 4], pts[i0 + 5]) // normal
            buffer.put(0f, 0f, 0f) // tangent
            buffer.put(pt.u, pt.v)
        }

        val bodyGroup = base.getGroup("body")
        for (faceIndex in bodyGroup) {
            val face = faces[faceIndex]
            write(face.a)
            write(face.b)
            write(face.c)
        }
        buffer
    }*/

}