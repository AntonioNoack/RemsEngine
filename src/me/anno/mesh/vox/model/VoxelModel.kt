package me.anno.mesh.vox.model

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Prefab
import me.anno.mesh.vox.meshing.BakeMesh
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.IsSolid
import me.anno.mesh.vox.meshing.VoxelMeshBuilder
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.f2
import me.anno.utils.types.Floats.roundToIntOr
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.roundToInt

abstract class VoxelModel(val sizeX: Int, val sizeY: Int, val sizeZ: Int) {

    var centerX = sizeX * 0.5f
    var centerY = sizeY * 0.5f
    var centerZ = sizeZ * 0.5f

    val size = sizeX * sizeY * sizeZ

    fun center0() {
        centerX = 0f
        centerY = 0f
        centerZ = 0f
    }

    open fun getIndex(x: Int, y: Int, z: Int) = (x * sizeY + y) * sizeZ + z

    // if outside the model, must return 0
    abstract fun getBlock(x: Int, y: Int, z: Int): Int

    open fun fill(palette: IntArray?, dst: IntArray) {
        val dz = getIndex(0, 0, 1)
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                var i = getIndex(x, y, 0)
                if (palette != null) {
                    for (z in 0 until sizeZ) {
                        dst[i] = palette[getBlock(x, y, z)]
                        i += dz
                    }
                } else {
                    for (z in 0 until sizeZ) {
                        dst[i] = getBlock(x, y, z)
                        i += dz
                    }
                }
            }
        }
    }

    open fun fill(dst: IntArray) {
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
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                for (z in 0 until sizeZ) {
                    if (getBlock(x, y, z) != 0) i++
                }
            }
        }
        return i
    }

    /**
     * Creates an optimized triangle mesh in voxel shape.
     *
     * @param insideIsSolid whether a cell needs a wall as border; if null, color != 0 is solid
     * @param outsideIsSolid whether a cell outside the box needs a wall as border; if null, all outside blocks get walls
     * */
    fun createMesh(
        palette: IntArray?,
        insideIsSolid: IsSolid?,
        outsideIsSolid: IsSolid?,
        dst: Mesh = Mesh()
    ) = createMesh(palette, insideIsSolid, outsideIsSolid, allSides, dst)

    /**
     * Creates an optimized triangle mesh in voxel shape.
     *
     * @param insideIsSolid whether a cell needs a wall as border; if null, color != 0 is solid
     * @param outsideIsSolid whether a cell outside the box needs a wall as border; if null, all outside blocks get walls
     * */
    @Suppress("unused")
    fun createMesh(
        palette: IntArray?,
        insideIsSolid: IsSolid?,
        outsideIsSolid: IsSolid?,
        side: BlockSide,
        dst: Mesh = Mesh()
    ) = createMesh(palette, insideIsSolid, outsideIsSolid, sideList[side.ordinal], dst)

    /**
     * Creates an optimized triangle mesh in voxel shape.
     *
     * @param insideIsSolid whether a cell needs a wall as border; if null, color != 0 is solid
     * @param outsideIsSolid whether a cell outside the box needs a wall as border; if null, all outside blocks get walls
     * */
    fun createMesh(
        palette: IntArray?,
        insideIsSolid: IsSolid?,
        outsideIsSolid: IsSolid?,
        sides: List<BlockSide>,
        dst: Mesh = Mesh()
    ): Mesh {

        // create a mesh
        // merge voxels of the same color
        // todo only create for a certain material (e.g., same glossiness, different color, same reflectivity, ...)

        // idea: create textures for large, flat sections
        // could increase performance massively
        // probably complicated to implement -> skip for now

        // guess the number of required points
        val vertexPointGuess = max(countBlocks() * 3, 18)

        val vertices = FloatArrayList(vertexPointGuess)
        val colors = IntArrayList(vertexPointGuess / 3 + 1)
        val normals = FloatArrayList(vertexPointGuess)

        val builder = VoxelMeshBuilder(palette, vertices, colors, normals)

        // go over all six directions
        // just reuse our old code for minecraft like stuff
        var removed = 0f

        for (side in sides) {
            // an estimate
            removed += BakeMesh.bakeMesh(this, side, builder, insideIsSolid, outsideIsSolid)
            builder.finishSide(side)
        }

        if (printReduction && removed > 0) {
            val numberOfBlocks = countBlocks()
            val triangleCount = vertices.size / 9
            removed = removed * 100f / 6f
            LOGGER.info(
                "" +
                        "Removed ${removed.roundToIntOr()}% of $numberOfBlocks blocks, " +
                        "created $triangleCount triangles, ${(triangleCount.toFloat() / numberOfBlocks).f2()}/block"
            )
        }

        dst.positions = vertices.toFloatArray()
        dst.normals = normals.toFloatArray()
        dst.color0 = colors.toIntArray()

        return dst
    }

    fun createMeshPrefab(palette: IntArray): Prefab {

        val mesh = createMesh(palette, null, null)
        val prefab = Prefab("Mesh")

        prefab._sampleInstance = mesh
        prefab["positions"] = mesh.positions
        prefab["normals"] = mesh.normals
        prefab["color0"] = mesh.color0

        return prefab
    }

    companion object {
        var printReduction = false
        private val allSides = BlockSide.entries
        private val sideList = allSides.map { listOf(it) }
        private val LOGGER = LogManager.getLogger(VoxelModel::class)
    }
}