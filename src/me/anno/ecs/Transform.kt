package me.anno.ecs

import me.anno.gpu.GFX
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.*

class Transform : Saveable() {

    // two transforms could be used to interpolate between draw calls
    var lastUpdateTime = 0L
    var lastDrawTime = 0L
    var lastUpdateDt = 0L

    fun teleportUpdate(time: Long = GFX.gameTime) {
        lastUpdateTime = time
        lastUpdateDt = 1_000_000_000
        drawTransform.set(globalTransform)
    }

    fun update(time: Long = GFX.gameTime) {
        val dt = time - lastUpdateTime
        if (dt > 0) {
            lastUpdateTime = time
            lastUpdateDt = dt
        }
    }

    fun update(time: Long, entity: Entity, calculateMatrices: Boolean) {
        update(time)
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (!child.isPhysicsControlled) {
                if (calculateMatrices) child.transform.calculateGlobalTransform(this)
                child.transform.update(time, child, calculateMatrices)
            }
        }
    }

    fun updateDrawingLerpFactor(time: Long = GFX.gameTime): Double {
        val v = drawDrawingLerpFactor(time)
        lastDrawTime = time
        return v
    }

    fun drawDrawingLerpFactor(time: Long = GFX.gameTime): Double {
        return if (lastUpdateDt <= 0) {
            // hasn't happened -> no interpolation
            drawTransform.set(globalTransform)
            0.0
        } else {
            val drawingDt = (time - lastDrawTime)
            drawingDt.toDouble() / lastUpdateDt
        }
    }

    val globalTransform = Matrix4x3d()
    val drawTransform = Matrix4x3d()

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
    fun update(parent: Transform?, time: Long = GFX.gameTime) {
        if (needsGlobalUpdate) {
            update(time)
            calculateGlobalTransform(parent)
            needsGlobalUpdate = false
        }
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

    var needsGlobalUpdate = true

    fun invalidateGlobal() {
        needsGlobalUpdate = true
    }

    override fun readMatrix4x3d(name: String, value: Matrix4x3d) {
        when (name) {
            "local" -> setLocal(value)
            else -> super.readMatrix4x3d(name, value)
        }
    }

    fun calculateGlobalTransform(parent: Transform?) {
        if (parent == null) {
            globalTransform.set(localTransform)
        } else {
            globalTransform.set(parent.globalTransform)
                .mul(localTransform)
        }
    }

    fun calculateLocalTransform(parent: Transform?) {
        if (parent == null) {
            localTransform.set(globalTransform)
        } else {
            // parent.global * self.local * point = self.global * point
            // parent.global * self.local = self.global
            // self.local = inv(parent.global) * self.global
            // correct???
            localTransform.set(parent.globalTransform).invert().mul(globalTransform)
        }
    }

    fun clone(): Transform {
        val t = Transform()
        t.globalTransform.set(globalTransform)
        t.localTransform.set(localTransform)
        t.pos.set(pos)
        t.rot.set(rot)
        t.sca.set(sca)
        t.invalidateGlobal()
        return t
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // global doesn't need to be saved, as it can be reconstructed
        writer.writeMatrix4x3d("local", localTransform)
    }

    override val className = "ECSTransform"
    override val approxSize: Int = 1

    override fun isDefaultValue(): Boolean = localTransform.properties() == 28 // the value assigned for a unit matrix

}