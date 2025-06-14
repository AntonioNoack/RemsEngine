package me.anno.ecs

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

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
        VALID,
        CHILDREN_NEED_UPDATE,
        VALID_LOCAL,
        VALID_GLOBAL,
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

    /** transform relative to center of the world; all transforms combined from root to this node */
    val globalTransform: Matrix4x3 = Matrix4x3()

    /** smoothly interpolated transform from the previous frame; global */
    private val drawnTransform: Matrix4x3 = Matrix4x3()

    private val pos = Vector3d()
    private val rot = Quaternionf()
    private val sca = Vector3f(1f)

    fun teleportUpdate() {
        validate()
        drawnTransform.set(globalTransform)
    }

    fun getLocalTransform(dst: Matrix4x3): Matrix4x3 {
        return dst.translationRotateScale(pos, rot, sca)
    }

    @Suppress("unused")
    fun getDrawnMatrix(): Matrix4x3 {
        return drawnTransform
    }

    fun getDrawMatrix(): Matrix4x3 {
        return globalTransform
    }

    fun getValidDrawMatrix(): Matrix4x3 {
        validate()
        return getDrawMatrix()
    }

    fun checkTransform(transform: Matrix4x3) {
        if (!transform.isFinite) {
            LOGGER.error("Transform became invalid: $transform")
            transform.identity()
        }
    }

    fun checkLocalTransform() {
        if (!pos.isFinite) {
            LOGGER.error("Position became invalid: $pos")
            pos.set(0.0)
        }
        if (!rot.isFinite) {
            LOGGER.error("Rotation became invalid: $rot")
            rot.identity()
        }
        if (!sca.isFinite) {
            LOGGER.error("Scale became invalid: $sca")
            sca.set(1.0)
        }
    }

    fun setStateAndUpdate(state: State) {
        this.state = state
    }

    fun invalidateLocal() {
        if (state == State.VALID_LOCAL) {
            LOGGER.warn("Invalidating local -> global")
        }
        state = State.VALID_GLOBAL
    }

    fun invalidateGlobal() {
        if (state == State.VALID_GLOBAL) {
            LOGGER.warn("Invalidating global -> local")
        }
        state = State.VALID_LOCAL
    }

    fun set(src: Transform) {
        src.validate()
        localPosition.set(src.localPosition)
        localRotation.set(src.localRotation)
        localScale.set(localScale)
        globalTransform.set(src.globalTransform)
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
            invalidateGlobal()
        }

    var localRotation: Quaternionf
        get() = rot
        set(value) {
            rot.set(value)
            invalidateGlobal()
        }

    @Suppress("unused")
    fun setLocalEulerAngle(x: Float, y: Float, z: Float): Transform {
        localRotation = localRotation.rotateY(y).rotateX(x).rotateZ(z)
        return this
    }

    fun setOffsetForLocalRotation(rotation: Quaternionf, center: Vector3d): Transform {
        localRotation = localRotation.identity()
            .mul(rotation)
        localPosition = localPosition
            .set(-center.x, -center.y, -center.z)
            .rotate(rotation)
            .add(center)
        return this
    }

    var localScale: Vector3f
        get() = sca
        set(value) {
            sca.set(value)
            invalidateGlobal()
        }

    /**
     * WARNING: setting this does not work together with setGlobalRotation/Scale().
     * Call validateTransform() in-between to make them work.
     * */
    var globalPosition: Vector3d
        get() = getGlobalPosition(JomlPools.vec3d.create())
        set(value) {
            setGlobalPosition(value)
        }

    /**
     * WARNING: setting this does not work together with setGlobalPosition().
     * Call validateTransform() in-between to make them work.
     * */
    var globalRotation: Quaternionf
        get() = getGlobalRotation(JomlPools.quat4f.create())
        set(value) {
            setGlobalRotation(value)
        }

    /**
     * WARNING: setting this does not work together with setGlobalPosition().
     * Call validateTransform() in-between to make them work;
     *
     * Only works well if object isn't rotated, or you set a uniform scale.
     * */
    var globalScale: Vector3f
        get() = getGlobalScale(JomlPools.vec3f.create())
        set(value) {
            setGlobalScale(value)
        }

    fun getGlobalPosition(dst: Vector3d): Vector3d {
        return globalTransform.getTranslation(dst)
    }

    fun getGlobalRotation(dst: Quaternionf): Quaternionf {
        return globalTransform.getUnnormalizedRotation(dst)
    }

    fun getGlobalScale(dst: Vector3f): Vector3f {
        return globalTransform.getScale(dst)
    }

    /**
     * WARNING: does not work together with setGlobalRotation/Scale().
     * Call validateTransform() in-between to make them work.
     * */
    fun setGlobalPosition(value: Vector3d): Transform {
        globalTransform.setTranslation(value)
        invalidateLocal()
        return this
    }

    /**
     * WARNING: does not work together with setGlobalPosition().
     * Call validateTransform() in-between to make them work.
     * */
    fun setGlobalRotation(value: Quaternionf): Transform {
        // we have no correct, direct control over globalRotation,
        // so we use tricks, and compute an ideal local rotation instead
        val parent = parent
        if (parent != null) {
            // now the rotation is like an inversion to the parent
            val parentInv = parent.globalRotation.invert() // value is on JomlPool-stack
            localRotation = parentInv.mul(value) // then apply this afterward
            JomlPools.quat4f.sub(1) // return value on stack
        } else {
            // local = global
            localRotation = value
        }
        return this
    }

    /**
     * WARNING: does not work together with setGlobalPosition().
     * Call validateTransform() in-between to make them work;
     *
     * Only works well if object isn't rotated, or you set a uniform scale.
     * */
    fun setGlobalScale(value: Vector3f): Transform {
        // we have no correct, direct control over globalScale,
        // so we use tricks, and compute an ideal local scale instead
        val parent = parent
        if (parent == null) localScale = value
        else {
            val tmp = parent.getGlobalScale(JomlPools.vec3f.create())
            // todo rotate, if possible
            //  only truly possible if localRotation is k * 90°s
            localScale = value.div(tmp, tmp)
            JomlPools.vec3f.sub(1)
        }
        return this
    }

    fun validate() {
        when (state) {
            // really update by time? idk... this is not the time when it was changed...
            // it kind of is, when we call updateTransform() every frame
            State.VALID_LOCAL -> {
                calculateGlobalTransform(parent)
                state = State.VALID
            }
            State.VALID_GLOBAL -> {
                calculateLocalTransform(parent)
                state = State.VALID
            }
            else -> {
                state = State.VALID
            }
        }
    }

    fun setLocal(values: Transform): Transform {
        localPosition = values.localPosition
        localRotation = values.localRotation
        localScale = values.localScale
        return this
    }

    fun setLocal(values: Matrix4x3): Transform {
        setPosRotSca(values, true)
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
        checkLocalTransform()
        globalTransform.translationRotateScale(pos, rot, sca)
        if (parent != null && !parent.globalTransform.isIdentity()) {
            parent.globalTransform.mul(globalTransform, dst = globalTransform)
        }
        checkTransform(globalTransform)
    }

    private fun calculateLocalTransform(parent: Transform?) {
        state = State.VALID
        if (parent == null || parent.globalTransform.isIdentity()) {
            setPosRotSca(globalTransform, false)
        } else {
            val localTransform = JomlPools.mat4x3m.borrow()
            // parent.global * self.local * point = self.global * point
            // parent.global * self.local = self.global
            // self.local = inv(parent.global) * self.global
            parent.globalTransform.invert(localTransform).mul(globalTransform)
            setPosRotSca(localTransform, false)
            checkLocalTransform()
        }
    }

    private fun setPosRotSca(localTransform: Matrix4x3, invalidate: Boolean) {
        localTransform.getTranslation(pos)
        localTransform.getUnnormalizedRotation(rot)
        localTransform.getScale(sca)
        if (invalidate) invalidateGlobal()
    }

    fun setLocalPosition(x: Float, y: Float, z: Float): Transform {
        return setLocalPosition(x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun setLocalPosition(x: Double, y: Double, z: Double): Transform {
        localPosition = localPosition.set(x, y, z)
        return this
    }

    fun setLocalPosition(v: Vector3f): Transform {
        localPosition = localPosition.set(v)
        return this
    }

    fun setLocalRotation(q: Quaternionf): Transform {
        localRotation = localRotation.set(q)
        return this
    }

    @Suppress("unused")
    fun setLocalScale(scale: Float): Transform {
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

    fun setGlobal(matrix: Matrix4x3): Transform {
        val parent = parent
        return if (parent == null || parent.globalTransform.isIdentity()) {
            // easy
            setLocal(matrix)
        } else {
            // a little more complex
            val inverseParent = JomlPools.mat4x3m.borrow()
            parent.globalTransform.invert(inverseParent).mul(matrix)
            setLocal(inverseParent)
        }
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

    fun rotateLocalX(angleRadians: Float): Transform {
        localRotation = localRotation.rotateX(angleRadians)
        return this
    }

    fun rotateLocalY(angleRadians: Float): Transform {
        localRotation = localRotation.rotateY(angleRadians)
        return this
    }

    @Suppress("unused")
    fun rotateLocalZ(angleRadians: Float): Transform {
        localRotation = localRotation.rotateZ(angleRadians)
        return this
    }

    @Suppress("unused")
    fun getGlobalScaleX(): Float {
        val tmp = JomlPools.vec3f.borrow()
        return globalTransform.getScale(tmp).x
    }

    @Suppress("unused")
    fun getGlobalScaleY(): Float {
        val tmp = JomlPools.vec3f.borrow()
        return globalTransform.getScale(tmp).y
    }

    fun getGlobalScaleZ(): Float {
        val tmp = JomlPools.vec3f.borrow()
        return globalTransform.getScale(tmp).z
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // global doesn't need to be saved, because it can be reconstructed
        writer.writeVector3d("pos", pos)
        writer.writeQuaternionf("rot", rot)
        writer.writeVector3f("sca", sca)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "local" -> setLocal(value as? Matrix4x3 ?: return)
            "pos" -> localPosition = value as? Vector3d ?: return
            "rot" -> when (value) {
                is Quaternionf -> localRotation.set(value)
                is Quaterniond -> localRotation.set(value)
            }
            "sca" -> when (value) {
                is Vector3f -> localScale.set(value)
                is Vector3d -> localScale.set(value)
            }
            else -> super.setProperty(name, value)
        }
    }

    fun getLocalForward(sign: Float, dst: Vector3f): Vector3f {
        return localRotation.transform(0f, 0f, sign, dst)
    }

    override val approxSize get() = 1
}