package me.anno.mesh.obj

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.files.FileReference
import me.anno.mesh.Triangulation
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import java.io.EOFException
import java.io.InputStream

class SimpleOBJReader(input: InputStream, val file: FileReference) : TextFileReader(input) {

    val mesh = Mesh()

    private val positions = FloatArrayList(256 * 3)
    private val facePositions = FloatArrayList(256 * 3)

    private var numPositions = 0

    private val points = IntArrayList(256)

    private fun putPoint(index: Int) {
        facePositions.addAll(positions, points[index], 3)
    }

    private fun putLinePoint(index: Int) {
        facePositions.addAll(positions, index * 3, 3)
    }

    private fun readCoordinate() {
        skipSpaces()
        positions += readFloat()
    }

    private fun readPosition() {
        readCoordinate()
        readCoordinate()
        readCoordinate()
        skipLine()
        numPositions++
    }

    private fun readLine() {
        points.clear()
        skipSpaces()
        val numVertices = positions.size / 3
        val idx0 = readIndex(numVertices)
        skipSpaces()
        val idx1 = readIndex(numVertices)
        val next0 = nextChar()
        if (next0 == '\n') {
            putLinePoint(idx0)
            putLinePoint(idx1)
            putLinePoint(idx1) // degenerate triangle
        } else {
            putBack(next0)
            points.add(idx0)
            points.add(idx1)
            pts@ while (true) {
                when (val next = next()) {
                    ' '.code, '\t'.code -> {
                    }
                    '\n'.code -> break@pts
                    else -> {
                        putBack(next)
                        points += readIndex(numVertices)
                    }
                }
            }
            var previous = points[0]
            for (i in 1 until points.size) {
                putLinePoint(previous)
                val next = points[i]
                putLinePoint(next)
                putLinePoint(next) // degenerate triangle
                previous = next
            }
        }
    }

    private fun readFace() {

        val points = points
        points.clear()
        var pointCount = 0
        val numPositions = numPositions
        findPoints@ while (true) {
            skipSpaces()
            val next = next()
            if (next in 48..58 || next == MINUS) {
                putBack(next)
                val vertexIndex = readIndex(numPositions)
                if (putBack == SLASH) {
                    putBack = -1
                    readInt()
                    if (putBack == SLASH) {
                        putBack = -1
                        readInt()
                    }
                }
                points.add(vertexIndex * 3)
                pointCount++
                if (pointCount and 63 == 0)
                    LOGGER.warn("Large polygon in $file, $pointCount points, '$next'")
            } else break@findPoints
        }

        val pattern = patterns.getOrNull(pointCount - 1)
        if (pattern != null) {
            for (i in pattern) {
                putPoint(i)
            }
        } else if (pointCount > 0) {
            // triangulate the points correctly
            // currently is the most expensive step, because of so many allocations:
            // points, the array, the return list, ...
            val points2 = (0 until points.size).map {
                Vector3d(positions.values, points[it])
            }
            val triangles = Triangulation.ringToTrianglesVec3d(points2)
            facePositions.ensureExtra(triangles.size * 3)
            for (i in triangles.indices) {
                facePositions.add(triangles[i])
            }
        }
    }

    init {
        try {
            while (true) {
                // read the line
                skipSpaces()
                when (nextChar()) {
                    'v' -> {
                        when (nextChar()) {
                            ' ', '\t' -> readPosition()
                            else -> skipLine()
                        }
                    }
                    'f' -> readFace()
                    'l' -> readLine() // hasn't been tested yet
                    else -> skipLine()
                }
            }
        } catch (_: EOFException) {
        }
        mesh.positions = facePositions.toFloatArray()
        reader.close()
    }

    companion object {
        private val patterns = arrayOf(
            intArrayOf(0, 0, 0),
            intArrayOf(0, 1, 1),
            intArrayOf(0, 1, 2),
            intArrayOf(0, 1, 2, 2, 3, 0)
        )

        @JvmStatic
        private val LOGGER = LogManager.getLogger(SimpleOBJReader::class)
    }
}