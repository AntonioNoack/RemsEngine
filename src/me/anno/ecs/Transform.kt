package me.anno.ecs

import me.anno.Time
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * represents position, rotation and scale of an Entity,
 * including lerping, and relative to its parent
 * */
class Transform() : Saveable() {

    companion object {
        private val LOGGER = LogManager.getLogger(Transform::class)
    }

    constructor(entity: Entity?) : this() {
        this.entity = entity
    }

    enum class State {
        VALID, CHILDREN_NEED_UPDATE, VALID_LOCAL, VALID_GLOBAL
    }

    var state: State = State.VALID
        private set(value) {
            field = value
            if (field != State.VALID) {
                parent?.invalidateForChildren()
            }
        }

    fun invalidateForChildren() {
        if (state == State.VALID) state = State.CHILDREN_NEED_UPDATE
        else parent?.invalidateForChildren() // keep behaviour linear
    }

    var entity: Entity? = null
    var parentEntity: Entity? = null
    val parent get() = (entity?.parentEntity ?: parentEntity)?.transform

    var lastUpdateFrameIndex = 0
    var lastUpdateTime: Long = 0L
    var lastDrawTime: Long = 0L
    var lastUpdateDt: Long = 0L

    /** transform relative to center of the world; all transforms combined from root to this node */
    val globalTransform: Matrix4x3d = Matrix4x3d()

    /** transform relative to parent */
    val localTransform: Matrix4x3d = Matrix4x3d()

    /** smoothly interpolated transform; global */
    private val drawTransform: Matrix4x3d = Matrix4x3d()

    /** smoothly interpolated transform from the previous frame; global */
    private val drawnTransform: Matrix4x3d = Matrix4x3d()

    private val pos = Vector3d()
    private val rot = Quaterniond()
    private val sca = Vector3d(1.0)

    private val prepos = Vector3d()
    private val prerot = Quaterniond()
    private val presca = Vector3d(1.0)

    fun teleportUpdate(time: Long = Time.gameTimeN) {
        validate()
        lastUpdateFrameIndex = Time.frameIndex
        lastUpdateTime = time
        lastUpdateDt = 0L
        drawTransform.set(globalTransform)
        drawnTransform.set(globalTransform)
    }

    @Suppress("unused")
    fun getDrawnMatrix(time: Long = Time.gameTimeN): Matrix4x3d {
        getDrawMatrix(time) // update matrices
        return drawnTransform
    }

    fun getDrawMatrix(time: Long = Time.gameTimeN): Matrix4x3d {
        val factor = updateDrawingLerpFactor(time)
        if (factor > 0f) {
            val udt = Time.gameTimeN - lastUpdateTime
            if (udt < lastUpdateDt) {
                val extrapolatedTime = 1f - udt.toFloat() / lastUpdateDt
                // needs to be changed, if the extrapolated time changes -> it changes if the physics engine is behind
                // its target -> in the physics engine, we send the game time instead of the physics time,
                // and this way, it's relatively guaranteed to be roughly within [0,1]
                if (factor < extrapolatedTime) {
                    drawnTransform.set(drawTransform)
                    val f = (factor / extrapolatedTime).toDouble()
                    prepos.mix(pos, f)
                    prerot.slerp(rot, f)
                    presca.mix(sca, f)
                    val parent = parent
                    if (parent != null) drawTransform.set(parent.getDrawnMatrix(time))
                    else drawTransform.identity()
                    drawTransform
                        .translate(prepos)
                        .rotate(prerot)
                        .scale(presca)
                    // checkDrawTransform()
                } else teleportVisuals()
            } else if (udt < 3 * lastUpdateDt) {
                // transform is coming to rest:
                // 3x longer than usual, we got no update
                teleportVisuals()
            } else {
                // transform is stationary
                lastUpdateDt = 0L
                teleportVisuals()
            }
        }
        return drawTransform
    }

    fun getValidDrawMatrix(): Matrix4x3d {
        validate()
        return getDrawMatrix()
    }

    private fun teleportVisuals() {
        drawnTransform.set(drawTransform)
        drawTransform.set(globalTransform)
        prepos.set(pos)
        prerot.set(rot)
        presca.set(sca)
    }

    private var needsStaticUpdate = true

    fun checkTransform(transform: Matrix4x3d) {
        if (!transform.isFinite) {
            LOGGER.error("Transform became invalid: $transform")
            transform.identity()
        }
    }

    fun smoothUpdate(time: Long = Time.gameTimeN) {
        needsStaticUpdate = true
        val dt = time - lastUpdateTime
        if (dt > 0) {
            lastUpdateFrameIndex = Time.frameIndex
            lastUpdateTime = time
            lastUpdateDt = dt
        }
    }

    fun setStateAndUpdate(state: State, time: Long = Time.gameTimeN) {
        this.state = state
        smoothUpdate(time)
    }

    private fun updateDrawingLerpFactor(time: Long = Time.gameTimeN): Float {
        val v = calculateDrawingLerpFactor(time)
        lastDrawTime = time
        return v
    }

    private fun calculateDrawingLerpFactor(time: Long = Time.gameTimeN): Float {
        return if (lastUpdateDt <= 0) {
            if (needsStaticUpdate) {
                // hasn't happened -> no interpolation
                drawTransform.set(globalTransform)
                drawnTransform.set(globalTransform)
                needsStaticUpdate = false
            }
            0f
        } else {
            val drawingDt = (time - lastDrawTime)
            drawingDt.toFloat() / lastUpdateDt
        }
    }

    fun invalidateLocal() {
        if (state == State.VALID_LOCAL) {
            LOGGER.warn("Invalidating local -> global")
        }
        state = State.VALID_GLOBAL
        needsStaticUpdate = true
    }

    fun invalidateGlobal() {
        if (state == State.VALID_GLOBAL) {
            LOGGER.warn("Invalidating global -> local")
        }
        state = State.VALID_LOCAL
        needsStaticUpdate = true
    }

    fun set(src: Transform) {
        src.validate()
        lastUpdateTime = src.lastUpdateTime
        lastDrawTime = src.lastDrawTime
        lastUpdateDt = src.lastUpdateDt
        localPosition.set(src.localPosition)
        localRotation.set(src.localRotation)
        localScale.set(localScale)
        localTransform.set(src.localTransform)
        globalTransform.set(src.globalTransform)
        drawTransform.set(src.drawTransform)
        pos.set(src.pos)
        rot.set(src.rot)
        sca.set(src.sca)
        invalidateGlobal()
        parent?.invalidateForChildren()
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
            localTransform.translationRotateScale(pos, rot, sca)
            invalidateGlobal()
        }

    @Suppress("unused")
    fun setLocalEulerAngle(x: Double, y: Double, z: Double): Transform {
        localRotation = localRotation.rotateY(y).rotateX(x).rotateZ(z)
        return this
    }

    fun setOffsetForLocalRotation(rotation: Quaterniond, center: Vector3d): Transform {
        localRotation = localRotation.identity()
            .mul(rotation)
        localPosition = localPosition
            .set(-center.x, -center.y, -center.z)
            .rotate(rotation)
            .add(center)
        return this
    }

    var localScale: Vector3d
        get() = sca
        set(value) {
            sca.set(value)
            localTransform.translationRotateScale(pos, rot, sca)
            invalidateGlobal()
        }

    /**
     * WARNING: setting this does not work together with setGlobalRotation/Scale().
     * Call validateTransform() in-between to make them work.
     * */
    var globalPosition: Vector3d
        get() = globalTransform.getTranslation(JomlPools.vec3d.create())
        set(value) {
            globalTransform.setTranslation(value)
            invalidateLocal()
        }

    /**
     * WARNING: setting this does not work together with setGlobalPosition().
     * Call validateTransform() in-between to make them work.
     * */
    var globalRotation: Quaterniond
        get() = globalTransform.getUnnormalizedRotation(JomlPools.quat4d.create())
        set(value) {
            // we have no correct, direct control over globalRotation,
            // so we use tricks, and compute an ideal local rotation instead
            val parent = parent
            if (parent != null) {
                // now the rotation is like an inversion to the parent
                val parentInv = parent.globalRotation.invert() // value is on JomlPool-stack
                localRotation = parentInv.mul(value) // then apply this afterward
                JomlPools.quat4d.sub(1) // return value on stack
            } else {
                // local = global
                localRotation = value
            }
        }

    /**
     * WARNING: setting this does not work together with setGlobalPosition().
     * Call validateTransform() in-between to make them work;
     *
     * Only works well if object isn't rotated, or you set a uniform scale.
     * */
    var globalScale: Vector3d
        get() = globalTransform.getScale(JomlPools.vec3d.create())
        set(value) {
            // we have no correct, direct control over globalScale,
            // so we use tricks, and compute an ideal local scale instead
            val parent = parent
            localScale = if (parent != null) {
                val m = parent.globalScale // returns a stack-allocated vector
                m.set(1.0 / m.x, 1.0 / m.y, 1.0 / m.z)
                // todo rotate, if possible
                //  only truly possible if localRotation is k * 90°s
                m.mul(value)
                m
            } else value
        }

    fun validate() {
        when (state) {
            // really update by time? idk... this is not the time when it was changed...
            // it kind of is, when we call updateTransform() every frame
            State.VALID_LOCAL -> {
                calculateGlobalTransform(parent)
                smoothUpdate()
                state = State.VALID
            }
            State.VALID_GLOBAL -> {
                calculateLocalTransform(parent)
                smoothUpdate()
                state = State.VALID
            }
            else -> {
                state = State.VALID
            }
        }
    }

    fun setLocal(values: Matrix4x3d): Transform {
        localTransform.set(values)
        checkTransform(localTransform)
        values.getTranslation(pos)
        values.getUnnormalizedRotation(rot)
        values.getScale(sca)
        state = State.VALID_LOCAL
        return this
    }

    fun setLocal(values: Matrix4x3f): Transform {
        localTransform.set(values)
        checkTransform(localTransform)
        setCachedPosRotSca()
        state = State.VALID_LOCAL
        return this
    }

    @Suppress("unused")
    fun setLocal(values: Matrix4f): Transform {
        localTransform.set(
            values.m00.toDouble(), values.m01.toDouble(), values.m02.toDouble(),
            values.m10.toDouble(), values.m11.toDouble(), values.m12.toDouble(),
            values.m20.toDouble(), values.m21.toDouble(), values.m22.toDouble(),
            values.m30.toDouble(), values.m31.toDouble(), values.m32.toDouble(),
        )
        checkTransform(localTransform)
        setCachedPosRotSca()
        invalidateGlobal()
        return this
    }

    fun distanceSquaredGlobally(v: Vector3d): Double {
        val w = globalTransform
        val x = w.m30 - v.x
        val y = w.m31 - v.y
        val z = w.m32 - v.z
        return x * x + y * y + z * z
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
        state = State.VALID
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
    fun setLocalPosition(x: Double, y: Double, z: Double): Transform {
        localPosition = localPosition.set(x, y, z)
        return this
    }

    @Suppress("unused")
    fun setLocalScale(scale: Double): Transform {
        localScale = localScale.set(scale)
        return this
    }

    @Suppress("unused")
    fun setLocalScale(sx: Double, sy: Double, sz: Double): Transform {
        localScale = localScale.set(sx, sy, sz)
        return this
    }

    @Suppress("unused")
    fun setGlobalPosition(x: Double, y: Double, z: Double): Transform {
        globalPosition = globalPosition.set(x, y, z)
        return this
    }

    fun setGlobal(matrix: Matrix4x3d): Transform {
        val parent = parent
        if (parent == null) {
            // easy
            setLocal(matrix)
        } else {
            // a little more complex
            val tmp = localTransform // overridden anyway
            parent.globalTransform.invert(tmp)
            tmp.mul(matrix)
            setLocal(tmp)
        }
        return this
    }

    @Suppress("unused")
    fun translateLocal(dx: Double, dy: Double, dz: Double): Transform {
        localPosition = localPosition.add(dx, dy, dz)
        return this
    }

    @Suppress("unused")
    fun translateGlobal(dx: Double, dy: Double, dz: Double): Transform {
        globalPosition = globalPosition.add(dx, dy, dz)
        return this
    }

    fun rotateLocalX(angleRadians: Double): Transform {
        localRotation = localRotation.rotateX(angleRadians)
        return this
    }

    fun rotateLocalY(angleRadians: Double): Transform {
        localRotation = localRotation.rotateY(angleRadians)
        return this
    }

    @Suppress("unused")
    fun rotateLocalZ(angleRadians: Double): Transform {
        localRotation = localRotation.rotateZ(angleRadians)
        return this
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // global doesn't need to be saved, because it can be reconstructed
        writer.writeMatrix4x3d("local", localTransform)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "local" -> setLocal(value as? Matrix4x3d ?: return)
            else -> super.setProperty(name, value)
        }
    }

    override val approxSize get() = 1
}