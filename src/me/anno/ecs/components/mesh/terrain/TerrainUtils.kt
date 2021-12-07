package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.components.mesh.Mesh
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3f

object TerrainUtils {

    fun generateRegularQuadHeightMesh(
        width: Int,
        height: Int,
        offset: Int,
        stride: Int,
        cellSizeMeters: Float,
        mesh: Mesh,
        getHeight: (index: Int) -> Float,
        getNormal: (x: Int, y: Int, i: Int, dst: Vector3f) -> Unit,
        getColor: (index: Int) -> Int
    ) {

        ////////////////////////////////
        // generate indices for faces //
        ////////////////////////////////

        var k = 0
        val indexCount = (width - 1) * (height - 1) * 6
        var indices = mesh.indices
        if (indices?.size != indexCount) {
            indices = IntArray(indexCount)
            mesh.indices = indices
        }
        for (y in 0 until height - 1) {
            var i00 = y * width
            for (x in 0 until width - 1) {

                val i01 = i00 + 1
                val i10 = i00 + width
                val i11 = i01 + width

                indices[k++] = i00
                indices[k++] = i10
                indices[k++] = i11

                indices[k++] = i11
                indices[k++] = i01
                indices[k++] = i00

                i00++
            }
        }

        ///////////////////////
        // generate vertices //
        ///////////////////////

        val vertexCount = width * height
        var vertices = mesh.positions
        var normals = mesh.normals
        if (vertices?.size != vertexCount * 3 || normals == null) {
            vertices = FloatArray(vertexCount * 3)
            normals = FloatArray(vertexCount * 3)
            mesh.positions = vertices
            mesh.normals = normals
        }

        var colors = mesh.color0
        if (colors?.size != vertexCount) {
            colors = IntArray(vertexCount)
            mesh.color0 = colors
        }

        // center mesh
        val centerX = width * 0.5f
        val centerY = height * 0.5f
        var j = 0
        var l = 0

        // define mesh positions
        val normal = JomlPools.vec3f.borrow()
        for (y in 0 until height) {
            var i = y * stride + offset
            for (x in 0 until width) {
                getNormal(x, y, i, normal)
                normals[j] = normal.x
                vertices[j++] = (x - centerX) * cellSizeMeters
                normals[j] = normal.y
                vertices[j++] = getHeight(i)
                normals[j] = normal.z
                vertices[j++] = (y - centerY) * cellSizeMeters
                colors[l++] = getColor(i)
                i++
            }
        }

        mesh.invalidateGeometry()

    }


}