package me.anno.mesh.vox.meshing

fun interface GetBlockId {
    fun getBlockId(x: Int, y: Int, z: Int): Int
}