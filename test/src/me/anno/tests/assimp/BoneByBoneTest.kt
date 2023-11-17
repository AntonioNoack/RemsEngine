package me.anno.tests.assimp

import me.anno.ecs.components.anim.BoneByBoneAnimation
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertTrue

class BoneByBoneTest {

    // todo we do support non-uniform scales, I think,
    //  but likely only properly correlated between matrices...
    // (or the maths is slightly wrong, and sb should fix it)

    @Test
    fun testTransformIsInvertible() {
        // skinned -> boneByBone -> skinned
        for (i in 0 until 1000) {
            testIsInvertible(
                SkinningState(
                    randomSca1(randomPos(randomRot(Matrix4x3f()))), // sca only supported in 1d
                    randomSca1(randomPos(randomRot(Matrix4x3f()))), // sca only supported in 1d
                    randomSca1(randomPos(randomRot(Matrix4x3f()))), // sca only supported in 1d
                )
            )
        }
    }

    data class SkinningState(
        val bindPose: Matrix4x3f,
        val skinning: Matrix4x3f,
        val parentSkinning: Matrix4x3f
    )

    data class BoneByBoneState(
        val position: Vector3f,
        val rotation: Quaternionf,
        val scale: Vector3f,
    )

    val random = Random(1234)
    fun randomRot(mat: Matrix4x3f): Matrix4x3f {
        return mat
            .rotateX(random.nextFloat() * 10f)
            .rotateY(random.nextFloat() * 10f)
            .rotateZ(random.nextFloat() * 10f)
    }

    fun randomPos(mat: Matrix4x3f): Matrix4x3f {
        return mat
            .translate(random.nextFloat(), random.nextFloat(), random.nextFloat())
    }

    fun randomSca1(mat: Matrix4x3f): Matrix4x3f {
        return mat.scale(random.nextFloat() + 1f)
    }

    fun randomSca3(mat: Matrix4x3f): Matrix4x3f {
        // todo do we support only forward, backward, or both???
        //  or just uniform?
        return mat.scale(
            random.nextFloat() + 1f,
            random.nextFloat() + 1f,
            random.nextFloat() + 1f
        )
    }

    fun testIsInvertible(skinning: SkinningState) {
        val boneByBone = from(skinning)
        val skinning2 = to(skinning, boneByBone)
        assertTrue(skinning.skinning.equals(skinning2, 0.001f))
    }

    fun from(state: SkinningState): BoneByBoneState {
        val pos = Vector3f()
        val rot = Quaternionf()
        val sca = Vector3f()
        BoneByBoneAnimation.fromImported(
            state.bindPose, state.skinning, state.parentSkinning,
            Matrix4x3f(), pos, rot, sca
        )
        return BoneByBoneState(pos, rot, sca)
    }

    fun to(state0: SkinningState, state1: BoneByBoneState): Matrix4x3f {
        val inverseBindPose = state0.bindPose.invert(Matrix4x3f())
        return BoneByBoneAnimation.toImported(
            state0.bindPose, inverseBindPose, state0.parentSkinning,
            Vector3f(state1.position), Quaternionf(state1.rotation), Vector3f(state1.scale), Matrix4x3f()
        )
    }
}