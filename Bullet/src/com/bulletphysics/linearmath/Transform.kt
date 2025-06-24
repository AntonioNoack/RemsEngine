package com.bulletphysics.linearmath

import com.bulletphysics.linearmath.MatrixUtil.getRotation
import com.bulletphysics.linearmath.MatrixUtil.setRotation
import com.bulletphysics.util.setMul
import com.bulletphysics.util.setSub
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

    constructor()

    constructor(mat: Matrix3d) {
        basis.set(mat)
    }

    constructor(tr: Transform) {
        set(tr)
    }

    fun set(tr: Transform) {
        basis.set(tr.basis)
        origin.set(tr.origin)
    }

    fun set(mat: Matrix3d) {
        basis.set(mat)
        origin.set(0.0, 0.0, 0.0)
    }

    fun transform(src: Vector3d, dst: Vector3d = src) {
        basis.transform(src, dst).add(origin)
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

       /* basis.transpose()
        origin.negate()
        basis.transform(origin)*/
    }

    fun setInverse(tr: Transform) {
        set(tr)
        inverse()
    }

    fun invXform(inVec: Vector3d, out: Vector3d) {
        out.setSub(inVec, origin)
        val mat = Stack.borrowMat(basis)
        mat.transpose()
        mat.transform(out)
    }

    fun mul(tr: Transform) {
        val vec = Stack.borrowVec(tr.origin)
        transform(vec)
        basis.mul(tr.basis)
        origin.set(vec)
    }

    fun setMul(tr1: Transform, tr2: Transform) {
        val vec = Stack.borrowVec(tr2.origin)
        tr1.transform(vec)
        basis.setMul(tr1.basis, tr2.basis)
        origin.set(vec)
    }

    fun setTranslation(x: Double, y: Double, z: Double) {
        origin.set(x, y, z)
    }

    fun setTranslation(v: Vector3d) {
        origin.set(v)
    }

    fun getRotation(out: Quaterniond): Quaterniond {
        getRotation(basis, out)
        return out
    }

    fun setRotation(q: Quaterniond) {
        setRotation(basis, q)
    }

    override fun equals(other: Any?): Boolean {
        return other is Transform &&
                basis == other.basis &&
                origin == other.origin
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 41 * hash + basis.hashCode()
        hash = 41 * hash + origin.hashCode()
        return hash
    }
}
