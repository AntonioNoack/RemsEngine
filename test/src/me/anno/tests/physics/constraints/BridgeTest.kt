package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.constraints.PointConstraint
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnUpdate
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.pow

/**
 * using bullet constraints, build a hanging bridge, and a few objects on top for testing
 * */
fun main() {

    ECSRegistry.init()

    val scene = Entity("Scene")
    Systems.registerSystem(BulletPhysics().apply {
        updateInEditMode = true
        enableDebugRendering = true
    })

    val numBars = 10
    val numLinks = 3
    val linkSpacing = 0.35

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
        for (j in 0 until numLinks) {
            val link = PointConstraint()
            link.disableCollisionsBetweenLinked = false
            link.breakingImpulseThreshold = 0.3 // a little fun ^^
            link.other = b.getComponent(DynamicBody::class)
            link.lerpingSpeed = 0.5
            a.add(link)
            val z = ((i - 1.5) - (numBars - 1) * 0.5) * (barSpacing + barSize.z)
            val x = (j - (numLinks - 1) * 0.5) * linkSpacing
            val barSpacingHalf = barSpacing * 0.5
            link.selfPosition.set(x, linkY - a.position.y, z - a.position.z + barSpacingHalf)
            link.otherPosition.set(x, linkY - b.position.y, z - b.position.z - barSpacingHalf)
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

                        val dir = globalB.sub(globalA, tmp).normalize()
                        transform.localRotation = Quaternionf(dir.normalToQuaternionY())
                        transform.localPosition = globalA.add(globalB, transform.localPosition).mul(0.5)
                        invalidateBounds()
                        JomlPools.vec3d.sub(3)
                    }
                })
        }
    }

    spawnFloor(scene)
    spawnSampleCubes(scene)

    testSceneWithUI("Bridge", scene)
}

fun spawnFloor(scene: Entity) {
    Entity("Floor", scene)
        .add(MeshComponent(plane))
        .add(InfinitePlaneCollider())
        .add(StaticBody().apply { friction = 1.0 })
        .setScale(10f)
}

fun spawnSampleCubes(scene: Entity) {
    // spawn a few cubes, we can lay on top
    val numCubes = 5
    val cubeDensity = 1.0
    val cubeSize = { i: Int -> 1f / (i + 1) }
    val cubeMaterial = Material.diffuse(0x5599ff)
    val cubeMesh = flatCube.front
    for (i in 0 until numCubes) {
        val size = cubeSize(i)
        Entity("Cube[$i]", scene)
            .add(BoxCollider().apply {
                roundness = 0.1f
            })
            .add(MeshComponent(cubeMesh, cubeMaterial))
            .setScale(size * 0.5f)
            .add(DynamicBody().apply {
                friction = 0.5
                mass = cubeDensity * size.pow(3)
            })
            .setPosition(8.0, size * 0.5, (i - (numCubes - 1) * 0.5) * 3.5)
    }
}