package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.components.mesh.Mesh
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import org.joml.Vector3f

object TerrainUtils {

    interface ColorMap {
        operator fun get(index: Int): Int
    }

    interface HeightMap {
        operator fun get(index: Int): Float
    }

    interface NormalMap {
        fun get(x: Int, y: Int, i: Int, dst: Vector3f)
    }

    fun generateRegularQuadHeightMesh(
        width: Int,
        height: Int,
        offset: Int,
        stride: Int,
        flipY: Boolean,
        cellSizeMeters: Float,
        mesh: Mesh,
        heightMap: HeightMap,
        normalMap: NormalMap,
        colorMap: ColorMap
    ) {

        ////////////////////////////////
        // generate indices for faces //
        ////////////////////////////////

        var k = 0
        val indexCount = (width - 1) * (height - 1) * 6
        val indices = mesh.indices.resize(indexCount)
        for (y in 0 until height - 1) {
            var i00 = y * width
            for (x in 0 until width - 1) {

                val i01 = i00 + 1
                val i10 = i00 + width
                val i11 = i01 + width

                if (flipY) {

                    indices[k++] = i00
                    indices[k++] = i11
                    indices[k++] = i10

                    indices[k++] = i01
                    indices[k++] = i11
                    indices[k++] = i00

                } else {

                    indices[k++] = i00
                    indices[k++] = i10
                    indices[k++] = i11

                    indices[k++] = i11
                    indices[k++] = i01
                    indices[k++] = i00

                }

                i00++
            }
        }

        ///////////////////////
        // generate vertices //
        ///////////////////////

        val vertexCount = width * height
        val numCoords = vertexCount * 3
        val vertices = mesh.positions.resize(numCoords)
        val normals = mesh.normals.resize(numCoords)
        val colors = mesh.color0.resize(vertexCount)

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
                normalMap.get(x, y, i, normal)
                normals[j] = normal.x
                vertices[j++] = (x - centerX) * cellSizeMeters
                normals[j] = normal.y
                vertices[j++] = heightMap[i]
                normals[j] = normal.z
                vertices[j++] = (y - centerY) * cellSizeMeters
                colors[l++] = colorMap[i]
                i++
            }
        }

        mesh.invalidateGeometry()

    }

}