package me.anno.ecs

import me.anno.Engine
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import org.joml.*

class Transform() : Saveable() {

    constructor(entity: Entity) : this() {
        this.entity = entity
    }

    enum class State {
        VALID, CHILDREN_NEED_UPDATE, VALID_LOCAL, VALID_GLOBAL
    }

    private var state = State.VALID
        set(value) {
            field = value
            if (field != State.VALID) {
                parent?.invalidateForChildren()
            }
        }

    fun invalidateForChildren() {
        if (state == State.VALID) state = State.CHILDREN_NEED_UPDATE
        else parent?.invalidateForChildren() // keep behaviour linear
    }

    val needsUpdate get() = state != State.VALID

    var entity: Entity? = null
    val parent get() = entity?.parentEntity?.transform

    // two transforms could be used to interpolate between draw calls
    var lastUpdateTime = 0L
    var lastDrawTime = 0L
    var lastUpdateDt = 0L

    /** transform relative to center of the world; all transforms combined from root to this node */
    val globalTransform = Matrix4x3d()

    /** transform relative to parent */
    val localTransform = Matrix4x3d()

    /** smoothly interpolated transform; global */
    private val drawTransform = Matrix4x3d()

    /** smoothly interpolated transform from the previous frame; global */
    private val drawnTransform = Matrix4x3d()

    private val pos = Vector3d()
    private val rot = Quaterniond()
    private val sca = Vector3d(1.0)

    fun teleportUpdate(time: Long = Engine.gameTime) {
        lastUpdateTime = time
        lastUpdateDt = 1_000_000_000
        drawTransform.set(globalTransform)
        checkDrawTransform()
    }

    @Suppress("unused")
    fun getDrawnMatrix(time: Long = Engine.gameTime): Matrix4x3d {
        getDrawMatrix(time) // update matrices
        return drawnTransform
    }

    fun getDrawMatrix(time: Long = Engine.gameTime): Matrix4x3d {
        val drawTransform = drawTransform
        val factor = updateDrawingLerpFactor(time)
        if (factor > 0.0) {
            val extrapolatedTime = (Engine.gameTime - lastUpdateTime).toDouble() / lastUpdateDt
            // needs to be changed, if the extrapolated time changes -> it changes if the phyisics engine is behind
            // its target -> in the physics engine, we send the game time instead of the physics time,
            // and this way, it's relatively guaranteed to be roughly within [0,1]
            val fac2 = factor / (Maths.clamp(1.0 - extrapolatedTime, 0.001, 1.0))
            if (fac2 < 1.0) {
                drawnTransform.set(drawTransform)
                drawTransform.lerp(globalTransform, fac2)
                checkDrawTransform()
            } else {
                drawnTransform.set(drawTransform)
                drawTransform.set(globalTransform)
                checkDrawTransform()
            }
        }
        return drawTransform
    }

    private fun checkDrawTransform() {
        checkTransform(drawTransform)
    }

    fun checkTransform(drawTransform: Matrix4x3d) {
        if (!drawTransform.isFinite) {
            Engine.requestShutdown()
            Thread.sleep(100)
            throw RuntimeException("Transform became invalid")
        }
    }

    fun smoothUpdate(time: Long = Engine.gameTime) {
        val dt = time - lastUpdateTime
        if (dt > 0) {
            lastUpdateTime = time
            lastUpdateDt = dt
        }
    }

    fun setStateAndUpdate(state: State, time: Long = Engine.gameTime) {
        this.state = state
        smoothUpdate(time)
    }

    private fun updateDrawingLerpFactor(time: Long = Engine.gameTime): Double {
        val v = calculateDrawingLerpFactor(time)
        lastDrawTime = time
        return v
    }

    private fun calculateDrawingLerpFactor(time: Long = Engine.gameTime): Double {
        return if (lastUpdateDt <= 0) {
            // hasn't happened -> no interpolation
            drawTransform.set(globalTransform)
            drawnTransform.set(globalTransform)
            checkDrawTransform()
            0.0
        } else {
            val drawingDt = (time - lastDrawTime)
            drawingDt.toDouble() / lastUpdateDt
        }
    }

    fun invalidateLocal() {
        state = State.VALID_GLOBAL
    }

    fun invalidateGlobal() {
        state = State.VALID_LOCAL
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
        state = src.state
    }

    var localPosition: Vector3d
        get() = pos
        set(value) {
            pos.set(value)
            localTransform.setTranslation(value)
            invalidateGlobal()
        }

    var localRotation: Quaterniond
        get() = rot
        set(value) {
            rot.set(value)
            localTransform.translation(pos).rotate(value).scale(sca)
            invalidateGlobal()
        }

    @Suppress("unused")
    fun setLocalEulerAngle(x: Double, y: Double, z: Double) {
        localRotation.set(Quaterniond().rotateY(y).rotateX(x).rotateZ(z))
    }

    var localScale: Vector3d
        get() = sca
        set(value) {
            sca.set(value)
            localTransform.translation(pos).rotate(rot).scale(value)
            invalidateGlobal()
        }

    /**
     * WARNING: setting this does not work together with setGlobalRotation/Scale()
     * */
    var globalPosition: Vector3d
        get() = globalTransform.getTranslation(JomlPools.vec3d.create())
        set(value) {
            globalTransform.setTranslation(value)
            invalidateLocal()
        }

    /**
     * WARNING: setting this does not work together with setGlobalPosition()
     * */
    var globalRotation: Quaterniond
        get() = globalTransform.getUnnormalizedRotation(JomlPools.quat4d.create())
        set(value) {
            // todo test this
            // we have no correct, direct control over globalRotation,
            // so we use tricks, and compute an ideal local rotation instead
            val parent = parent
            localRotation = if (parent != null) {
                val m = Quaterniond()
                m.set(parent.globalRotation)
                m.invert() // now the rotation is like an inversion to the parent
                m.mul(value) // then apply this afterwards
                m
            } else {
                // local = global
                value
            }
        }

    /**
     * WARNING: setting this does not work together with setGlobalPosition()
     * */
    var globalScale: Vector3d
        get() = globalTransform.getScale(JomlPools.vec3d.create())
        set(value) {
            // todo test this
            // we have no correct, direct control over globalScale,
            // so we use tricks, and compute an ideal local scale instead
            val parent = parent
            localScale = if (parent != null) {
                val m = parent.globalScale // returns a "copy"
                m.set(1.0 / m.x, 1.0 / m.y, 1.0 / m.z) // invert
                // we need to correct for the local rotation
                // might be correct, am very unsure...
                localRotation.transformInverse(m)
                m.mul(value) // then apply this afterwards
                m
            } else {
                // local = global
                value
            }
        }

    fun setGlobalRotation(yxz: Vector3d) {
        val tmp = JomlPools.quat4d.create()
        tmp.rotateYXZ(yxz.y, yxz.x, yxz.z)
        globalRotation = tmp
        JomlPools.quat4d.sub(1)
    }

    fun validate() {
        val parent = entity?.parentEntity?.transform
        when (state) {
            // really update by time? idk... this is not the time when it was changed...
            // it kind of is, when we call updateTransform() every frame
            State.VALID_LOCAL -> {
                calculateGlobalTransform(parent)
                smoothUpdate()
            }
            State.VALID_GLOBAL -> {
                calculateLocalTransform(parent)
                smoothUpdate()
            }
            else -> {
            }
        }
        state = State.VALID
    }

    fun setLocal(values: Matrix4x3d) {
        localTransform.set(values)
        checkTransform(localTransform)
        values.getTranslation(pos)
        values.getUnnormalizedRotation(rot)
        values.getScale(sca)
        state = State.VALID_LOCAL
    }

    fun setLocal(values: Matrix4x3f) {
        localTransform.set(values)
        checkTransform(localTransform)
        setCachedPosRotSca()
        state = State.VALID_LOCAL
    }

    @Suppress("unused")
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

    @Suppress("unused")
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

    private fun calculateGlobalTransform(parent: Transform?) {
        checkTransform(localTransform)
        if (parent == null) {
            globalTransform.set(localTransform)
        } else {
            checkTransform(parent.globalTransform)
            globalTransform.set(parent.globalTransform).mul(localTransform)
        }
        checkTransform(globalTransform)
    }

    private fun calculateLocalTransform(parent: Transform?) {
        val localTransform = localTransform
        if (parent == null) {
            localTransform.set(globalTransform)
            setCachedPosRotSca()
            // invalidateGlobal() not needed, or is it?
        } else {
            // parent.global * self.local * point = self.global * point
            // parent.global * self.local = self.global
            // self.local = inv(parent.global) * self.global
            localTransform.set(parent.globalTransform).invert().mul(globalTransform)
            setCachedPosRotSca()
            checkTransform(localTransform)
        }
    }

    private fun setCachedPosRotSca() {
        val localTransform = localTransform
        localTransform.getTranslation(pos)
        localTransform.getUnnormalizedRotation(rot)
        localTransform.getScale(sca)
        invalidateGlobal()
    }

    @Suppress("unused")
    fun setLocalPosition(v: Vector3dc): Transform {
        return setLocalPosition(v.x(), v.y(), v.z())
    }

    @Suppress("unused")
    fun setLocalPosition(x: Double, y: Double, z: Double): Transform {
        localPosition.set(x, y, z)
        invalidateGlobal()
        return this
    }

    @Suppress("unused")
    fun setGlobalPosition(v: Vector3dc): Transform {
        return setGlobalPosition(v.x(), v.y(), v.z())
    }

    @Suppress("unused")
    fun setGlobalPosition(x: Double, y: Double, z: Double): Transform {
        globalPosition.set(x, y, z)
        invalidateLocal()
        return this
    }

    @Suppress("unused")
    fun translateLocal(dx: Double, dy: Double, dz: Double): Transform {
        localPosition.add(dx, dy, dz)
        invalidateGlobal()
        return this
    }

    @Suppress("unused")
    fun translateGlobal(dx: Double, dy: Double, dz: Double): Transform {
        globalPosition.add(dx, dy, dz)
        invalidateLocal()
        return this
    }

    @Suppress("unused")
    fun resetLocalRotation(): Transform {
        localRotation = localRotation.identity()
        return this
    }

    fun rotateXLocal(angleRadians: Double): Transform {
        localRotation = localRotation.rotateX(angleRadians)
        return this
    }

    fun rotateYLocal(angleRadians: Double): Transform {
        localRotation = localRotation.rotateY(angleRadians)
        return this
    }

    @Suppress("unused")
    fun rotateZLocal(angleRadians: Double): Transform {
        localRotation = localRotation.rotateZ(angleRadians)
        return this
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