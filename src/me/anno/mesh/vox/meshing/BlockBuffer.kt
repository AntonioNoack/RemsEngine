package me.anno.mesh.vox.meshing

object BlockBuffer {

    fun addQuad(
        base: VoxelMeshBuilder,
        ax: Int, ay: Int, az: Int,
        bx: Int, by: Int, bz: Int,
        cx: Int, cy: Int, cz: Int,
        dx: Int, dy: Int, dz: Int
    ) {
        base.add(ax, ay, az)
        base.add(cx, cy, cz)
        base.add(bx, by, bz)
        base.add(ax, ay, az)
        base.add(dx, dy, dz)
        base.add(cx, cy, cz)
    }

    fun addQuad(
        base: VoxelMeshBuilder,
        side: BlockSide,
        dx: Int, dy: Int, dz: Int
    ) {
        when (side) {
            BlockSide.NX -> {
                addQuad(
                    base,
                    0, 0, 0,
                    0, dy, 0,
                    0, dy, dz,
                    0, 0, dz
                )
            }
            BlockSide.PX -> {
                addQuad(
                    base,
                    dx, dy, 0,
                    dx, 0, 0,
                    dx, 0, dz,
                    dx, dy, dz
                )
            }
            BlockSide.NY -> {
                addQuad(
                    base,
                    0, 0, 0,
                    0, 0, dz,
                    dx, 0, dz,
                    dx, 0, 0
                )
            }
            BlockSide.PY -> {
                addQuad(
                    base,
                    0, dy, dz,
                    0, dy, 0,
                    dx, dy, 0,
                    dx, dy, dz
                )
            }
            BlockSide.NZ -> {
                addQuad(
                    base,
                    0, dy, 0,
                    0, 0, 0,
                    dx, 0, 0,
                    dx, dy, 0
                )
            }
            BlockSide.PZ -> {
                addQuad(
                    base,
                    0, 0, dz,
                    0, dy, dz,
                    dx, dy, dz,
                    dx, 0, dz
                )
            }
        }
    }

}