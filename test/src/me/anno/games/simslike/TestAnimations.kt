package me.anno.games.simslike

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.simslike.Sim.Companion.weight
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS

val assets =
    if (OS.isWindows) getReference("E:/Assets")
    else getReference("/media/antonio/4TB WDRed/Assets")

fun main() {
    OfficialExtensions.initForTests()

    val scene = Entity("Scene")
    val animatedMeshSrc = assets.getChild("/Mixamo XBot/Female Locomotion Pack.zip")

    // todo why is only 'running' loading???
    // todo mix bones before root motion, try that out with running + turning

    val size = 5
    for (i in 0 until size) {
        val x = (i - (size - 1) * 0.5) * 5.0
        val f = i * 3f / (size - 1f)
        Entity(scene)
            .add(AnimMeshComponent().apply {
                meshFile = animatedMeshSrc.getChild("X Bot.fbx")
                animations = listOf(
                    AnimationState(animatedMeshSrc.getChild("idle.fbx"), weight(f)),
                    AnimationState(animatedMeshSrc.getChild("walking.fbx"), weight(f - 1f)),
                    AnimationState(animatedMeshSrc.getChild("running.fbx"), weight(f - 2f)),
                    AnimationState(animatedMeshSrc.getChild("left turn.fbx"), weight(f - 3f))
                )
            }).setPosition(x, 0.0, 0.0)
    }

    testSceneWithUI("SimsLike", scene)
}