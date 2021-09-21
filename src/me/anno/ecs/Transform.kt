package me.anno.ecs

import me.anno.Engine
import me.anno.gpu.GFX
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.maths.Maths
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
        checkDrawTransform()
    }

    fun checkDrawTransform() {
        checkTransform(drawTransform)
    }

    fun checkTransform(drawTransform: Matrix4x3d) {
        if (!drawTransform.isFinite) {
            Engine.shutdown()
            Thread.sleep(100)
            throw RuntimeException("Transform became invalid")
        }
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

    fun getDrawMatrix(time: Long = GFX.gameTime): Matrix4x3d {
        val drawTransform = drawTransform
        val factor = updateDrawingLerpFactor(time)
        if (factor > 0.0) {
            val extrapolatedTime = (GFX.gameTime - lastUpdateTime).toDouble() / lastUpdateDt
            // needs to be changed, if the extrapolated time changes -> it changes if the phyisics engine is behind
            // its target -> in the physics engine, we send the game time instead of the physics time,
            // and this way, it's relatively guaranteed to be roughly within [0,1]
            val fac2 = factor / (Maths.clamp(1.0 - extrapolatedTime, 0.001, 1.0))
            if (fac2 < 1.0) {
                drawTransform.lerp(globalTransform, fac2)
                checkDrawTransform()
            } else {
                drawTransform.set(globalTransform)
                checkDrawTransform()
            }
        }
        return drawTransform
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
            checkDrawTransform()
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
    private val rot = Quaterniond()
    private val sca = Vector3d(1.0)

    var needsGlobalUpdate = true

    fun invalidateGlobal() {
        needsGlobalUpdate = true
    }

    fun set(src: Transform) {
        lastUpdateTime = src.lastUpdateTime
        lastDrawTime = src.lastDrawTime
        lastUpdateDt = src.lastUpdateDt
        localTransform.set(src.localTransform)
        globalTransform.set(src.globalTransform)
        drawTransform.set(src.drawTransform)
        pos.set(src.pos)
        rot.set(src.rot)
        sca.set(src.sca)
        needsGlobalUpdate = src.needsGlobalUpdate
    }

    var localPosition: Vector3d
        get() = pos
        set(value) {
            pos.set(value)
            localTransform.m30(value.x)
            localTransform.m31(value.y)
            localTransform.m32(value.z)
            invalidateGlobal()
        }

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

    // todo setter
    // todo cache it
    val globalPosition
        get() = globalTransform.getTranslation(Vector3d())


    // todo only update if changed to save resources
    fun update(parent: Transform?, time: Long = GFX.gameTime) {
        if (needsGlobalUpdate) {
            update(time)
            calculateGlobalTransform(parent)
        }
    }

    fun setLocal(values: Matrix4x3d) {
        localTransform.set(values)
        checkTransform(localTransform)
        pos.set(values.m30(), values.m31(), values.m32())
        values.getUnnormalizedRotation(rot)
        values.getScale(sca)
        invalidateGlobal()
    }

    fun setLocal(values: Matrix4x3f) {
        localTransform.set(values)
        checkTransform(localTransform)
        setCachedPosRotSca()
        invalidateGlobal()
    }

    fun setLocal(values: Matrix4f) {
        localTransform.set(
            values.m00().toDouble(), values.m01().toDouble(), values.m02().toDouble(),
            values.m10().toDouble(), values.m11().toDouble(), values.m12().toDouble(),
            values.m20().toDouble(), values.m21().toDouble(), values.m22().toDouble(),
            values.m30().toDouble(), values.m31().toDouble(), values.m32().toDouble(),
        )
        checkTransform(localTransform)
        setCachedPosRotSca()
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

    override fun readMatrix4x3d(name: String, value: Matrix4x3d) {
        when (name) {
            "local" -> setLocal(value)
            else -> super.readMatrix4x3d(name, value)
        }
    }

    fun calculateGlobalTransform(parent: Transform?) {
        checkTransform(localTransform)
        if (parent == null) {
            globalTransform.set(localTransform)
        } else {
            checkTransform(parent.globalTransform)
            globalTransform.set(parent.globalTransform)
                .mul(localTransform)
        }
        checkTransform(globalTransform)
        needsGlobalUpdate = false
    }

    fun calculateLocalTransform(parent: Transform?) {
        val localTransform = localTransform
        if (parent == null) {
            localTransform.set(globalTransform)
            setCachedPosRotSca()
            // invalidateGlobal() not needed, or is it?
        } else {
            // parent.global * self.local * point = self.global * point
            // parent.global * self.local = self.global
            // self.local = inv(parent.global) * self.global
            localTransform.set(parent.globalTransform)
                .invert()
                .mul(globalTransform)
            setCachedPosRotSca()
            checkTransform(localTransform)
        }
    }

    private fun setCachedPosRotSca() {
        val localTransform = localTransform
        pos.set(localTransform.m30(), localTransform.m31(), localTransform.m32())
        localTransform.getUnnormalizedRotation(rot)
        localTransform.getScale(sca)
        invalidateGlobal()
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

    override fun isDefaultValue() = false

}