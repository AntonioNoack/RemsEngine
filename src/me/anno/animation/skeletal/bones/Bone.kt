package me.anno.animation.skeletal.bones

import me.anno.utils.types.Vectors.minus
import org.joml.Matrix4x3f
import org.joml.Vector3f

// head -> tail
/**
 * Each Animated Skeleton has this abstraction layer to simplify calculations
 * */
class Bone(
    val index: Int,
    val name: String,
    val head: Vector3f,
    val tail: Vector3f,
    val rotation: Vector3f,
    val minRotation: Vector3f,
    val maxRotation: Vector3f
) {

    /**
     * deltas for bones
     * */
    val localBindPose = Matrix4x3f()

    /**
     * world transform
     */
    val jointWorldTransform = Matrix4x3f()

    /**
     * user defined transforms
     * */
    val animatedTransform = Matrix4x3f()

    /**
     * user defined transforms
     * */
    val globalBindTransform = Matrix4x3f()

    /**
     * skinning matrices
     * */
    val skinningMatrix = Matrix4x3f()

    var parent: Bone? = null
    val delta = tail - head
    val children = ArrayList<Bone>(4)
    val canMoveX = maxRotation.x > minRotation.x
    val canMoveY = maxRotation.y > minRotation.y
    val canMoveZ = maxRotation.z > minRotation.z

    fun limit(min: Float, max: Float) {
        minRotation.set(min)
        maxRotation.set(max)
    }

    fun lock(mask: Int) {
        if (mask and 1 != 0) {
            minRotation.x = 0f
            maxRotation.x = 0f
        }
        if (mask and 2 != 0) {
            minRotation.y = 0f
            maxRotation.y = 0f
        }
        if (mask and 4 != 0) {
            minRotation.z = 0f
            maxRotation.z = 0f
        }
    }

    fun updateDelta() {
        delta.set(tail - head)
    }

    val hasRotation get() = rotation.x != 0f || rotation.y != 0f || rotation.z != 0f

    // todo for fbx we do know the bind pose: it's "GetLink()->EvaluateLocalTransform(FBXSDK_TIME_INFINITE)"
    // by https://forums.autodesk.com/t5/fbx-forum/manually-building-bindpose-matrices-in-fbx-sdk/td-p/7440504
    fun updateLocalBindPose() {
        localBindPose.identity()
        if (index > 0) {
            localBindPose.translate(delta)
        }
    }

    fun updateUserTransform() {
        val customMatrix = animatedTransform
        customMatrix.identity()
        if (hasRotation) {
            customMatrix.translate(-delta.x, -delta.y, -delta.z)
            customMatrix.rotateX(rotation.x)
            customMatrix.rotateY(rotation.y)
            customMatrix.rotateZ(rotation.z)
            // customMatrix.rotate(rotation)
            customMatrix.translate(delta)
        }
    }

    fun updateTransform(tmp: Matrix4x3f) {
        if (index == 0) {

            // jointWorldTransform_j = jointWorldTransform_parent(j) * localBindPose_j * animatedTransform_j
            localBindPose.mul(animatedTransform, jointWorldTransform)

            // globalBindTransform_j = globalBindTransform_parent * localBindPose_j
            globalBindTransform.set(localBindPose)

            // skinning = jointWorldTransform * inv(globalBindTransform)
            jointWorldTransform.mul(globalBindTransform.invert(tmp), skinningMatrix)

        } else {

            val parent = parent!!

            // jointWorldTransform_j = jointWorldTransform_parent(j) * localBindPose_j * animatedTransform_j
            parent.jointWorldTransform.mul(localBindPose, tmp)
            tmp.mul(animatedTransform, jointWorldTransform)

            // globalBindTransform_j = globalBindTransform_parent * localBindPose_j
            parent.globalBindTransform.mul(localBindPose, globalBindTransform)

            // skinning = jointWorldTransform * inv(globalBindTransform)
            jointWorldTransform.mul(globalBindTransform.invert(tmp), skinningMatrix)

        }
    }

    fun createAxes(): List<BoneAxis> {
        val result = ArrayList<BoneAxis>(3)
        if (canMoveX) result += BoneAxis(this, 0)
        if (canMoveY) result += BoneAxis(this, 1)
        if (canMoveZ) result += BoneAxis(this, 2)
        return result
    }

}