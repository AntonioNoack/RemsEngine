@file:Suppress("SpellCheckingInspection")

package me.anno.mesh.assimp.test

import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.cache.AnimationCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS.downloads
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.utils.types.Vectors.f2
import org.joml.Matrix4x3f
import kotlin.math.sqrt

// todo most bones only experience rotation, not translation:
// todo remove the translation parameter, and calculate it only using the rotation (and bind pose / original pose)
fun main() {

    ECSRegistry.init()

    // fox skeleton
    val file = getReference(downloads, "3d/azeria/scene.gltf")
    val animation = AnimationCache[getReference(file, "animations/Walk.json")]!! as ImportedAnimation
    val skeleton = SkeletonCache[animation.skeleton]!!
    val bones = skeleton.bones
    for (bone in bones) {
        println(bone)
    }

    val testName = "b_RightUpperArm_06_9"

    println("duration: ${animation.duration}, frames: ${animation.frames.size}")

    val locals = Array(bones.size) { Matrix4x3f() }
    val localsInv = Array(bones.size) { Matrix4x3f() }

    for (boneId in bones.indices) {

        val bone = bones[boneId]//.first { it.name == "b_RightUpperArm_06_9" }
        val frames = animation.frames.map { it[bone.id] }

        // check guess: inv(frame.rot) * frame.trans is constant
        var sum = 0f
        var sum2 = 0f
        for (frameIndex in frames.indices) {
            val skinning = frames[frameIndex]
            // println("\nframe $index")
            // println(skinning.print())
            // boneOffsetMatrix = bone.inverseBindPose
            // skinningMatrix = localTransform * boneOffsetMatrix
            // -> localTransform = skinningMatrix * inv(boneOffsetMatrix) = skinningMatrix * bone.bindPose
            val local = Matrix4x3f(skinning).mul(bone.bindPose) // position in model
            val localB = if (bone.parentId < 0) locals[boneId].set(local) // position relative to parent
            else localsInv[bone.parentId].mul(local, locals[boneId])

            val invLocalB = localB.invert(localsInv[boneId])

            val length = length(localB.m30(), localB.m31(), localB.m32())
            sum += length
            sum2 += length * length
            // val inv = localTransform.invert(Matrix4x3f())
            // println("scale: ${localTransform.getScale(Vector3f())}") // 1,1,1, just as expected
            /*println(inv.print())
            println(localTransform.print())
            println(length(inv.m30(),inv.m31(),inv.m32()))
            println(length(localTransform.m30(),localTransform.m31(),localTransform.m32()))*/
            // println(inv.transformDirection(Vector3f(skinning.m30(), skinning.m31(), skinning.m32())).print())

            if (bone.name == testName) {
                println("\nframe $frameIndex")
                println(localB.f2())
                // todo jackpot: all translations are roughly the same
                // todo we can
                // todo - encode animations as just rotations (except root)
                // todo - extract the inherit rotation
                // todo - using an rotation, rotate a bone
                println(invLocalB.f2())
            }

        }

        sum /= frames.size
        sum2 /= frames.size

        val diff = sqrt(max(0f, sum2 - sum * sum))
        println("${bone.name} $diff/$sum, ${diff / sum}")

    }


}