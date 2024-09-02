package me.anno.ecs.components.mesh

import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4x3d

object TransformMesh {

    fun transformMesh(mesh: Mesh, matrix: Matrix4x3d): Mesh {
        mesh.positions = transformPositions(matrix, mesh.positions)
        mesh.normals = transformDirections(matrix, mesh.normals, 3)
        mesh.tangents = transformDirections(matrix, mesh.tangents, 4)
        mesh.invalidateGeometry()
        return mesh
    }

    private fun transformPositions(matrix: Matrix4x3d, src: FloatArray?): FloatArray? {
        src ?: return null
        val tmp = JomlPools.vec3f.borrow()
        for (i in src.indices step 3) {
            tmp.set(src, i)
            matrix.transformPosition(tmp)
            tmp.get(src, i)
        }
        return src
    }

    private fun transformDirections(matrix: Matrix4x3d, src: FloatArray?, stride: Int): FloatArray? {
        src ?: return null
        val tmp = JomlPools.vec3f.borrow()
        for (i in src.indices step stride) {
            tmp.set(src, i)
            matrix.transformDirection(tmp)
                .safeNormalize()
            tmp.get(src, i)
        }
        return src
    }
}