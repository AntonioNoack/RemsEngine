package me.anno.ecs

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertNotSame
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
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

    // todo it would be nice if we could combine these into one field
    var entity: Entity? = null
    var parentEntity: Entity? = null
    val parent get() = (entity?.parentEntity ?: parentEntity)?.transform

    /** transform relative to center of the world; all transforms combined from root to this node */
    val globalTransform: Matrix4x3d = Matrix4x3d()

    /** smoothly interpolated transform from the previous frame; global */
    private val drawnTransform: Matrix4x3d = Matrix4x3d()

    private val pos = Vector3d()
    private val rot = Quaterniond()
    private val sca = Vector3d(1.0)

    fun teleportUpdate() {
        validate()
        drawnTransform.set(globalTransform)
    }

    fun getLocalTransform(dst: Matrix4x3d): Matrix4x3d {
        return dst.translationRotateScale(pos, rot, sca)
    }

    @Suppress("unused")
    fun getDrawnMatrix(): Matrix4x3d {
        return drawnTransform
    }

    fun getDrawMatrix(): Matrix4x3d {
        return globalTransform
    }

    fun getValidDrawMatrix(): Matrix4x3d {
        validate()
        return getDrawMatrix()
    }

    fun checkTransform(transform: Matrix4x3d) {
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

    var localRotation: Quaterniond
        get() = rot
        set(value) {
            rot.set(value)
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

    fun setLocal(values: Matrix4x3d): Transform {
        val tmp = JomlPools.mat4x3d.borrow()
        setPosRotSca(tmp.set(values), true)
        return this
    }

    fun setLocal(values: Matrix4x3f): Transform {
        val tmp = JomlPools.mat4x3d.borrow()
        return setLocal(tmp.set(values))
    }

    @Suppress("unused")
    fun setLocal(values: Matrix4f): Transform {
        val tmp = JomlPools.mat4x3d.borrow()
        val tmp2 = JomlPools.mat4d.borrow()
        assertNotSame(tmp2, tmp)
        return setLocal(tmp.set(tmp2.set(values))) // could be made more efficient
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
            val localTransform = JomlPools.mat4x3d.borrow()
            // parent.global * self.local * point = self.global * point
            // parent.global * self.local = self.global
            // self.local = inv(parent.global) * self.global
            parent.globalTransform.invert(localTransform).mul(globalTransform)
            setPosRotSca(localTransform, false)
            checkLocalTransform()
        }
    }

    private fun setPosRotSca(localTransform: Matrix4x3d, invalidate: Boolean) {
        localTransform.getTranslation(pos)
        localTransform.getUnnormalizedRotation(rot)
        localTransform.getScale(sca)
        if (invalidate) invalidateGlobal()
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
        return if (parent == null || parent.globalTransform.isIdentity()) {
            // easy
            setLocal(matrix)
        } else {
            // a little more complex
            val inverseParent = JomlPools.mat4x3d.borrow()
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
        writer.writeVector3d("pos", pos)
        writer.writeQuaterniond("rot", rot)
        writer.writeVector3d("sca", sca)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "local" -> setLocal(value as? Matrix4x3d ?: return)
            "pos" -> localPosition = value as? Vector3d ?: return
            "rot" -> localRotation = value as? Quaterniond ?: return
            "sca" -> localScale = value as? Vector3d ?: return
            else -> super.setProperty(name, value)
        }
    }

    override val approxSize get() = 1
}