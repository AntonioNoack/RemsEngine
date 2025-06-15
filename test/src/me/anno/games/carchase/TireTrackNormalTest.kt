package me.anno.games.carchase

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.DecalMaterial
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.PIf
import me.anno.utils.OS.pictures

/**
 * create decal material using the generated texture
 * */
fun main() {

    val scene = Entity()
    val cube = Entity("Floor", scene)
        .add(MeshComponent(flatCube))

    val mat = DecalMaterial()
    mat.writeColor = false
    mat.writeNormal = true
    mat.normalMap =
        if (false) tracksDecalFile
        else pictures.getChild("BricksNormal.png")
    mat.normalStrength = 0.5f
    mat.decalSharpness.set(5f)

    Entity("Top", cube)
        .add(MeshComponent(flatCube, mat))
        .setPosition(0.0, 1.0, 0.0)
        .setRotation(PIf / 2f, 0f, 0f)
        .setScale(0.7f)

    Entity("Bottom", cube)
        .add(MeshComponent(flatCube, mat))
        .setPosition(0.0, -1.0, 0.0)
        .setRotation(PIf / 2f, 0f, 0f)
        .setScale(0.7f)

    Entity("Front", cube)
        .add(MeshComponent(flatCube, mat))
        .setPosition(0.0, 0.0, 1.0)
        .setRotation(0f, 0f, 0f)
        .setScale(0.7f)

    Entity("Back", cube)
        .add(MeshComponent(flatCube, mat))
        .setPosition(0.0, 0.0, -1.0)
        .setRotation(0f, 0f, 0f)
        .setScale(0.7f)

    Entity("Left", cube)
        .add(MeshComponent(flatCube, mat))
        .setPosition(-1.0, 0.0, 0.0)
        .setRotation(0f, -PIf / 2, 0f)
        .setScale(0.7f)

    Entity("Right", cube)
        .add(MeshComponent(flatCube, mat))
        .setPosition(1.0, 0.0, 0.0)
        .setRotation(0f, -PIf / 2, 0f)
        .setScale(0.7f)

    testSceneWithUI("Track Decals", scene)
}