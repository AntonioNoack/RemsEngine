package me.anno.tests.physics.bullet.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.PhysicalBody
import me.anno.bullet.constraints.HingeConstraint
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.ECSRegistry
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes.showDebugArrow
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.UIColors
import me.anno.utils.pooling.JomlPools
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * using bullet constraints, build a hanging bridge, and a few objects on top for testing
 * */
fun main() {

    ECSRegistry.init()

    val scene = Entity("Scene")
    Systems.registerSystem(BulletPhysics().apply {
        // updateInEditMode = true
        enableDebugRendering = true
    })

    val numBars = 10

    val barSize = Vector3d(1.2, 0.1, 0.35)
    val barSpacing = 0.1
    val density = 0.5

    val height = 3.0
    val pillarSize = Vector3d(1.5, height, 1.5)
    val pillarMaterial = Material.diffuse(0x333333)

    val barMesh = flatCube.scaled(Vector3f(barSize).mul(0.5f)).front
    val pillarMesh = flatCube.scaled(Vector3f(pillarSize).mul(0.5f)).front

    val toLink = ArrayList<Entity>()
    val linkMaterial = Material.diffuse(0x777777)
    val barMaterial = Material.diffuse(0x6A5540)

    fun addPillar(i: Int, sign: Int) {
        val z = (i - (numBars - 1) * 0.5) * (barSpacing + barSize.z) + sign * 0.5 * (pillarSize.z - barSize.z)
        val entity = Entity("Pillar[$sign]", scene)
            .setPosition(0.0, pillarSize.y * 0.5, z)
            .add(BoxCollider().apply {
                halfExtents.set(pillarSize).mul(0.5f)
                roundness = 0.1f
            })
            .add(MeshComponent(pillarMesh, pillarMaterial))
            .add(DynamicBody().apply {
                friction = 1.0
                if (this is DynamicBody) {
                    mass = pillarSize.x * pillarSize.y * pillarSize.z * density
                }
            })
        toLink.add(entity)
    }

    addPillar(-1, -1)

    for (i in 0 until numBars) {
        val z = (i - (numBars - 1) * 0.5) * (barSpacing + barSize.z)
        val entity = Entity("Bar[$i]", scene)
            .setPosition(0.0, height - barSize.y * 0.5, z)
            .add(BoxCollider().apply {
                halfExtents.set(barSize).mul(0.5f)
                roundness = 0.1f
            })
            .add(MeshComponent(barMesh, barMaterial))
            .add(DynamicBody().apply {
                mass = density * barSize.x * barSize.y * barSize.z
                friction = 0.5
            })
        toLink.add(entity)
    }

    addPillar(numBars, +1)

    // add all links
    val linkThickness = 0.02f
    val linkMesh = flatCube.scaled(Vector3f(linkThickness, barSpacing.toFloat(), linkThickness)).front
    val linkY = height - barSize.y * 0.5
    for (i in 1 until toLink.size) {
        val a = toLink[i - 1]
        val b = toLink[i]

        val link = HingeConstraint()
        link.disableCollisionsBetweenLinked = false
        // link.breakingImpulseThreshold = 0.3 // a little fun ^^
        link.other = b.getComponent(PhysicalBody::class)
        link.lowerLimit = -2.0
        link.upperLimit = +2.0
        // link.lerpingSpeed = 0.5
        a.add(link)
        val z = ((i - 1.5) - (numBars - 1) * 0.5) * (barSpacing + barSize.z)
        val x = 0.0
        link.selfPosition.set(x, linkY - a.position.y, z - a.position.z)
        link.otherPosition.set(x, linkY - b.position.y, z - b.position.z)
        Entity("Link[$i]", scene)
            .setPosition(x, linkY, z)
            .add(MeshComponent(linkMesh, linkMaterial))
            .add(object : Component(), OnUpdate {
                override fun onUpdate() {
                    // this calculation isn't ideal, but it should be sufficient for now
                    val transform = transform ?: return
                    val tmp = JomlPools.vec3d.create()
                    val globalA = JomlPools.vec3d.create().set(link.selfPosition)
                    val globalB = JomlPools.vec3d.create().set(link.otherPosition)

                    globalA.z -= barSpacing
                    globalB.z += barSpacing

                    // use better a.position and b.position by using the anchors
                    a.transform.globalTransform.transformPosition(globalA)
                    b.transform.globalTransform.transformPosition(globalB)

                    // visualize these points
                    showDebugArrow(
                        DebugLine(
                            Vector3d(globalA), Vector3d(globalB),
                            UIColors.paleGoldenRod, 0f
                        )
                    )

                    val dir = globalB.sub(globalA, tmp).normalize()
                    transform.localRotation = Quaternionf(dir.normalToQuaternionY())
                    transform.localPosition = globalA.add(globalB, transform.localPosition).mul(0.5)
                    invalidateBounds()
                    JomlPools.vec3d.sub(3)
                }
            })
    }

    spawnFloor(scene)
    spawnSampleCubes(scene)

    testSceneWithUI("Hinge Bridge", scene)
}
