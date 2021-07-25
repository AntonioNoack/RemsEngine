package me.anno.mesh.vox.model

import me.anno.ecs.components.mesh.Mesh
import me.anno.mesh.vox.meshing.BakeMesh
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.VoxelMeshBuildInfo
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList

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

    fun createMesh(palette: IntArray): Mesh {

        // create a mesh
        // merge voxels of the same color
        // todo only create for a certain material (e.g. same glossiness, different color, same reflectivity, ...)

        // idea: create textures for large, flat sections
        // could increase performance massively
        // probably complicated to implement -> skip for now

        val mesh = Mesh()
        val vertices = FloatArrayList(512)
        val colors = IntArrayList(512)
        val normals = FloatArrayList(512)

        val info = VoxelMeshBuildInfo(palette, vertices, colors, normals)

        // go over all six directions
        // just reuse our old code for minecraft like stuff
        for (side in BlockSide.values) {
            info.setNormal(side)
            BakeMesh.bakeMesh(this, side, info)
        }

        mesh.positions = vertices.toFloatArray()
        mesh.normals = normals.toFloatArray()
        mesh.color0 = colors.toIntArray()

        return mesh

    }

}