package me.anno.tests.physics.bullet

import com.bulletphysics.dynamics.RigidBody
import me.anno.Time
import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.CharacterBody
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.GhostBody
import me.anno.bullet.bodies.KinematicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.CollisionFilters.GHOST_MASK
import me.anno.ecs.components.collider.CollisionFilters.KINEMATIC_MASK
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.CapsuleModel
import me.anno.ecs.components.mesh.shapes.RingMeshModel
import me.anno.ecs.systems.OnPhysicsUpdate
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAU
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.ui.UIColors
import me.anno.utils.structures.lists.Lists.roundRobin
import me.anno.utils.types.Booleans.minus
import me.anno.utils.types.Booleans.withoutFlag
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tan

/**
 * Creates a scene with a keyboard-controllable character, that can push a few balls around.
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

    val colors = intArrayOf(
        UIColors.dodgerBlue,
        UIColors.fireBrick,
        UIColors.greenYellow,
        UIColors.midOrange,
        UIColors.gold,
        UIColors.blueishGray,
        UIColors.paleGoldenRod
    )
    val materials = colors.map { color ->
        Material.diffuse(color)
    }
    for (i in 0 until 10) {
        val angle = i * TAU / 10
        val material = materials.roundRobin(i)
        Entity("Gem", scene)
            .add(DynamicBody())
            .add(SphereCollider())
            .add(MeshComponent(DefaultAssets.icoSphere, material))
            .setPosition(cos(angle), 1.0, sin(angle))
            .setScale(0.2f)
    }

    val doorTest = Entity("DoorTest", scene)
        .setPosition(0.0, 0.0, -3.0)

    val door = Entity("Door", doorTest)
        .setPosition(0.0, 1.0, 0.0)
        .setScale(0.5f, 1f, 0.05f)
        .add(BoxCollider())
        .add(MeshComponent(DefaultAssets.flatCube, Material.diffuse(0xcc7722)))
        .add(KinematicBody().apply {
            // door should not activate sensors
            collisionMask = collisionMask.withoutFlag(GHOST_MASK)
        }) // purely script controlled

    // add door opening when the player is close enough / pressure plate
    Entity("PressurePlate", doorTest)
        .add(BoxCollider())
        .add(GhostBody().apply {
            // pressure plate should not be activated by kinematic objects
            collisionMask = collisionMask.withoutFlag(KINEMATIC_MASK)
        })
        .add(MeshComponent(DefaultAssets.flatCube, Material.diffuse(0xff0000)))
        .setPosition(1.0, 0.0, 1.5)
        .setScale(0.4f, 0.05f, 0.4f)
        .add(object : Component(), OnUpdate {
            var progress = 0f
            override fun onUpdate() {
                val ghost = getComponent(GhostBody::class) ?: return
                val targetProgress = ghost.numOverlaps.toFloat()
                progress = progress.stepTowards(targetProgress, Time.deltaTime.toFloat() * 5f)
                val angle = mix(-90f, 0f, progress).toRadians()
                door.setRotation(0f, angle, 0f)
                val r = 0.5
                door.setPosition(-cos(angle) * r, 1.0, +sin(angle) * r)
            }
        })

    Entity("Player", scene)
        .add(PlayerController())
        .add(CharacterBody().apply { stepHeight = 1.0 })
        .add(CapsuleCollider().apply { radius = 0.5f; halfHeight = 0.5f })
        .add(SphereCollider())
        .add(MeshComponent(CapsuleModel.createCapsule(20, 10, 0.5f, 0.5f)))
        .add(NearbyPusher())
        .setPosition(0.0, 1.0, 0.0)

    val ngonMesh = RingMeshModel.createRingMesh(
        12, 0f, 1f,
        0, 2, 1, 0f,
        true, Mesh()
    )
    Entity("Floor", scene)
        .add(StaticBody())
        .add(InfinitePlaneCollider())
        .add(MeshComponent(ngonMesh))
        .setRotation(0f, PIf / 12f, 0f)
        .setScale(10f)

    // add border around world to prevent balls from rolling away
    for (i in 0 until 12) {
        val angle = i * TAU / 12
        val radius = 9.5f
        val length = radius * tan(PIf / 12)
        Entity("Border[$i]", scene)
            .add(StaticBody())
            .add(BoxCollider())
            .add(MeshComponent(DefaultAssets.flatCube))
            .setScale(length, 0.3f, 0.3f)
            .setPosition(sin(angle) * radius, 0.3, cos(angle) * radius)
            .setRotation(0f, angle.toFloat(), 0f)
    }

    testSceneWithUI("GemMiner", scene)
}

class NearbyPusher : Component(), OnPhysicsUpdate {

    var maxForce = 10.0
    var radius = 1.0

    override fun onPhysicsUpdate(dt: Double) {

        val ghost = getComponent(CharacterBody::class) ?: return
        val ghost2 = ghost.nativeInstance2 ?: return

        val currentPosition = ghost2.currentPosition
        val dir = Vector3d()
        ghost2.ghostObject.overlappingPairCache.processAllOverlappingPairs { pair ->
            val other = if (pair.proxy0.clientObject === ghost2.ghostObject) {
                pair.proxy1.clientObject
            } else pair.proxy0.clientObject

            if (other is RigidBody && !other.isStaticOrKinematicObject) {
                // Compute push direction (horizontal only)
                other.worldTransform.origin.sub(currentPosition, dir)
                dir.y = 0.0

                // Strength decreases with distance
                val pushStrength = maxForce * clamp(1.0 - dir.length() / radius)
                dir.safeNormalize(pushStrength)

                // todo if aligning, add velocity onto force
                // ghost2.targetVelocity

                // Apply central force
                other.activate()
                other.applyCentralForce(dir)
            }
        }
    }
}

// todo bug: stepping up isn't working :(
class PlayerController : Component(), OnUpdate {

    var dtAcceleration = 5f
    var speed = 3.0

    private val velocity = Vector3d()

    override fun onUpdate() {
        val character = getComponent(CharacterBody::class) ?: return
        val physics = Systems.findSystem(BulletPhysics::class.java) ?: return
        val dt = physics.fixedStep
        val dirX = Input.isKeyDown(Key.KEY_L) - Input.isKeyDown(Key.KEY_J)
        val dirZ = Input.isKeyDown(Key.KEY_K) - Input.isKeyDown(Key.KEY_I)

        val dir = Vector3d(dirX.toDouble(), 0.0, dirZ.toDouble())
        val rv = RenderView.currentInstance
        if (rv != null && (dirX != 0 || dirZ != 0)) {
            // rotate with camera
            val angle = rv.orbitRotation.getEulerAngleYXZvY()
            dir.rotateY(angle.toDouble())
        }
        dir.mul(speed)

        velocity.lerp(dir, dtTo01(dt * dtAcceleration))

        character.nativeInstance2?.setTargetVelocity(velocity)
        if (Input.wasKeyPressed(Key.KEY_SPACE)) {
            character.jumpSpeed = 5.0
            character.jump()
        }
    }
}

fun Float.stepTowards(target: Float, step: Float): Float {
    val delta = target - this
    return if (abs(delta) <= step) target
    else this + sign(delta) * step
}
