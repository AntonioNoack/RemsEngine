package me.anno.games.simplefps

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Key
import me.anno.utils.Color.black
import me.anno.utils.Color.toRGB
import org.hsluv.HSLuvColorSpace
import org.joml.Vector3d

class SimpleShootingControls : Component(), CustomEditMode {

    var force = 25.0

    override fun onEditClick(button: Key, long: Boolean): Boolean {
        if (button == Key.BUTTON_LEFT) {

            val rv = RenderView.currentInstance
            val scene = rv?.getWorld() as? Entity ?: return false

            // apply impulse onto scene
            val query = RayQuery(rv.cameraPosition, Vector3d(rv.mouseDirection), 1e9)
            if (Raycast.raycast(scene, query)) {
                val hitComponent = query.result.component
                val hitEntity = hitComponent?.entity
                val hitRigidbody = hitEntity?.getComponent(Rigidbody::class)
                if (hitRigidbody != null && !hitRigidbody.isStatic) {
                    val relativePos = query.result.positionWS.sub(hitEntity.transform.globalPosition, Vector3d())
                    val impulse = Vector3d(query.direction).mul(force)
                    hitRigidbody.applyImpulse(relativePos, impulse)
                } else {
                    DebugShapes.debugArrows.add(
                        DebugLine(
                            Vector3d(rv.cameraPosition),
                            Vector3d(query.result.positionWS),
                            0x3377ff or black
                        )
                    )
                }
            }

            return true
        } else return false
    }
}

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
        .add(Rigidbody().apply { mass = 0.0 })
        .add(BoxCollider().apply { margin = 0.001f / 50.0f })
        .add(MeshComponent(flatCube, Material.diffuse(0x555555)))
        .setPosition(0.0, -50.0, 0.0)
        .setScale(50f)

    // sample cubes stacked on top of each other
    for (i in 0 until 5) {
        val color = HSLuvColorSpace.hsluvToRgb(Vector3d(360.0 * i / 5, 100.0, 70.0))
        Entity("Cube[$i]", scene)
            .add(Rigidbody().apply { mass = 2.5 })
            .add(BoxCollider().apply { margin = 0.001f })
            .add(MeshComponent(flatCube, Material.diffuse(color.toRGB())))
            .setPosition(0.0, i * 1.002 + 0.001, 0.0)
            .setRotation(0f, i.toFloat(), 0f)
            .setScale(0.5f)
    }

    testSceneWithUI("FirstPersonShooter", scene) {
        EditorState.select(controls)
    }
}