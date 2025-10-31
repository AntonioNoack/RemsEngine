package me.anno.tests.physics.bullet

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.CharacterBody
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.CapsuleModel
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.TAU
import me.anno.utils.types.Booleans.minus
import org.joml.Vector3d
import kotlin.math.cos
import kotlin.math.sin

/**
 * Controls: IJKL for walking, space for jumping
 * */
fun main() {
    val physics = BulletPhysics().apply {
        updateInEditMode = true
        enableDebugRendering = true
    }
    registerSystem(physics)

    val scene = Entity()
    Systems.world = scene

    if (false) for (i in 0 until 10) {
        val angle = i * TAU / 10
        Entity("Gem", scene)
            .add(DynamicBody())
            .add(SphereCollider())
            .add(MeshComponent(DefaultAssets.icoSphere))
            .setPosition(cos(angle), 1.0, sin(angle))
            .setScale(0.2f)
    }

    Entity("Player", scene)
        .add(PlayerController())
        .add(CharacterBody())
        .add(CapsuleCollider().apply { radius = 0.5f; halfHeight = 0.5f })
        .add(MeshComponent(CapsuleModel.createCapsule(20, 10, 0.5f, 0.5f)))
        .setPosition(0.0, 1.0, 0.0)

    val floorMaterial = Material.diffuse(0x111111)
    Entity("Floor", scene)
        .add(StaticBody())
        .add(InfinitePlaneCollider())
        .add(MeshComponent(DefaultAssets.plane, floorMaterial))
        .setScale(10f)

    testSceneWithUI("GemMiner", scene)
}

class PlayerController : Component(), OnUpdate {
    override fun onUpdate() {
        val body = getComponent(CharacterBody::class) ?: return
        val physics = Systems.findSystem(BulletPhysics::class.java) ?: return
        val dt = physics.fixedStep
        val dirX = Input.isKeyDown(Key.KEY_L) - Input.isKeyDown(Key.KEY_J)
        val dirZ = Input.isKeyDown(Key.KEY_K) - Input.isKeyDown(Key.KEY_I)
        val dir = Vector3d(dirX * dt, 0.0, dirZ * dt)
        body.nativeInstance2?.setWalkDirection(dir)
        if (Input.wasKeyPressed(Key.KEY_SPACE)) {
            body.jumpSpeed = 5.0
            body.jump()
        }
    }
}