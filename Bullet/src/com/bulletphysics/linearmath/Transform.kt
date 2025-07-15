package com.bulletphysics.linearmath

import cz.advel.stack.Stack
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * Transform represents translation and rotation (rigid transform). Scaling and
 * shearing is not supported.
 *
 * You can use local shape scaling or [com.bulletphysics.collision.shapes.UniformScalingShape] for static rescaling
 * of collision objects.
 *
 * @author jezek2
 */
class Transform {

    /**
     * Rotation matrix of this Transform.
     */
    @JvmField
    val basis = Matrix3d()

    /**
     * Translation vector of this Transform.
     */
    @JvmField
    val origin = Vector3d()

    fun set(tr: Transform) {
        basis.set(tr.basis)
        origin.set(tr.origin)
    }

    fun set(mat: Matrix3d) {
        basis.set(mat)
        origin.set(0.0, 0.0, 0.0)
    }

    fun transformPosition(src: Vector3d, dst: Vector3d = src): Vector3d {
        basis.transform(src, dst).add(origin)
        return dst
    }

    fun transformDirection(src: Vector3d, dst: Vector3d = src) {
        basis.transform(src, dst)
    }

    fun setIdentity() {
        basis.identity()
        origin.set(0.0, 0.0, 0.0)
    }

    fun inverse() {
        basis.transpose() // no scale -> transpose = inverse
        basis.transform(origin.negate())
    }

    fun setInverse(tr: Transform) {
        tr.basis.transpose(basis)
        basis.transform(tr.origin.negate(origin))
    }

    fun invXform(inVec: Vector3d, out: Vector3d) {
        inVec.sub(origin, out)
        val mat = Stack.borrowMat(basis)
        mat.transpose()
        mat.transform(out)
    }

    fun mul(tr: Transform) {
        // can only be simplified, if tr !== this
        val vec = Stack.borrowVec(tr.origin)
        transformPosition(vec)
        basis.mul(tr.basis)
        origin.set(vec)
    }

    fun setMul(tr1: Transform, tr2: Transform) {
        val vec = Stack.borrowVec(tr2.origin)
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

    fun getRotation(out: Quaterniond): Quaterniond {
        basis.getUnnormalizedRotation(out).normalize()
        return out
    }

    fun setRotation(q: Quaterniond) {
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
