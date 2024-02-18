package me.anno.mesh.obj

import me.anno.ecs.components.mesh.Mesh
import me.anno.mesh.Triangulation
import me.anno.io.files.FileReference
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import org.apache.logging.log4j.LogManager
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

    private fun readPosition() {
        skipSpaces()
        positions += readFloat()
        skipSpaces()
        positions += readFloat()
        skipSpaces()
        positions += readFloat()
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
            if (next in 48..58 || next == minus) {
                putBack(next)
                val vertexIndex = readIndex(numPositions)
                if (putBack == slash) {
                    putBack = -1
                    readInt()
                    if (putBack == slash) {
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

        when (pointCount) {
            0 -> {
            } // nothing...
            1 -> {
                // a single, floating point
                putPoint(0)
                putPoint(0)
                putPoint(0)
            }
            2 -> {
                // a line...
                putPoint(0)
                putPoint(1)
                putPoint(1)
            }
            3 -> {
                putPoint(0)
                putPoint(1)
                putPoint(2)
            }
            4 -> {
                putPoint(0)
                putPoint(1)
                putPoint(2)
                putPoint(2)
                putPoint(3)
                putPoint(0)
            }
            else -> {

                // triangulate the points correctly
                // currently is the most expensive step, because of so many allocations:
                // points, the array, the return list, ...


                val points2 = Array(points.size) {
                    val vi = points[it]
                    JomlPools.vec3f.create().set(
                        positions[vi],
                        positions[vi + 1],
                        positions[vi + 2]
                    )
                }
                val triangles = Triangulation.ringToTrianglesPoint2(points2)
                for (i in triangles.indices step 3) {
                    facePositions.add(triangles[i])
                    facePositions.add(triangles[i + 1])
                    facePositions.add(triangles[i + 2])
                }

                JomlPools.vec3f.sub(points.size)

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
        @JvmStatic
        private val LOGGER = LogManager.getLogger(SimpleOBJReader::class)
    }

}