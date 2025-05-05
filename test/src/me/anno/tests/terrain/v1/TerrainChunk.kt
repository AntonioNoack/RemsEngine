package me.anno.tests.terrain.v1

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.tests.terrain.v1.TerrainChunkSystem.Companion.sx
import me.anno.tests.terrain.v1.TerrainChunkSystem.Companion.sz
import me.anno.utils.Color.white
import me.anno.utils.types.Arrays.resize
import kotlin.math.cos
import kotlin.math.sin

class TerrainChunk(
    val xi: Int, val zi: Int
) : ProceduralMesh() {

    val dx = (xi + 0.5f) * sz
    val dz = (zi + 0.5f) * sx

    override fun generateMesh(mesh: Mesh) {
        val s = 0.1f
        RectangleTerrainModel.generateRegularQuadHeightMesh(
            sz + 1, sx + 1, false, 1f,
            mesh, { xi, zi ->
                val x = xi + dx
                val y = zi + dz
                sin(x) * sin(y) + 10f * cos(x * s) * cos(y * s)
            })
        mesh.calculateNormals(true)
        mesh.color0 = mesh.color0.resize(mesh.positions!!.size / 3)
        mesh.color0!!.fill(white)
    }
}
