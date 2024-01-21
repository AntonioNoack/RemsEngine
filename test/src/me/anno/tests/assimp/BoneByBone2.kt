package me.anno.tests.assimp

import me.anno.animation.LoopingState
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView
import me.anno.maths.Maths
import me.anno.tests.LOGGER
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * ImportedAnimation -> BoneByBone -> ImportedAnimation
 * */
fun main() {
    LogManager.enableLogger("AnimatedMeshesLoader")

    // front legs are broken slightly???

    ECSRegistry.initMeshes()

    val meshFile = OS.downloads.getChild("3d/azeria/scene.gltf")
    var animFile = meshFile.getChild("animations")
    if (animFile.listChildren().first().isDirectory) animFile = animFile.listChildren().first()
    animFile = animFile.getChild("BoneByBone.json")
    val animation = PrefabCache.getPrefabInstance(animFile).run {
        if (this is ImportedAnimation) BoneByBoneAnimation(this)
        else this as BoneByBoneAnimation
    }

    // create test scene
    val scene = Entity("Scene")
    val mesh = AnimMeshComponent()
    mesh.skeleton = animation.skeleton
    val animState = AnimationState(animation.ref, 1f, 0f, 1f, LoopingState.PLAY_LOOP)
    mesh.animations = listOf(animState)
    mesh.meshFile = meshFile
    scene.add(mesh)

    for (bone in SkeletonCache[mesh.skeleton]!!.bones) {
        LOGGER.debug(
            "Bone ${bone.id}: ${bone.name}${" ".repeat(Maths.max(0, 80 - bone.name.length))}" +
                    "f0: ${animation.getTranslation(0, bone.id, Vector3f())}, " +
                    "${animation.getRotation(0, bone.id, Quaternionf())}"
        )
    }

    // create script, which modifies the animation at runtime
    scene.add(object : Component() {
        val r = Quaternionf()
        override fun onUpdate(): Int {
            // rotations should be relative to their parent, probably
            //  but also in global space... is this contradicting?
            // yes, it is 😅, but we could define sth like a common up;
            val time = animState.progress * 2f * Maths.PIf / animation.duration
            val boneIndex = if (meshFile.absolutePath.contains("azeria")) 7 else 67
            val amplitude = 0.7f
            r.identity()
                .rotateX(sin(time) * amplitude)
                .rotateY(cos(time) * amplitude)
            // todo we'd actually only need to update the currently used two frames
            for (fi in 0 until animation.frameCount) {
                animation.setRotation(fi, boneIndex, r)
            }
            AnimationCache.invalidate(animation)
            return 1
        }
    })

    SceneView.testSceneWithUI("BoneByBoneAnimation", scene)
}