package me.anno.ecs.components.anim

import com.jme3.anim.AnimClip
import com.jme3.anim.Armature
import com.jme3.anim.Joint
import com.jme3.math.Matrix4f
import org.joml.Matrix4x3f
import org.lwjgl.BufferUtils
import java.nio.FloatBuffer

class Skeleton(val joints: Array<Joint>) {

    val armature = Armature(joints)

    val matrices = Array(joints.size) { Matrix4x3f() }

    // val composer = AnimComposer()

    fun lerpAnimation(animation: AnimClip, factor: Float) {
        if (factor <= 0f) return
        val set = factor >= 1f
        // todo set it somehow...
    }

    fun computeSkinningMatrices(): Array<Matrix4x3f> {
        val result = armature.computeSkinningMatrices()
        for (i in joints.indices) {
            set(matrices[i], result[i])
        }
        return matrices
    }

    fun computeSkinningMatrices(dst: FloatBuffer = gpuBuffer) {
        val result = armature.computeSkinningMatrices()
        for (i in joints.indices) {
            // todo is this correct for src?, or does it need to be transposed?
            // todo is the layout correct for dst, or does it need to be transposed?
            val src = result[i]
            // set(matrices[i], result[i])
            dst.put(src.m00)
            dst.put(src.m01)
            dst.put(src.m02)
            dst.put(src.m10)
            dst.put(src.m11)
            dst.put(src.m12)
            dst.put(src.m20)
            dst.put(src.m21)
            dst.put(src.m22)
            dst.put(src.m30)
            dst.put(src.m31)
            dst.put(src.m32)
        }
    }


    fun set(dst: Matrix4x3f, src: Matrix4f) {
        dst.set(
            src.m00, src.m01, src.m02,
            src.m10, src.m11, src.m12,
            src.m20, src.m21, src.m22,
            src.m30, src.m31, src.m32,
        )
    }

    companion object {
        val gpuBuffer = BufferUtils.createFloatBuffer(12 * 256)
    }

}