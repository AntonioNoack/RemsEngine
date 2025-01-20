package me.anno.ecs.components.mesh

import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4x3d

object TransformMesh {

    fun transformMesh(mesh: Mesh, matrix: Matrix4x3d): Mesh {
        mesh.positions = transformPositions(matrix, mesh.positions, 3)
        mesh.normals = transformDirections(matrix, mesh.normals, 3)
        mesh.tangents = transformDirections(matrix, mesh.tangents, 4)
        mesh.invalidateGeometry()
        return mesh
    }

    @Suppress("SameParameterValue")
    private fun transformPositions(matrix: Matrix4x3d, src: FloatArray?, stride: Int): FloatArray? {
        src ?: return null
        val tmp = JomlPools.vec3d.borrow()
        for (i in 0 until src.size - 2 step stride) {
            tmp.set(src, i)
            matrix.transformPosition(tmp)
            tmp.get(src, i)
        }
        return src
    }

    private fun transformDirections(matrix: Matrix4x3d, src: FloatArray?, stride: Int): FloatArray? {
        src ?: return null
        val tmp = JomlPools.vec3f.borrow()
        val tmpM = JomlPools.mat3f.borrow()
            .set(matrix).normal()
        for (i in 0 until src.size - 2 step stride) {
            tmp.set(src, i)
            tmpM.transform(tmp)
            tmp.get(src, i)
        }
        return src
    }
}