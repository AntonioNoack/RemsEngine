package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
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
        val entity = Entity("Bridge[$i]", bridge)
            .setPosition(pos)
            .add(MeshCollider(mesh).apply {
                maxNumVertices = 0
                margin = 1e-6f
                roundness = 1e-6f
                isConvex = true
            })
            .add(MeshComponent(mesh))
        if (i == 0 || i == meshes.lastIndex) {
            entity.add(StaticBody().apply {
                friction = 0.9
            })
        } else {
            entity.add(DynamicBody().apply {
                // make the middle heavier for better stability?
               mass = 500.0 * (2.0 - abs(x))
                friction = 0.9
            })
        }
    }

    spawnFloor(scene)
    spawnSampleCubes(scene)

    testSceneWithUI("Bridge", scene)
}