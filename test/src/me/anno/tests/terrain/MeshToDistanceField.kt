package me.anno.tests.terrain

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.MeshToDistanceField
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.image.ImageWriter
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mix
import me.anno.maths.geometry.DualContouring3d
import me.anno.maths.geometry.MarchingCubes
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.floor
import kotlin.math.max

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
    val marchedMesh5 = meshToDistanceFieldToMesh(marchedMesh4, fieldSize, bounds)
    val marchedMesh6 = meshToDistanceFieldToMesh(marchedMesh5, fieldSize, bounds)
    val marchedMesh7 = meshToDistanceFieldToMesh(marchedMesh6, fieldSize, bounds)

    // todo can we polish the mesh by the original normals???
    //  I don't think this would help much...

    // todo try out two overlapping spheres, how they are merged. Probably broken :/

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
    Entity("Marched5", scene)
        .add(MeshComponent(marchedMesh5))
        .setPosition(8.0, 0.0, 0.0)
    Entity("Marched6", scene)
        .add(MeshComponent(marchedMesh6))
        .setPosition(10.0, 0.0, 0.0)
    Entity("Marched7", scene)
        .add(MeshComponent(marchedMesh7))
        .setPosition(12.0, 0.0, 0.0)

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
    if (false) for (z in 0 until fieldSize.z) {
        val offset = z * fieldSize.x * fieldSize.y
        val values = field.copyOfRange(offset, offset + fieldSize.x * fieldSize.y)
        ImageWriter.writeImageFloat(fieldSize.x, fieldSize.y, "M2SDF/Layer$z.png", true, values)
    }

    val marchedMesh = Mesh()
    marchedMesh.positions = if (true) {
        MarchingCubes.march(
            fieldSize.x, fieldSize.y, fieldSize.z,
            field, 0f, bounds, false
        ).toFloatArray()
    } else {
        val layers = (0 until fieldSize.z).map { z ->
            FloatImage(
                fieldSize.x, fieldSize.y, 1,
                field, fieldSize.x * fieldSize.y * z, fieldSize.x
            )
        }
        // todo this is completely broken :(, why???
        DualContouring3d.contour3d(
            fieldSize.x, fieldSize.y, fieldSize.z,
            { x, y, z ->
                val z0 = clamp(floor(z), 0f, fieldSize.z - 2f)
                val zi = z0.toInt()
                val v0 = layers[zi].getValue(x, y, 0)
                val v1 = layers[zi + 1].getValue(x, y, 0)
                mix(v0, v1, z - z0)
            }
        ).flatten(fieldSize, bounds)
    }
    return marchedMesh
}

fun List<Vector3f>.flatten(fieldSize: Vector3i, bounds: AABBf): FloatArray {
    val x0 = bounds.minX
    val y0 = bounds.minY
    val z0 = bounds.minZ
    val dx = bounds.deltaX / max(fieldSize.x - 1, 1)
    val dy = bounds.deltaY / max(fieldSize.y - 1, 1)
    val dz = bounds.deltaZ / max(fieldSize.z - 1, 1)
    val positions = FloatArray(size * 3)
    var k = 0
    for (i in indices) {
        val value = this[i]
        positions[k++] = value.x * dx + x0
        positions[k++] = value.y * dy + y0
        positions[k++] = value.z * dz + z0
    }
    return positions
}