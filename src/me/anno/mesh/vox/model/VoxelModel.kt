package me.anno.mesh.vox.model

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.prefab.Prefab
import me.anno.mesh.vox.meshing.BakeMesh
import me.anno.mesh.vox.meshing.BakeMesh.getColors
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.GetBlockId
import me.anno.mesh.vox.meshing.NeedsFace
import me.anno.mesh.vox.meshing.VoxelMeshBuilder
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import kotlin.math.max

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
        repeat(sizeX) { x ->
            repeat(sizeY) { y ->
                var i = getIndex(x, y, 0)
                if (palette != null) {
                    repeat(sizeZ) { z ->
                        dst[i] = palette[getBlock(x, y, z)]
                        i += dz
                    }
                } else {
                    repeat(sizeZ) { z ->
                        dst[i] = getBlock(x, y, z)
                        i += dz
                    }
                }
            }
        }
    }

    open fun fill(dst: IntArray) {
        var i = 0
        repeat(sizeX) { x ->
            repeat(sizeY) { y ->
                repeat(sizeZ) { z ->
                    dst[i++] = getBlock(x, y, z)
                }
            }
        }
    }

    open fun countBlocks(): Int {
        var i = 0
        repeat(sizeX) { x ->
            repeat(sizeY) { y ->
                repeat(sizeZ) { z ->
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
        outsideIsSolid: GetBlockId?,
        needsFace: NeedsFace?,
        dst: Mesh = Mesh()
    ) = createMesh(palette, outsideIsSolid, needsFace, BlockSide.entries, dst)

    fun createBuilder(vertexPointGuess: Int, palette: IntArray?): VoxelMeshBuilder {
        val vertices = FloatArrayList(vertexPointGuess)
        val colors = IntArrayList(vertexPointGuess / 3 + 1)
        val normals = FloatArrayList(vertexPointGuess)
        return VoxelMeshBuilder(palette, vertices, colors, normals)
    }

    /**
     * Creates an optimized triangle mesh in voxel shape.
     *
     * @param insideBlocks whether a cell needs a wall as border; if null, color != 0 is solid
     * @param outsideBlocks whether a cell outside the box needs a wall as border; if null, all outside blocks get walls
     * */
    fun createMesh(
        palette: IntArray?,
        outsideBlocks: GetBlockId?,
        needsFace: NeedsFace?,
        sides: List<BlockSide>,
        dst: Mesh = Mesh()
    ): Mesh {

        // create a mesh
        // merge voxels of the same color

        // todo support multiple-materials with glossiness and metallic for VOX-reader
        // only create for a certain material (e.g., same glossiness, different color, same reflectivity, ...)

        // idea: create textures for large, flat sections
        // could increase performance massively
        // probably complicated to implement -> skip for now

        // guess the number of required points
        val vertexPointGuess = max(countBlocks() * 3, 18)
        val builder = createBuilder(vertexPointGuess, palette)

        // go over all six directions
        // just reuse our old code for minecraft like stuff

        val colors = getColors(this, builder)
        val insideBlocks1 = GetBlockId(this::getBlock)
        for (si in sides.indices) {
            val side = sides[si]
            // an estimate
            BakeMesh.bakeMesh(this, side, builder, insideBlocks1, outsideBlocks, needsFace, colors)
            builder.finishSide(side)
        }

        dst.positions = builder.vertices.toFloatArray()
        dst.normals = builder.normals?.toFloatArray()
        dst.color0 = builder.colors?.toIntArray()
        dst.invalidateGeometry()

        return dst
    }

    fun createMeshPrefab(palette: IntArray): Prefab {

        val mesh = createMesh(palette, null, null)
        val prefab = Prefab("Mesh")

        prefab._sampleInstance = mesh
        prefab["positions"] = mesh.positions
        prefab["normals"] = mesh.normals
        prefab["colors0"] = mesh.color0

        return prefab
    }
}