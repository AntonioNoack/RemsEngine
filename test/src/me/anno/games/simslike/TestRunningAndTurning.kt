package me.anno.games.simslike

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.disableLoggers("Saveable")
    OfficialExtensions.initForTests()

    val scene = Entity("Scene")
    val animatedMeshSrc = assets.getChild("/Mixamo XBot/Female Locomotion Pack.zip")

    val animFile = animatedMeshSrc.getChild("left turn.fbx")
    // todo why are there no children???
    check(animFile.exists)
    val children = animFile.listChildren()
    println(children.map { it.name })

    // todo mix bones before root motion, try that out with running + turning
    // todo why is only the first animation loading???
    Entity(scene)
        .add(AnimMeshComponent().apply {
            meshFile = animatedMeshSrc.getChild("X Bot.fbx")
            animations = listOf(
                AnimationState(animatedMeshSrc.getChild("left turn.fbx"), 0.5f),
                AnimationState(animatedMeshSrc.getChild("running.fbx"), 0.5f),
            )
        })

    testSceneWithUI("SimsLike", scene)
}