package me.anno.tests.physics

import me.anno.animation.LoopingState
import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.GhostBody
import me.anno.bullet.bodies.KinematicBody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.DefaultAssets.goldenMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.clamp
import me.anno.tests.physics.constraints.spawnFloor
import me.anno.tests.physics.constraints.spawnSampleCubes

class PlatformScript(val ghostBody: GhostBody) : Component(), OnPhysicsUpdate {

    var range = 5.0
    var speed = 4.0
    var time = range * 0.5 / speed // start at center

    @DebugProperty
    val duration: Double get() = range / speed

    override fun onPhysicsUpdate(dt: Double) {
        if (speed > 0.0 && ghostBody.numOverlaps > 0) {
            time += dt
            val duration = clamp(duration, 1e-300, 1e300)
            val animTime = LoopingState.PLAY_REVERSING_LOOP[time, duration]
            val transform = transform ?: return
            transform.setLocalPosition(0.0, 0.5, (animTime - duration * 0.5) * speed)
            invalidateBounds()
        }
    }
}

fun main() {

    registerSystem(BulletPhysics().apply {
        enableDebugRendering = true
    })

    val scene = Entity()
    spawnFloor(scene)
    spawnSampleCubes(scene)

    // for this to detect anything, the moved cube must be active!
    val ghost = GhostBody()
    Entity("Detector", scene)
        .add(BoxCollider())
        .add(MeshComponent(flatCube, goldenMaterial))
        .add(ghost)
        .setPosition(2.5, 0.025, 0.0)
        .setScale(0.6f, 0.05f, 0.6f)

    Entity("Moving Platform", scene)
        .add(KinematicBody())
        .add(BoxCollider())
        .add(MeshComponent(flatCube, Material.diffuse(0xff9955)))
        .setPosition(0.0, 0.5, 0.0)
        .setScale(1f, 0.2f, 1.4f)
        .add(PlatformScript(ghost))

    testSceneWithUI("Moving Platform", scene)
}