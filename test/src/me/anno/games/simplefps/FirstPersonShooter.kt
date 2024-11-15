package me.anno.games.simplefps

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import kotlin.math.pow

/**
 * sample on how to write a first-person shooter (FPS) game (very WIP)
 * */
fun main() {
    // todo world
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

    Systems.registerSystem(BulletPhysics())

    val scene = Entity("Scene")
    Entity("Floor", scene)
        .add(Rigidbody().apply { mass = 0.0 })
        .add(BoxCollider())
        .add(MeshComponent(flatCube, Material.diffuse(0x555555)))
        .setPosition(0.0, -50.0, 0.0)
        .setScale(50.0)

    // sample cubes stacked on top of each other
    for (i in 0 until 5) {
        Entity("Cube[$i]", scene)
            .add(Rigidbody().apply { mass = (50.0).pow(3) * 2.5 })
            .add(BoxCollider())
            .add(MeshComponent(flatCube, Material.diffuse(0x555555)))
            .setPosition(0.0, i + 2.0, 0.0)
            .setRotation(0.0, i * 1.0, 0.0)
            .setScale(0.5)
    }

    testSceneWithUI("FirstPersonShooter", scene)
}