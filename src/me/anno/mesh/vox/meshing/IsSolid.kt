package me.anno.mesh.vox.meshing

fun interface IsSolid {
    fun test(x: Int, y: Int, z: Int): Boolean
}