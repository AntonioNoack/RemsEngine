package me.anno.mesh.vox.meshing

fun interface BlockCallback {
    fun process(x: Int, y: Int, z: Int)
}