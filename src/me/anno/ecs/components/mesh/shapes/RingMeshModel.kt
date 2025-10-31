package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.maths.Maths.TAUf
import me.anno.utils.types.Arrays.resize
import kotlin.math.cos
import kotlin.math.sin

object RingMeshModel {

    @JvmStatic
    fun createRingMesh(u: Int, innerRadius: Float, outerRadius: Float): Mesh {
        return createRingMesh(
            u, innerRadius, outerRadius,
            0, 1, 2,
            0f, false, Mesh()
        )
    }

    @JvmStatic
    fun createRingMesh(
        u: Int, innerRadius: Float, outerRadius: Float,
        axisCos: Int, axisSin: Int, axisNormal: Int,
        valueNormal: Float, flipFaces: Boolean, mesh: Mesh,
    ): Mesh {
        val positions = mesh.positions.resize((u + 1) * 2 * 3)
        for (i in 0..u) {
            val angle = i * TAUf / u
            for (j in 0 until 2) {
                val radius = if (j == 0) outerRadius else innerRadius
                val k = (i * 2 + (if (flipFaces) 1 - j else j)) * 3
                positions[k + axisCos] = cos(angle) * radius
                positions[k + axisSin] = sin(angle) * radius
                positions[k + axisNormal] = valueNormal
            }
        }
        mesh.positions = positions
        RectangleTerrainModel.generateQuadIndices(2, u + 1, false, mesh)
        return mesh
    }
}