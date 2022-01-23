package me.anno.mesh.vox.model

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Prefab
import me.anno.mesh.vox.meshing.BakeMesh
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.VoxelMeshBuildInfo
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Floats.f2
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.roundToInt

abstract class VoxelModel(val sizeX: Int, val sizeY: Int, val sizeZ: Int) {

    val centerX = sizeX * 0.5f
    val centerY = sizeY * 0.5f
    val centerZ = sizeZ * 0.5f

    val size = sizeX * sizeY * sizeZ

    open fun getIndex(x: Int, y: Int, z: Int) = (x * sizeY + y) * sizeZ + z

    // if outside the model, must return 0
    abstract fun getBlock(x: Int, y: Int, z: Int): Byte

    open fun fill(palette: IntArray, dst: IntArray) {
        var i = 0
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                for (z in 0 until sizeZ) {
                    dst[i++] = palette[getBlock(x, y, z).toInt() and 255]
                }
            }
        }
    }

    open fun fill(dst: ByteArray) {
        var i = 0
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                for (z in 0 until sizeZ) {
                    dst[i++] = getBlock(x, y, z)
                }
            }
        }
    }

    open fun countBlocks(): Int {
        var i = 0
        val zero = 0.toByte()
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                for (z in 0 until sizeZ) {
                    if (getBlock(x, y, z) != zero) i++
                }
            }
        }
        return i
    }

    fun createMesh(
        palette: IntArray,
        outsideIsSolid: ((x: Int, y: Int, z: Int) -> Boolean)?,
        mesh: Mesh = Mesh()
    ): Mesh {

        // create a mesh
        // merge voxels of the same color
        // todo only create for a certain material (e.g. same glossiness, different color, same reflectivity, ...)

        // idea: create textures for large, flat sections
        // could increase performance massively
        // probably complicated to implement -> skip for now

        // guess the number of required points
        val vertexPointGuess = max(size * 6, 18)

        val vertices = ExpandingFloatArray(vertexPointGuess)
        val colors = ExpandingIntArray(vertexPointGuess / 3 + 1)
        val normals = ExpandingFloatArray(vertexPointGuess)

        val info = VoxelMeshBuildInfo(palette, vertices, colors, normals)

        // go over all six directions
        // just reuse our old code for minecraft like stuff
        var removed = 0f
        for (side in BlockSide.values) {
            info.setNormal(side)
            // an estimate
            removed += BakeMesh.bakeMesh(this, side, info, outsideIsSolid)
        }
        removed = removed * 100f / 6f
        val numberOfBlocks = countBlocks()
        val triangleCount = vertices.size / 9
        if (numberOfBlocks > 0) {
            LOGGER.info(
                "" +
                        "Removed ${removed.roundToInt()}% of $numberOfBlocks blocks, " +
                        "created $triangleCount triangles, ${(triangleCount.toFloat() / numberOfBlocks).f2()}/block"
            )
        }

        mesh.positions = vertices.toFloatArray()
        mesh.normals = normals.toFloatArray()
        mesh.color0 = colors.toIntArray()

        return mesh

    }

    fun createMeshPrefab(palette: IntArray): Prefab {

        val mesh = createMesh(palette, null)
        val prefab = Prefab("Mesh")

        prefab.setProperty("positions", mesh.positions)
        prefab.setProperty("normals", mesh.normals)
        prefab.setProperty("color0", mesh.color0)

        return prefab

    }

    companion object {
        private val LOGGER = LogManager.getLogger(VoxelModel::class)
    }

}