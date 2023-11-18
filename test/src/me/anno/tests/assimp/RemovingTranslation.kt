package me.anno.tests.assimp

import me.anno.Engine
import me.anno.ecs.components.anim.BoneByBoneAnimation.Companion.fromImported
import me.anno.ecs.components.anim.BoneByBoneAnimation.Companion.toImported
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.engine.ECSRegistry
import me.anno.utils.OS.documents
import me.anno.utils.types.Floats.f2x
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f

// most bones only experience rotation, not translation:
// remove the translation parameter, and calculate it only using the rotation (and bind pose / original pose)

fun Matrix4x3f.f2y(): String {
    val t = getTranslation(Vector3f())
    val r = getUnnormalizedRotation(Quaternionf())
    return "" +
            "                          |${t.x.f2x()}|\n" +
            "(${r.x.f2x()} ${r.y.f2x()} ${r.z.f2x()} ${r.w.f2x()}) |${t.y.f2x()}|\n" +
            "                          |${t.z.f2x()}|"
}

fun main() {

    ECSRegistry.init()

    // fbx animation is broken...
    // glb works
    val file = documents.getChild("RobotArmTest.glb")
    val animFile = file.getChild("animations").listChildren()!!.first()
    val animation = AnimationCache[animFile]!! as ImportedAnimation
    val skeleton = SkeletonCache[animation.skeleton]!!
    val bones = skeleton.bones
    for (bone in bones) {
        println(bone)
    }

    // testSceneWithUI(PrefabCache[file, false]!!)

    println("duration: ${animation.duration}, frames: ${animation.frames.size}")

    val printer = TablePrinter()
    val pred = Vector3f()
    val predictedTranslations = Array(bones.size) { Vector3f() }
    for ((fi, frameIndex) in (0 until animation.numFrames step 5).withIndex()) {

        for (boneId in bones.indices) {

            val bone = bones[boneId]

            printer.addColumnTitle("\"${bone.name}\", bind pose:\n${bone.bindPose.f2y()}")

            if (boneId == 0) {
                printer.addRowTitle("frame $frameIndex")
            }

            val skinning = animation.frames[frameIndex][boneId] // target matrix
            val parentSkinning = animation.frames[frameIndex].getOrNull(bone.parentId)
            val test = Matrix4x3f()
            fromImported(bone.bindPose, bone.inverseBindPose, skinning, parentSkinning, Vector3f(), Quaternionf(), Vector3f(), test)

            val invTest = Matrix4x3f()
            toImported(
                bone, parentSkinning,
                test.getTranslation(Vector3f()),
                test.getUnnormalizedRotation(Quaternionf()),
                test.getScale(Vector3f()),
                invTest
            )

            pred.set(predictedTranslations[boneId])

            printer.println("predicted: (${pred.x.f2x()} ${pred.y.f2x()} ${pred.z.f2x()})")
            printer.println(skinning.f2y())
            printer.println("invTest:")
            printer.println(invTest.f2y())
            printer.println("optimized:")
            printer.println(test.f2y())

            printer.finishCell(boneId, fi)

        }

    }

    printer.finish(
        "  |  ", "|  ", "  |",
        '_', '-', '-'
    )

    Engine.requestShutdown()

}