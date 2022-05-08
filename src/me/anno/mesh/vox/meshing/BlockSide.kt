package me.anno.mesh.vox.meshing

enum class BlockSide(
    val id: Int,
    val x: Int, val y: Int, val z: Int
) {

    NX(0, -1, 0, 0),
    PX(1, +1, 0, 0),
    NY(2, 0, -1, 0),
    PY(3, 0, +1, 0),
    NZ(4, 0, 0, -1),
    PZ(5, 0, 0, +1);

    companion object {
        val values = values()
    }
}