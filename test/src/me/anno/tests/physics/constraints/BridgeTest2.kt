package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import kotlin.math.abs

/**
 * using bullet, build a bow bridge, and a few objects on top for testing
 *
 * todo build a bow-bridge with hinge constraints?
 * todo build a string-hung bridge using spring constraints??
 * */
fun main() {

    ECSRegistry.init()

    val scene = Entity("Scene")
    Systems.registerSystem(BulletPhysics().apply {
        //  updateInEditMode = true
        enableDebugRendering = true
    })

    val bridge = Entity("Bridge", scene)
    val meshes = createBridgeMeshes(20, 0.5f, 0.3f, 0f)
    for (i in meshes.indices) {
        val (mesh, pos) = meshes[i]
        val x = (i.toFloat() / meshes.lastIndex) * 2f - 1f
        val mass =
            if (i == 0 || i == meshes.lastIndex) 0.0
            else 500.0 * (2.0 - abs(x)) // make the middle heavier for better stability?
        Entity("Bridge[$i]", bridge)
            .setPosition(pos)
            .add(Rigidbody().apply {
                this.mass = mass
                friction = 0.9
            })
            .add(MeshCollider(mesh).apply {
                margin = 0.1f
                isConvex = true
            })
            .add(MeshComponent(mesh))
    }

    spawnFloor(scene)
    spawnSampleCubes(scene)

    testSceneWithUI("Bridge", scene)
}