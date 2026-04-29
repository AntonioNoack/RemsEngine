package me.anno.experiments.ocean

import me.anno.ecs.Entity
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.PIf
import me.anno.utils.OS.documents
import org.joml.Quaternionf
import org.joml.Vector3f

fun main() {
    val size = 512
    val halfSize = (size - 1f) * 0.5f
    val mesh = RectangleTerrainModel.generateRegularQuadHeightMesh(
        size, size, false, 1f,
        Mesh(), true
    )

    val scene = Entity()

    val oceanMaterial = OceanMaterial().apply {
        val sizeF = halfSize - 5f // 5f, because we have 0.2x falloff
        waveBounds.set(
            -sizeF, 0f, -sizeF,
            +sizeF, 0f, +sizeF
        )
    }

    // todo create a similar island mesh for testing, just without that large of a file
    Entity("Ocean", scene)
        .add(MeshComponent(mesh, oceanMaterial).apply { castReflections = false })
        .add(MeshComponent(createCutoutPlane(halfSize, 5f), oceanMaterial).apply { castReflections = false })

    Entity("Island", scene)
        .add(MeshComponent(documents.getChild("Island0.fbx"), Material.diffuse(0x333333)))
        .setScale(4f, 2f, 4f)

    Entity("Ocean Floor", scene)
        .add(MeshComponent(DefaultAssets.plane, Material.diffuse(0x333333)))
        .setPosition(0.0, -50.0, 0.0)
        .setScale(halfSize)

    Entity("ReflectionPlane", scene)
        .add(PlanarReflection())
        .setRotationDegrees(-90f, 0f, 0f)
        .setScale(halfSize)

    testSceneWithUI("Ocean", scene)
}

// place plane with cutout around the main water plane...
fun createCutoutPlane(s: Float, x: Float): Mesh {
    val positions = FloatArray(3 * 3 * 2 * 4)
    var k = 0

    val r = Quaternionf()
    val v = Vector3f()
    val t = x * s
    repeat(4) {
        v.set(-s, 0f, -s).rotate(r).get(positions, k)
        v.set(-t, 0f, -t).rotate(r).get(positions, k + 3)
        v.set(+t, 0f, -t).rotate(r).get(positions, k + 6)

        v.set(+s, 0f, -s).rotate(r).get(positions, k + 9)
        v.set(-s, 0f, -s).rotate(r).get(positions, k + 12)
        v.set(+t, 0f, -t).rotate(r).get(positions, k + 15)

        k += 18

        r.rotateY(PIf * 0.5f)
    }

    val mesh = Mesh()
    mesh.positions = positions
    return mesh
}