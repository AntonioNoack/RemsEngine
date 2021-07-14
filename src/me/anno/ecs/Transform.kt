package me.anno.ecs

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.*

class Transform : Saveable() {

    // two transforms could be used to interpolate between draw calls
    var time = 0.0

    val globalTransform = Matrix4x3d()
    val localTransform = Matrix4x3d()

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

    fun setLocalEulerAngle(x: Double, y: Double, z: Double) {
        localRotation.set(Quaterniond().rotateY(y).rotateX(x).rotateZ(z))
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
            globalTransform.identity()
        } else {
            globalTransform.set(parent.globalTransform)
        }

        globalTransform.mul(localTransform)

    }

    fun setLocal(values: Matrix4x3d) {
        localTransform.set(values)
        pos.set(values.m30(), values.m31(), values.m32())
        values.getUnnormalizedRotation(rot)
        values.getScale(sca)
        invalidateGlobal()
    }

    fun setLocal(values: Matrix4f) {
        localTransform.set(
            values.m00().toDouble(), values.m01().toDouble(), values.m02().toDouble(),
            values.m10().toDouble(), values.m11().toDouble(), values.m12().toDouble(),
            values.m20().toDouble(), values.m21().toDouble(), values.m22().toDouble(),
            values.m30().toDouble(), values.m31().toDouble(), values.m32().toDouble(),
        )
        pos.set(values.m30().toDouble(), values.m31().toDouble(), values.m32().toDouble())
        values.getUnnormalizedRotation(rot)
        sca.set(values.getScale(Vector3f()))
        invalidateGlobal()
    }

    fun distanceSquaredGlobally(v: Vector3d): Double {
        val w = globalTransform
        val x = w.m30() - v.x
        val y = w.m31() - v.y
        val z = w.m32() - v.z
        return x * x + y * y + z * z
    }

    fun dotViewDir(pos2: Vector3d, dir: Vector3d): Double {
        val w = globalTransform
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

    fun invalidateLocal() {
        needsLocalUpdate = true
    }

    override fun readMatrix4x3d(name: String, value: Matrix4x3d) {
        when (name) {
            "local" -> setLocal(value)
            else -> super.readMatrix4x3d(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "time" -> time = value
            else -> super.readDouble(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // global doesn't need to be saved, as it can be reconstructed
        writer.writeMatrix4x3d("local", localTransform)
        writer.writeDouble("time", time)
    }

    override val className = "ECSTransform"
    override val approxSize: Int = 1

    override fun isDefaultValue(): Boolean = localTransform.properties() == 28 // the value assigned for a unit matrix

}