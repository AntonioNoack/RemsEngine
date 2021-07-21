package me.anno.mesh.vox.meshing

import org.joml.Vector3i

object BlockBuffer {

    fun addQuad(
        base: VoxelMeshBuildInfo,
        a: Vector3i,
        b: Vector3i,
        c: Vector3i,
        d: Vector3i
    ) {
        base.add(a)
        base.add(c)
        base.add(b)
        base.add(a)
        base.add(d)
        base.add(c)
    }

    fun addQuad(
        base: VoxelMeshBuildInfo,
        side: BlockSide,
        dx: Int, dy: Int, dz: Int
    ) {
        when (side) {
            BlockSide.NX -> {
                addQuad(
                    base,
                    Vector3i(0, 0, 0),
                    Vector3i(0, dy, 0),
                    Vector3i(0, dy, dz),
                    Vector3i(0, 0, dz)
                )
            }
            BlockSide.PX -> {
                addQuad(
                    base,
                    Vector3i(dx, dy, 0),
                    Vector3i(dx, 0, 0),
                    Vector3i(dx, 0, dz),
                    Vector3i(dx, dy, dz)
                )
            }
            BlockSide.NY -> {
                addQuad(
                    base,
                    Vector3i(0, 0, 0),
                    Vector3i(0, 0, dz),
                    Vector3i(dx, 0, dz),
                    Vector3i(dx, 0, 0)
                )
            }
            BlockSide.PY -> {
                addQuad(
                    base,
                    Vector3i(0, dy, dz),
                    Vector3i(0, dy, 0),
                    Vector3i(dx, dy, 0),
                    Vector3i(dx, dy, dz)
                )
            }
            BlockSide.NZ -> {
                addQuad(
                    base,
                    Vector3i(0, dy, 0),
                    Vector3i(0, 0, 0),
                    Vector3i(dx, 0, 0),
                    Vector3i(dx, dy, 0)
                )
            }
            BlockSide.PZ -> {
                addQuad(
                    base,
                    Vector3i(0, 0, dz),
                    Vector3i(0, dy, dz),
                    Vector3i(dx, dy, dz),
                    Vector3i(dx, 0, dz)
                )
            }
        }
    }

}