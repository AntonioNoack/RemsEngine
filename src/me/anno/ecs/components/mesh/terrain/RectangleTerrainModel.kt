package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.components.mesh.Mesh
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import org.joml.Vector3f
import kotlin.math.max

object RectangleTerrainModel {

    fun interface ColorMap {
        operator fun get(xi: Int, zi: Int): Int
    }

    fun interface HeightMap {
        operator fun get(xi: Int, zi: Int): Float
    }

    fun interface NormalMap {
        fun get(xi: Int, zi: Int, dst: Vector3f)
    }

    fun generateRegularQuadHeightMesh(
        width: Int, height: Int, flipY: Boolean,
        cellSizeMeters: Float, mesh: Mesh,
        heightMap: HeightMap, normalMap: NormalMap, colorMap: ColorMap? = null
    ): Mesh {
        generateRegularQuadHeightMesh(width, height, flipY, cellSizeMeters, mesh, true)
        generateQuadIndices(width, height, flipY, mesh)
        fillInYAndNormals(width, height, heightMap, normalMap, mesh)
        if (colorMap != null) fillInColor(width, height, colorMap, mesh)
        mesh.invalidateGeometry()
        return mesh
    }

    fun fillInYAndNormals(width: Int, height: Int, heightMap: HeightMap, normalMap: NormalMap, mesh: Mesh) {
        val positions = mesh.positions!!
        val normals = mesh.normals!!
        fillInYAndNormals(width, height, positions, heightMap, normals, normalMap)
        mesh.positions = positions
        mesh.normals = normals
    }

    fun fillInColor(width: Int, height: Int, colorMap: ColorMap, mesh: Mesh) {
        val colors = mesh.color0.resize(width * height)
        fillInColor(width, height, colors, colorMap)
        mesh.color0 = colors
    }

    fun fillInYAndNormals(
        width: Int, height: Int,
        positions: FloatArray, heightMap: HeightMap,
        normals: FloatArray, normalMap: NormalMap
    ) {
        var j = 0
        val normal = JomlPools.vec3f.create()
        for (y in 0 until height) {
            for (x in 0 until width) {
                normalMap.get(x, y, normal)
                positions[j + 1] += heightMap[x, y]
                normals[j++] = normal.x
                normals[j++] = normal.y
                normals[j++] = normal.z
            }
        }
        JomlPools.vec3f.sub(1)
    }

    fun fillInColor(width: Int, height: Int, colors: IntArray, colorMap: ColorMap) {
        var l = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                colors[l++] = colorMap[x, y]
            }
        }
    }

    fun generateQuadIndices(width: Int, height: Int, flipY: Boolean, mesh: Mesh) {
        var k = 0
        val indexCount = max(0, width - 1) * max(0, height - 1) * 6
        val indices = mesh.indices.resize(indexCount)
        mesh.indices = indices
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
    }

    fun generateRegularQuadHeightMesh(
        numPointsX: Int,
        numPointsZ: Int,
        flipY: Boolean,
        cellSize: Float,
        mesh: Mesh,
        heightMap: HeightMap,
        colorMap: ColorMap? = null
    ): Mesh {

        generateQuadIndices(numPointsX, numPointsZ, flipY, mesh)

        ///////////////////////
        // generate vertices //
        ///////////////////////

        val vertexCount = numPointsX * numPointsZ
        val numCoords = vertexCount * 3
        val positions = mesh.positions.resize(numCoords)
        val normals = mesh.normals.resize(numCoords)
        val colors = if (colorMap != null) mesh.color0.resize(vertexCount)
        else null

        mesh.positions = positions
        mesh.normals = normals
        mesh.color0 = colors

        normals.fill(0f)

        // center mesh
        val centerX = numPointsX * 0.5f
        val centerY = numPointsZ * 0.5f

        // define mesh positions
        var j = 0
        for (zi in 0 until numPointsZ) {
            for (xi in 0 until numPointsX) {
                positions[j++] = (xi - centerX) * cellSize
                positions[j++] = heightMap[xi, zi]
                positions[j++] = (zi - centerY) * cellSize
            }
        }

        if (colorMap != null && colors != null) {
            var l = 0
            for (zi in 0 until numPointsZ) {
                for (xi in 0 until numPointsX) {
                    colors[l++] = colorMap[xi, zi]
                }
            }
        }

        mesh.invalidateGeometry()
        return mesh
    }

    fun generateRegularQuadHeightMesh(
        numPointsX: Int,
        numPointsZ: Int,
        flipY: Boolean,
        cellSizeMeters: Float,
        mesh: Mesh, center: Boolean
    ): Mesh {
        generateQuadIndices(numPointsX, numPointsZ, flipY, mesh)
        generateQuadVertices(numPointsX, numPointsZ, cellSizeMeters, mesh, center)
        mesh.invalidateGeometry()
        return mesh
    }

    fun generateQuadVertices(numPointsX: Int, numPointsZ: Int, cellSizeMeters: Float, mesh: Mesh, center: Boolean) {
        val vertexCount = numPointsX * numPointsZ
        val numCoords = vertexCount * 3
        val positions = mesh.positions.resize(numCoords)
        val normals = mesh.normals.resize(numCoords)

        mesh.positions = positions
        mesh.normals = normals

        normals.fill(0f)

        // center mesh
        val centerX = if (center) (numPointsX - 1) * 0.5f else 0f
        val centerY = if (center) (numPointsZ - 1) * 0.5f else 0f
        var j = 0

        // define mesh positions
        for (zi in 0 until numPointsZ) {
            for (xi in 0 until numPointsX) {
                positions[j++] = (xi - centerX) * cellSizeMeters
                normals[j] = 1f
                positions[j++] = 0f
                positions[j++] = (zi - centerY) * cellSizeMeters
            }
        }
    }

    fun fillUVs(mesh: Mesh) {
        val pos = mesh.positions!!
        val bs = mesh.getBounds()
        val uvs = mesh.uvs.resize(pos.size / 3 * 2)
        var j = 0
        val fx = +1f / bs.deltaX
        val fz = -1f / bs.deltaZ
        val mx = bs.minX
        val mz = bs.maxZ
        for (i in pos.indices step 3) {
            uvs[j++] = (pos[i] - mx) * fx
            uvs[j++] = (pos[i + 2] - mz) * fz
        }
        mesh.uvs = uvs
        mesh.invalidateGeometry()
    }
}