package me.anno.games.simplefps

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.Color.toRGB
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3d

/**
 * sample on how to write a first-person shooter (FPS) game (very WIP)
 * */
fun main() {

    // done world
    //  - floor
    //  - targets
    //  - basic block physics on them
    //  - impulse when shot -> tumbling

    // todo controls:
    //  - look around
    //  - orbitRadius = 0
    //  - walk???
    //  - sneak?
    //  - jump
    //  - shoot
    //  - reload?

    Systems.registerSystem(BulletPhysics().apply { updateInEditMode = true })

    val scene = Entity("Scene")
    val controls = SimpleShootingControls()
    scene.add(controls)

    Entity("Floor", scene)
        .add(StaticBody())
        .add(BoxCollider().apply { roundness = 0.001f / 50.0f })
        .add(MeshComponent(flatCube, Material.diffuse(0x555555)))
        .setPosition(0.0, -50.0, 0.0)
        .setScale(50f)

    // sample cubes stacked on top of each other
    for (i in 0 until 5) {
        val color = HSLuvColorSpace.hsluvToRgb(Vector3d(360.0 * i / 5, 100.0, 70.0))
        Entity("Cube[$i]", scene)
            .add(DynamicBody().apply { mass = 2.5f })
            .add(BoxCollider().apply { roundness = 0.001f })
            .add(MeshComponent(flatCube, Material.diffuse(color.toRGB())))
            .setPosition(0.0, i * 1.002 + 0.001, 0.0)
            .setRotation(0f, i.toFloat(), 0f)
            .setScale(0.5f)
    }

    testSceneWithUI("FirstPersonShooter", scene) {
        EditorState.select(controls)
    }
}