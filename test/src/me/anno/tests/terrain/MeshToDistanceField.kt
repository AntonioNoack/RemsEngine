package me.anno.tests.terrain

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.MeshToDistanceField
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.image.ImageWriter
import me.anno.maths.Maths.length
import me.anno.maths.geometry.MarchingCubes
import org.joml.AABBf
import org.joml.Vector3i

fun main() {

    // todo why are the normals inaccurate???
    //  is it because we use a plane to calculate the distance??? shouldn't be...

    val originalMesh = IcosahedronModel.createIcosphere(5)
    val fieldSize = Vector3i(64)
    val s = 0.9f
    val bounds = AABBf(-s, -s, -s, s, s, s)
    val marchedMesh1 = meshToDistanceFieldToMesh(originalMesh, fieldSize, bounds)
    val marchedMesh2 = meshToDistanceFieldToMesh(marchedMesh1, fieldSize, bounds)
    val marchedMesh3 = meshToDistanceFieldToMesh(marchedMesh2, fieldSize, bounds)
    val marchedMesh4 = meshToDistanceFieldToMesh(marchedMesh3, fieldSize, bounds)

    val scene = Entity("Scene")
    Entity("Marched1", scene)
        .add(MeshComponent(marchedMesh1))
    Entity("Marched2", scene)
        .add(MeshComponent(marchedMesh2))
        .setPosition(2.0, 0.0, 0.0)
    Entity("Marched3", scene)
        .add(MeshComponent(marchedMesh3))
        .setPosition(4.0, 0.0, 0.0)
    Entity("Marched4", scene)
        .add(MeshComponent(marchedMesh4))
        .setPosition(6.0, 0.0, 0.0)

    Entity("Original", scene)
        .setPosition(-2.0, 0.0, 0.0)
        .add(MeshComponent(originalMesh))
    Entity("Baseline", scene)
        .setPosition(-4.0, 0.0, 0.0)
        .add(MeshComponent(baselineFieldToMesh(fieldSize, bounds)))

    testSceneWithUI("MeshToDistanceField", scene)
}

fun baselineFieldToMesh(fieldSize: Vector3i, bounds: AABBf): Mesh {
    val field = FloatArray(fieldSize.x * fieldSize.y * fieldSize.z)
    val x0 = (fieldSize.x - 1f) * 0.5f
    val y0 = (fieldSize.y - 1f) * 0.5f
    val z0 = (fieldSize.z - 1f) * 0.5f
    val radius = fieldSize.x / bounds.deltaX
    for (z in 0 until fieldSize.z) {
        for (y in 0 until fieldSize.y) {
            for (x in 0 until fieldSize.x) {
                field[MeshToDistanceField.getIndex(fieldSize, x, y, z)] =
                    length(x - x0, y - y0, z - z0) - radius
            }
        }
    }

    // field doesn't look good yet, print it somehow
    for (z in 0 until fieldSize.z) {
        val offset = z * fieldSize.x * fieldSize.y
        val values = field.copyOfRange(offset, offset + fieldSize.x * fieldSize.y)
        ImageWriter.writeImageFloat(fieldSize.x, fieldSize.y, "M2SDF/Layer$z.png", true, values)
    }

    val marched = MarchingCubes.march(
        fieldSize.x, fieldSize.y, fieldSize.z,
        field, 0f, bounds, false
    )

    val marchedMesh = Mesh()
    marchedMesh.positions = marched.toFloatArray()
    return marchedMesh
}

fun meshToDistanceFieldToMesh(mesh: Mesh, fieldSize: Vector3i, bounds: AABBf): Mesh {
    val field = MeshToDistanceField.meshToDistanceField(mesh, bounds, fieldSize)

    // field doesn't look good yet, print it somehow
    for (z in 0 until fieldSize.z) {
        val offset = z * fieldSize.x * fieldSize.y
        val values = field.copyOfRange(offset, offset + fieldSize.x * fieldSize.y)
        ImageWriter.writeImageFloat(fieldSize.x, fieldSize.y, "M2SDF/Layer$z.png", true, values)
    }

    val marched = MarchingCubes.march(
        fieldSize.x, fieldSize.y, fieldSize.z,
        field, 0f, bounds, false
    )

    val marchedMesh = Mesh()
    marchedMesh.positions = marched.toFloatArray()
    return marchedMesh
}