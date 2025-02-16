package me.anno.tests.mesh.spline

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.spline.SplineControlPoint
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.spline.SplineProfile
import me.anno.ecs.components.mesh.spline.SplineSpawner
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.utils.Color.black
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector2f
import org.joml.Vector3d
import kotlin.math.PI

fun main() {

    OfficialExtensions.initForTests()
    val cone = getReference("G:/Assets/Quaternius/Public Transport.zip/TrafficCone.fbx")
    val fence = getReference("G:/Assets/Quaternius/Farm Buildings.zip/Fence.fbx")
    val trees = (1..4).map {
        getReference("G:/Assets/Quaternius/Ultimate Nature.zip/PineTree_$it.fbx")
    }
    val coneMaterial = Material.diffuse(0xff9900)

    val streetProfile = SplineProfile(
        listOf(
            Vector2f(-7f, -0.49f),
            Vector2f(-4.81f, +0.3f),
            Vector2f(-4.8f, +0.6f),
            Vector2f(-3.31f, +0.6f),
            Vector2f(-3.3f, +0.5f),
            Vector2f(+3.3f, +0.5f), // dark
            Vector2f(+3.31f, +0.6f), // light
            Vector2f(+4.8f, +0.6f), // light
            Vector2f(+4.81f, +0.3f), // green
            Vector2f(+7f, -0.49f), // green
        ), null,
        IntArrayList(
            intArrayOf(
                0x77dd77 or black,
                0x77dd77 or black,
                0xaaaaaa or black,
                0xaaaaaa or black,
                0x555555 or black,
                0x555555 or black,
                0xaaaaaa or black,
                0xaaaaaa or black,
                0x77dd77 or black,
                0x77dd77 or black,
            )
        ), false
    )

    val scene = Entity("Scene")
    val spline = Entity("SplineMesh", scene)
    fun addPoint(v: Vector3d, a: Double) {
        Entity("Pt[${spline.children.size}]", spline)
            .setPosition(v)
            .setRotation(0f, a.toRadians().toFloat(), 0f)
            .add(SplineControlPoint())
    }
    addPoint(Vector3d(5.0, 0.0, 10.0), 90.0)
    addPoint(Vector3d(-5.0, 0.0, 0.0), 0.0)

    // todo fences should (optionally! - not trees) rotate to the surface on xz-level, too

    spline.add(SplineMesh().apply {
        profile = streetProfile
    })

    // test cones
    spline.add(SplineSpawner().apply {
        meshFiles = listOf(cone)
        materialOverride = coneMaterial.ref
        useCenterLength = true
        spacing = 10.0
        offsetX = +2.5
        offsetY = 0.53
    })
    spline.add(SplineSpawner().apply {
        meshFiles = listOf(cone)
        materialOverride = coneMaterial.ref
        useCenterLength = true
        spacing = 10.0
        offsetX = -2.5
        offsetY = 0.53
    })

    // test fences
    spline.add(SplineSpawner().apply {
        meshFiles = listOf(fence)
        rotation = PI / 2
        // useCenterLength = true
        scaleIfNeeded = true
        spacing = 6.0
        offsetX = -4.5
        offsetY = 0.53
    })

    spline.add(SplineSpawner().apply {
        meshFiles = listOf(fence)
        rotation = PI / 2
        // useCenterLength = true
        scaleIfNeeded = true
        spacing = 6.0
        offsetX = +4.5
        offsetY = 0.53
    })

    spline.add(SplineSpawner().apply {
        meshFiles = trees
        scaleIfNeeded = false
        randomRotations = true
        alwaysUp = true
        spawnChance = 0.7f
        spacing = 3.0
        offsetX = 6.2
        offsetY = -0.3
    })

    spline.add(SplineSpawner().apply {
        meshFiles = trees
        scaleIfNeeded = false
        randomRotations = true
        alwaysUp = true
        spawnChance = 0.7f
        spacing = 3.0
        offsetX = -6.2
        offsetY = -0.3
    })

    testSceneWithUI("SplineSpawner", scene)
}