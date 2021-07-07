package me.anno.ecs

import org.joml.*

class Transform {

    private val pos = Vector3d()
    var localPosition: Vector3d
        get() = pos
        set(value) {
            pos.set(value)
            localTransform.m30(value.x)
            localTransform.m31(value.y)
            localTransform.m32(value.z)
            invalidateGlobal()
        }

    private val rot = Quaterniond()
    var localRotation: Quaterniond
        get() = rot
        set(value) {
            rot.set(value)
            localTransform.identity()
                .translate(pos)
                .rotate(value)
                .scale(sca)
            invalidateGlobal()
        }

    private val sca = Vector3d(1.0)
    var localScale: Vector3d
        get() = sca
        set(value) {
            sca.set(value)
            localTransform.identity()
                .translate(pos)
                .rotate(rot)
                .scale(value)
            invalidateGlobal()
        }

    val worldTransform = Matrix4x3d()
    val localTransform = Matrix4x3d()

    fun updateLocal() {
        pos.set(localTransform.m30(), localTransform.m31(), localTransform.m32())
        localTransform.getUnnormalizedRotation(rot);rot.normalize()
        localTransform.getScale(sca)
    }

    // todo only update if changed to save resources
    fun update(parent: Transform?) {

        /*localTransform.identity()

        localTransform.translate(localPosition)

        localTransform.rotate(localRotation)
        // localTransform.rotateY(relativeRotation.y)
        // localTransform.rotateX(relativeRotation.x)
        // localTransform.rotateZ(relativeRotation.z)

        localTransform.scale(localScale)*/

        if (parent == null) {
            worldTransform.identity()
        } else {
            worldTransform.set(parent.worldTransform)
        }

        worldTransform.mul(localTransform)

    }

    fun setLocal(values: Matrix4f) {
        localTransform.set(
            values.m00().toDouble(), values.m01().toDouble(), values.m02().toDouble(),
            values.m10().toDouble(), values.m11().toDouble(), values.m12().toDouble(),
            values.m20().toDouble(), values.m21().toDouble(), values.m22().toDouble(),
            values.m30().toDouble(), values.m31().toDouble(), values.m32().toDouble(),
        )
        // updateLocal()
        // invalidate()
        // todo this is 99% correct, now update the matrix
        // todo where is the ghost rotation coming from?
        pos.set(values.m30().toDouble(), values.m31().toDouble(), values.m32().toDouble())
        values.getUnnormalizedRotation(rot)
        sca.set(values.getScale(Vector3f()))

        /*synchronized(Unit) {
            println("comparison:")
            println(values)
            println(localTransform)
            println(
                Matrix4d()
                    .translate(pos)
                    .rotate(rot)
                    .scale(sca)
            )
        }*/

    }

    fun distanceSquaredGlobally(v: Vector3d): Double {
        val w = worldTransform
        val x = w.m30() - v.x
        val y = w.m31() - v.y
        val z = w.m32() - v.z
        return x * x + y * y + z * z
    }

    fun dotViewDir(pos2: Vector3d, dir: Vector3d): Double {
        val w = worldTransform
        val x = w.m30() - pos2.x
        val y = w.m31() - pos2.y
        val z = w.m32() - pos2.z
        return dir.dot(x, y, z)
    }

    var needsLocalUpdate = false
    var needsGlobalUpdate = true

    fun invalidateGlobal() {
        needsGlobalUpdate = true
    }

    fun invalidate() {
        needsLocalUpdate = true
    }

}