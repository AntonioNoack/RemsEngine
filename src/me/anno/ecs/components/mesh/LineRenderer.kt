package me.anno.ecs.components.mesh

import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.gpu.CullMode
import me.anno.maths.MinMax.max
import me.anno.maths.MinMax.min
import me.anno.utils.types.Arrays.resize
import org.joml.Vector3f

class LineRenderer : ProceduralMesh() {

    companion object {
        val defaultLine
            get() = listOf(
                Vector3f(0f, 0f, 0f),
                Vector3f(0f, 0f, 1f),
            )
    }

    var points: List<Vector3f> = defaultLine
        set(value) {
            field = value
            invalidateMesh()
        }

    var thickness = 0.2f
        set(value) {
            field = value
            invalidateMesh()
        }

    var up = Vector3f(0f, 1f, 0f)
        set(value) {
            field.set(value)
            invalidateMesh()
        }

    // todo optional line smoothing?
    //  would generate intermediate points on the line

    override fun generateMesh(mesh: Mesh) {
        val up = up
        val points = points
        val thickness = thickness
        val positions = mesh.positions.resize(points.size * 6)
        val dir = Vector3f()
        val tmp = Vector3f()
        for (i in points.indices) {
            points[min(i + 1, points.lastIndex)]
                .sub(points[max(i - 1, 0)], dir)
                .cross(up).safeNormalize(thickness)
            points[i].add(dir, tmp).get(positions, i * 6)
            points[i].sub(dir, tmp).get(positions, i * 6 + 3)
        }
        RectangleTerrainModel.generateQuadIndices(2, points.size, false, mesh)
        mesh.positions = positions
        mesh.calculateNormals(true)
    }
}