package com.bulletphysics.linearmath

import cz.advel.stack.Stack
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Transform represents translation and rotation (rigid transform). Scaling and
 * shearing is not supported.
 *
 * You can use local shape scaling or [com.bulletphysics.collision.shapes.UniformScalingShape] for static rescaling
 * of collision objects.
 *
 * @author jezek2
 */
class Transform() {

    constructor(position: Vector3d, rotation: Quaternionf) : this() {
        origin.set(position)
        basis.set(rotation)
    }

    /**
     * Rotation matrix of this Transform.
     */
    @JvmField
    val basis = Matrix3f()

    /**
     * Translation vector of this Transform.
     */
    @JvmField
    val origin = Vector3d()

    fun set(tr: Transform) {
        basis.set(tr.basis)
        origin.set(tr.origin)
    }

    fun set(mat: Matrix3f) {
        basis.set(mat)
        origin.set(0.0, 0.0, 0.0)
    }

    fun transformPosition(src: Vector3d, dst: Vector3d = src): Vector3d {
        src.mul(basis, dst).add(origin)
        return dst
    }

    fun transformPosition(src: Vector3f, dst: Vector3f = src): Vector3f {
        src.mul(basis, dst)
        dst.add(origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat())
        return dst
    }

    fun transformDirection(src: Vector3f, dst: Vector3f = src) {
        src.mul(basis, dst)
    }

    fun transformDirection(src: Vector3f, dst: Vector3d) {
        val tmp = Stack.borrowVec3f()
        src.mul(basis, tmp)
        dst.set(tmp)
    }

    fun setIdentity() {
        basis.identity()
        origin.set(0.0, 0.0, 0.0)
    }

    fun inverse() {
        basis.transpose() // no scale -> transpose = inverse
        origin.negate().mul(basis)
    }

    fun setInverse(tr: Transform) {
        tr.basis.transpose(basis)
        tr.origin.negate(origin).mul(basis)
    }

    fun invXform(inVec: Vector3d, out: Vector3d) {
        inVec.sub(origin, out)
        out.mulTranspose(basis)
    }

    fun mul(tr: Transform) {
        // can only be simplified, if tr !== this
        val vec = Stack.borrowVec3d(tr.origin)
        transformPosition(vec)
        basis.mul(tr.basis)
        origin.set(vec)
    }

    fun setMul(tr1: Transform, tr2: Transform) {
        val vec = Stack.borrowVec3d(tr2.origin)
        tr1.transformPosition(vec)
        tr1.basis.mul(tr2.basis, basis)
        origin.set(vec)
    }

    fun setTranslation(x: Double, y: Double, z: Double) {
        origin.set(x, y, z)
    }

    fun setTranslation(v: Vector3d) {
        origin.set(v)
    }

    fun getRotation(out: Quaternionf): Quaternionf {
        basis.getUnnormalizedRotation(out).normalize()
        return out
    }

    fun setRotation(q: Quaternionf) {
        basis.set(q)
    }

    override fun equals(other: Any?): Boolean {
        return other is Transform &&
                basis == other.basis &&
                origin == other.origin
    }

    override fun hashCode(): Int {
        return basis.hashCode() * 31 + origin.hashCode()
    }

    override fun toString(): String {
        return "($basis + $origin)"
    }
}
