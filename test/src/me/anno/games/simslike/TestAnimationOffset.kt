package me.anno.games.simslike

import me.anno.Engine
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.io.files.Reference.getReference
import org.joml.Matrix4x3f

/**
 * There was a weird y-offset for my bone-by-bone animation.
 * Root-cause was that we must not apply the bone-matrix for the root bone when converting imported animations to bone-by-bone animations.
 * */
fun main() {

    val frameIndex = 0
    val boneIndex = 0

    OfficialExtensions.initForTests()
    val folder = getReference("E:/Assets/Mixamo XBot/Female Locomotion Pack.zip")
    val meshFile = folder.getChild("X Bot.fbx")
    val animFolder = folder.getChild("idle.fbx/animations/mixamo.com")
    val mesh = MeshCache.getEntry(meshFile).waitFor() as Mesh
    val dstSkeleton = SkeletonCache.getEntry(mesh.skeleton).waitFor()!!
    val importedAnim = AnimationCache.getEntry(animFolder.getChild("Imported.json")).waitFor() as ImportedAnimation
    val boneByBoneAnim = AnimationCache.getEntry(animFolder.getChild("BoneByBone.json")).waitFor() as BoneByBoneAnimation
    val mappedAnimation = AnimationCache.getMappedAnimation(importedAnim, dstSkeleton).waitFor()!!
    val imported = importedAnim.getMatrix(frameIndex, boneIndex, listOf(Matrix4x3f()))
    val boneByBone = boneByBoneAnim.getMatrix(frameIndex, boneIndex, listOf(Matrix4x3f()))
    println("imported: $imported") // looks good
    println("boneByBone: $boneByBone") // has offset :/
    val mapped = mappedAnimation.getMatrix(frameIndex, boneIndex, listOf(Matrix4x3f()))
    println("mapped: $mapped") // offset is inherited
    Engine.requestShutdown()
}