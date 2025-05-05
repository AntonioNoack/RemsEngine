package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.hpc.WorkSplitter
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import kotlin.math.max

object RectangleTerrainModel {

    fun generateRegularQuadHeightMesh(
        width: Int, height: Int, flipY: Boolean,
        cellSize: Float, mesh: Mesh,
        heightMap: HeightMap, normalMap: NormalMap, colorMap: ColorMap? = null
    ): Mesh {
        generateRegularQuadHeightMesh(width, height, flipY, cellSize, mesh, true)
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

                indices[k++] = i00
                if (flipY) {
                    indices[k++] = i11
                    indices[k++] = i10
                    indices[k++] = i01
                    indices[k++] = i11
                } else {
                    indices[k++] = i10
                    indices[k++] = i11
                    indices[k++] = i11
                    indices[k++] = i01
                }
                indices[k++] = i00
                i00++
            }
        }
    }

    fun generateSparseQuadIndices(
        originalWidth: Int, originalHeight: Int,
        sparseWidth: Int, sparseHeight: Int, flipY: Boolean,
        sparseMesh: Mesh
    ) {

        val sparseSize = sparseWidth * sparseHeight
        val indices = sparseMesh.indices.resize(sparseSize * 6)

        val xs = IntArray(sparseWidth) { WorkSplitter.partition(it, originalWidth, sparseWidth) }

        var k = 0
        for (y in 0 until sparseHeight - 1) {
            val y0 = WorkSplitter.partition(y, originalHeight, sparseHeight)
            val y1 = WorkSplitter.partition(y + 1, originalHeight, sparseHeight)
            for (x in 0 until sparseWidth - 1) {

                val x0 = xs[x]
                val x1 = xs[x + 1]

                val i00 = y0 * originalWidth + x0
                val i01 = y0 * originalWidth + x1
                val i10 = y1 * originalWidth + x0
                val i11 = y1 * originalWidth + x1

                indices[k++] = i00
                if (flipY) {
                    indices[k++] = i11
                    indices[k++] = i10
                    indices[k++] = i01
                    indices[k++] = i11
                } else {
                    indices[k++] = i10
                    indices[k++] = i11
                    indices[k++] = i11
                    indices[k++] = i01
                }
                indices[k++] = i00
            }
        }

        sparseMesh.indices = indices
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
        cellSize: Float,
        mesh: Mesh, center: Boolean
    ): Mesh {
        generateQuadIndices(numPointsX, numPointsZ, flipY, mesh)
        generateQuadVertices(numPointsX, numPointsZ, cellSize, mesh, center)
        mesh.invalidateGeometry()
        return mesh
    }

    fun generateQuadVertices(numPointsX: Int, numPointsZ: Int, cellSize: Float, mesh: Mesh, center: Boolean) {
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
                positions[j++] = (xi - centerX) * cellSize
                normals[j] = 1f
                positions[j++] = 0f
                positions[j++] = (zi - centerY) * cellSize
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
        forLoopSafely(pos.size, 3) { i ->
            uvs[j++] = (pos[i] - mx) * fx
            uvs[j++] = (pos[i + 2] - mz) * fz
        }
        mesh.uvs = uvs
        mesh.invalidateGeometry()
    }
}