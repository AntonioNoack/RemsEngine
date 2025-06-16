package me.anno.games.roadcraft

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.games.roadcraft.FallingSandShader.optY
import me.anno.games.roadcraft.FallingSandShader.strandSize
import me.anno.mesh.Shapes.flatCube
import org.joml.Vector3f

fun main() {

    val material = FallingSandMaterial()
    val size = material.size
    val scale = Vector3f(
        0.5f * size.x * strandSize,
        0.5f * size.y * strandSize * optY,
        0.5f * size.z * strandSize
    )

    val scene = Entity("Scene")
    Entity("Sand", scene)
        .setPosition(0.0, scale.y.toDouble(), 0.0)
        .add(MeshComponent(flatCube.scaled(scale).front, material))

    Entity("Floor", scene)
        .add(MeshComponent(flatCube.front))
        .setPosition(0.0, -1.0, 0.0)
        .setScale(scale.y * 1.5f, 1f, scale.y * 1.5f)

    testSceneWithUI("Falling Sand", scene)
}