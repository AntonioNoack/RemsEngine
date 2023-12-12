package me.anno.tests.physics.constraints

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.bullet.constraints.PointConstraint
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * using bullet constraints, build a hanging bridge, and a few objects on top for testing
 * */
fun main() {

    ECSRegistry.init()

    val scene = Entity("Scene")
    scene.add(BulletPhysics().apply {
        updateInEditMode = true
    })

    val numBars = 10
    val numLinks = 3
    val linkSpacing = 0.35

    val barSize = Vector3d(1.2, 0.1, 0.35)
    val barSpacing = 0.1
    val density = 0.5

    val height = 3.0
    val pillarSize = Vector3d(1.5, height, 1.5)

    val barMesh = flatCube.scaled(Vector3f(barSize).mul(0.5f)).front
    val pillarMesh = flatCube.scaled(Vector3f(pillarSize).mul(0.5f)).front

    val toLink = ArrayList<Entity>()

    fun addPillar(i: Int, sign: Int) {
        val entity = Entity("Pillar[$sign]", scene)
        val z = (i - (numBars - 1) * 0.5) * (barSpacing + barSize.z) + sign * 0.5 * (pillarSize.z - barSize.z)
        entity.setPosition(0.0, pillarSize.y * 0.5, z)
        entity.add(BoxCollider().apply {
            halfExtends.set(pillarSize).mul(0.5)
            margin = 0.0
        })
        entity.add(MeshComponent(pillarMesh))
        entity.add(Rigidbody().apply {
            friction = 1.0
        })
        toLink.add(entity)
    }

    addPillar(-1, -1)

    for (i in 0 until numBars) {
        val entity = Entity("Bar[$i]", scene)
        val z = (i - (numBars - 1) * 0.5) * (barSpacing + barSize.z)
        entity.setPosition(0.0, height - barSize.y * 0.5, z)
        entity.add(BoxCollider().apply {
            halfExtends.set(barSize).mul(0.5)
            margin = 0.0
        })
        entity.add(MeshComponent(barMesh))
        entity.add(Rigidbody().apply {
            mass = density * barSize.x * barSize.y * barSize.z
            friction = 0.5
        })
        toLink.add(entity)
    }

    addPillar(numBars, +1)

    // add all links
    val linkY = height - barSize.y * 0.5
    for (i in 1 until toLink.size) {
        val a = toLink[i - 1]
        val b = toLink[i]
        for (j in 0 until numLinks) {
            val link = PointConstraint()
            link.disableCollisionsBetweenLinked = false
            link.other = b.getComponent(Rigidbody::class)
            link.lerpingSpeed = 0.5
            a.add(link)
            val z = ((i - 1.5) - (numBars - 1) * 0.5) * (barSpacing + barSize.z)
            val x = (j - (numLinks - 1) * 0.5) * linkSpacing
            val fakeSpacing = barSpacing * 0.9
            link.selfPosition.set(x, linkY - a.position.y, z - a.position.z + fakeSpacing * 0.5)
            link.otherPosition.set(x, linkY - b.position.y, z - b.position.z - fakeSpacing * 0.5)
        }
    }

    // spawn a few cubes, we can lay on top
    val numCubes = 5
    val cubeDensity = 1.0
    val cubeSize = { i: Int -> 1f / (i + 1) }
    for (i in 0 until numCubes) {
        val size = cubeSize(i)
        val cube = Entity("Cube[$i]", scene)
        cube.add(BoxCollider().apply {
            margin = 0.0
            halfExtends.set(size * 0.5)
        })
        cube.add(MeshComponent(flatCube.scaled(size * 0.5f).front))
        cube.add(Rigidbody().apply {
            friction = 0.5
            mass = cubeDensity * size * size * size
        })
        cube.setPosition(8.0, size * 0.5, (i - (numCubes - 1) * 0.5) * 3.5)
    }

    val floor = Entity("Floor", scene)
    floor.add(BoxCollider().apply {
        margin = 0.0
    })
    floor.add(MeshComponent(flatCube.front))
    floor.add(Rigidbody().apply { friction = 1.0 })
    floor.setScale(10.0)
    floor.setPosition(0.0, -10.0, 0.0)

    testSceneWithUI("Bridge", scene)
}