package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val mirrored = mirrorX(flatCube.ref)
    testSceneWithUI(
        "MirrorX",
        // todo what??? these two lines produce different results :/
        // mirrored
        Entity().add(MeshComponent(mirrored))
    )
}